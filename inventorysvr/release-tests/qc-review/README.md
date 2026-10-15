# QC release regressions

Run with JDK 17 and an existing inventory Spring Boot JAR (Colton or Fay):

```sh
python3 inventorysvr/release-tests/qc-review/run.py /path/to/inventoryserver.jar
```

The harness compiles current QC sources against that JAR in a temporary directory. EntityManager and service persistence calls are faked; it never accesses production services or a business database. It checks trusted standards, mandatory failure notes and acknowledgement, pending without quantity updates, warehouse/check boundaries, preparation locking, strict legacy recovery guards, generated checkpoint IDs, sample quantity and RF code forwarding.
