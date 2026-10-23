import { CommonModule } from '@angular/common';
import { Component, Input, OnDestroy, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { _HttpClient } from '@delon/theme';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzCardModule } from 'ng-zorro-antd/card';
import { NzModalModule, NzModalService } from 'ng-zorro-antd/modal';
import { NzMessageService } from 'ng-zorro-antd/message';
import { Subscription } from 'rxjs';
import { UserService } from '../../auth/services/user.service';
import { WarehouseService } from '../../warehouse-layout/services/warehouse.service';

interface Rule { customerId: number; destinationLocationId: number; enabled: boolean; }
interface Configuration { revision: number; rules: Rule[]; modifiedBy?: string; modifiedAt?: string; }
interface Choice { id: number; name: string; }
interface Metadata { featureEnabled: boolean; customers: Choice[]; locations: Choice[]; configuration: Configuration; }

@Component({
  selector: 'app-in-transit-shipping-configuration', standalone: true,
  imports: [CommonModule, FormsModule, NzButtonModule, NzCardModule, NzModalModule],
  template: `
    <nz-card *ngIf="visible" nzTitle="FAY → Olivet: In-transit shipping">
      <p>Completed shipments for enabled customers stay in the selected physical location as company inventory.
        Inventory remains there until your team handles it manually.</p>
      <p role="alert" *ngIf="error" style="color:#b42318">{{ error }}</p>
      <div style="overflow-x:auto" *ngIf="metadata">
        <table style="width:100%;border-collapse:collapse" aria-label="In-transit customer rules">
          <thead><tr><th>Customer</th><th>Destination location</th><th>Enabled</th></tr></thead>
          <tbody><tr *ngFor="let row of draft">
            <td>{{ customerName(row.customerId) }}</td>
            <td><select [(ngModel)]="row.destinationLocationId" [disabled]="busy || displayOnly"
              [attr.aria-label]="'Destination for ' + customerName(row.customerId)">
              <option *ngFor="let location of metadata.locations" [ngValue]="location.id">{{ location.name }}</option>
            </select></td>
            <td><input type="checkbox" [(ngModel)]="row.enabled" [disabled]="busy || displayOnly"
              [attr.aria-label]="'Enable in-transit for ' + customerName(row.customerId)" /></td>
          </tr></tbody>
        </table>
        <p style="margin-top:16px">Changes apply to future shipment completions. Existing inventory is not moved.
          Disabled customers use the standard shipping flow.</p>
        <button nz-button nzType="primary" (click)="confirmSave()" [disabled]="busy || displayOnly || stale" [nzLoading]="busy">Save in-transit rules</button>
        <button nz-button (click)="load()" [disabled]="busy" style="margin-left:8px">Reload</button>
        <p *ngIf="metadata.configuration.modifiedAt" style="margin-top:12px">
          Last saved by {{ metadata.configuration.modifiedBy }} · {{ metadata.configuration.modifiedAt }}
        </p>
      </div>
    </nz-card>`,
  styles: [`th,td {text-align:left;padding:12px;border-bottom:1px solid #eee} select {padding:6px;min-width:180px}`]
})
export class InTransitShippingComponent implements OnInit, OnDestroy {
  @Input() displayOnly = false;
  metadata?: Metadata;
  draft: Rule[] = [];
  visible = false;
  busy = false;
  stale = false;
  error = '';
  private subscriptions = new Subscription();
  private warehouseId?: number;
  constructor(private http: _HttpClient, private warehouses: WarehouseService,
    private modal: NzModalService, private messages: NzMessageService, private users: UserService) {}
  async ngOnInit(): Promise<void> {
    try {
      const user = await this.users.getCurrentUser();
      if (this.subscriptions.closed) return;
      if (user && (user.admin === true || user.systemAdmin === true || user.roles?.some(role => role.enabled === true && role.name === 'Admin'))) this.load();
    } catch { this.visible = false; }
  }
  ngOnDestroy(): void { this.subscriptions.unsubscribe(); }
  customerName(id: number): string { return this.metadata?.customers.find(c => c.id === id)?.name ?? String(id); }
  load(): void {
    if (this.busy) return;
    const warehouseId = this.warehouses.getCurrentWarehouse().id;
    this.warehouseId = warehouseId;
    this.busy = true;
    this.error = '';
    this.subscriptions.add(this.http.get(`outbound/in-transit-shipping/rules?warehouseId=${warehouseId}`).subscribe({
      next: response => {
        this.busy = false;
        if (this.warehouses.getCurrentWarehouse().id !== warehouseId) { this.visible = false; return; }
        const data = response.data as Metadata;
        this.visible = data?.featureEnabled === true;
        if (!this.visible) return;
        this.metadata = data;
        this.draft = data.customers.map(customer => ({
          customerId: customer.id,
          destinationLocationId: data.configuration.rules.find(r => r.customerId === customer.id)?.destinationLocationId ?? data.locations[0]?.id,
          enabled: data.configuration.rules.find(r => r.customerId === customer.id)?.enabled ?? false
        }));
        this.stale = false;
      },
      error: error => {
        this.busy = false;
        // Existing servers, unconfigured warehouses and non-admin users keep the existing page.
        this.visible = ![401, 403, 404].includes(error.status);
        this.stale = true;
        this.error = 'Could not load in-transit rules. Reload before making changes.';
      }
    }));
  }
  confirmSave(): void {
    if (this.busy || this.stale || this.displayOnly || !this.metadata) return;
    this.modal.confirm({
      nzTitle: 'Save in-transit shipping rules?',
      nzContent: 'Future shipments for enabled customers will remain as physical inventory in the selected location. Your team will handle this inventory manually. Existing stock and other customers are unchanged.',
      nzOnOk: () => this.save()
    });
  }
  private save(): void {
    if (!this.metadata || this.displayOnly || this.stale || this.busy) return;
    if (this.warehouses.getCurrentWarehouse().id !== this.warehouseId) {
      this.stale = true; this.error = 'Warehouse changed. Reload before saving.'; return;
    }
    this.busy = true;
    this.error = '';
    const warehouseId = this.warehouseId;
    const payload: Configuration = { revision: this.metadata.configuration.revision, rules: this.draft.map(row => ({...row})) };
    this.subscriptions.add(this.http.put(`outbound/in-transit-shipping/rules?warehouseId=${warehouseId}`, payload).subscribe({
      next: () => { this.busy = false; this.messages.success('In-transit rules saved'); this.load(); },
      error: error => {
        this.busy = false;
        this.stale = true;
        this.error = error.status === 409 ? 'Rules or location settings changed. Reload before saving again.' : 'Save failed. Reload to check the current rules before retrying.';
      }
    }));
  }
}
