package com.garyzhangscm.cwms.adminserver.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.garyzhangscm.cwms.adminserver.model.wms.Client;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.persistence.*;

/**
 * Billable web request call
 *
 */
@Entity
@Table(name = "invoice_document")
public class InvoiceDocument extends AuditibleEntity<String>{


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "invoice_document_id")
    @JsonProperty(value="id")
    private Long id;


    @Column(name = "file_name")
    private String fileName;

    @Column(name = "remote_file_name")
    private String remoteFileName;


    @ManyToOne
    @JoinColumn(name="invoice_id")
    @JsonIgnore
    private Invoice invoice;

    public InvoiceDocument(){}
    public InvoiceDocument(String fileName, String remoteFileName, Invoice invoice) {
        this.fileName = fileName;
        this.remoteFileName = remoteFileName;
        this.invoice = invoice;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getRemoteFileName() {
        return remoteFileName;
    }

    public void setRemoteFileName(String remoteFileName) {
        this.remoteFileName = remoteFileName;
    }

    public Invoice getInvoice() {
        return invoice;
    }

    public void setInvoice(Invoice invoice) {
        this.invoice = invoice;
    }
}
