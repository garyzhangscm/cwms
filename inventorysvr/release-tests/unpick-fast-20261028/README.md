Unpick performance optimization

Resolve consolidation policy before loading inventory at the destination. The default/NONE handler returns the original inventory without loading the destination rows. Configured consolidation paths remain in place.

Immediate Unpick returns the persisted inventory from moveInventory; deferred movement retains its save. Existing outbound quantity synchronization, movement cleanup and audit calls are retained. Production InventoryService is patched narrowly from the verified live JAR to preserve earlier release fixes. Source change is also saved in the repository.

Only Inventory service is deployed to Colton. No Web, Fay, database or mobile changes. Timings need user confirmation after rollout.
