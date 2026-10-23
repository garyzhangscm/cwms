# FAY pilot: pallet quantities on Manual Pick Sheet

Shows full standard pallets plus remainder in the product stock unit, together with units per pallet. Uses only explicitly named pallet UOMs (PL/PLT/PAL/PALLET/PALLETS) in the default package. Case and tracking-LPN flags alone do not establish a pallet size. Missing, invalid or ambiguous configurations show a clear setup prompt; no quantity is guessed or rounded up. Weight remainders retain their own unit.

Only report data and the three Manual Pick Sheet resources are changed. Allocation, actual picking, report availability, drop-off barcodes, JDA and database structure are unchanged. Deploy FAY only, preserving Colton's current images and templates. Back up both FAY services and verify health, artifact and effective-template hashes.
