# Shipment deletion staging reservation cleanup

Both internal removeShipment and the public batch delete entry must use the existing outstanding Pick/short-allocation guards and release Layout reservations by shipment number before deleting the shipment. The ID delete overload now delegates to guarded removal. No stage location ID is required. A Layout failure prevents deleting the shipment; retry can find it again. This uses the existing release contract and does not introduce a cross-service atomic transaction.

Regression checks exercise release-before-delete, failed release, outstanding Pick protection, null stage ID and public batch routing. Test fixtures are synthetic and do not execute production deletion.

Deployment must be based on the exact current Colton JAR; only ShipmentService.class is replaced. Historical cleanup is a separate reviewed transaction: lock candidate locations, check current inventory and all relevant work/assignment references, preserve a before-record and update only reserved_code. Do not infer that an empty location is unused.
