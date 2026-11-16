# Colton backend V1.65

The Maven service versions use `v1.65`. Docker Hub image tags use `V1.65`, including the repaired outbound image. The running Common, resource and inventory services use the already released `V1.65-r1` images. [dockerhub-images.tsv](dockerhub-images.tsv) records exact multi-platform digests for this deployment.

The outbound image packages the production-accepted Java application from `brianwang1912/cwms-outboundserver@sha256:76534ea37d7e178dc23ee29c78a4b93544fe463873d210fa31c29466fe7bf958`. The extracted outbound application JAR has SHA-256 `3618ca9d10c2f4079234feae21b292e96851a374ac7a4485f61239abd00cb5af`. Use that JAR with the Java 17 image to reproduce the package containing the order pagination, allocation mode and shipment print routes.

The repository records the source and version changes for the Java services. The web and companion QC shift report sources are maintained in the cwms-client repository and tagged `V19.1.65`. Colton's host-based Oracle integration uses its own deployment.
