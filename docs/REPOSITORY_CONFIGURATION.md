# Repository deployment configuration

Keep real addresses, passwords, signing keys, operator configuration and captured
production data outside Git and Docker build contexts. The supplied .env.example
contains variable names only. A .env file is not automatically loaded by Spring;
provision settings through your deployment environment or an external private
configuration file.

Auth, API Gateway and legacy Zuul now require auth.jwt.secret (environment variable
AUTH_JWT_SECRET), with no embedded default. Provision the same Base64 signing key
to the authentication service and its validating gateways before building/deploying
these source changes. Removing a published default does not rotate running keys.
A deliberate key rotation invalidates existing sessions and should be coordinated.

Docker examples require external MYSQL_ROOT_PASSWORD, MYSQL_PASSWORD,
POSTGRES_PASSWORD and ENCRYPT_KEY where used. They no longer contain example
passwords. No live deployment or database is changed by this source cleanup.

The FAY operator deployment reads site addresses from MES_INVENTORY_BASE_URL,
MES_AUTH_BASE_URL, MES_USER_BASE_URL, MES_LAYOUT_BASE_URL and
MES_OPERATOR_BIND_ADDRESS when creating a new operator configuration.
MES_FORBIDDEN_SITE_PREFIX is the other site's forbidden address prefix; preserve
this cross-site safety check using private deployment configuration.

Run python3 scripts/check_repository_security.py before committing. It reports
paths and line numbers, never matched values. It checks text files for private
IPv4 addresses, common credential tokens, private-key blocks, JWT key fallbacks,
URL credentials and literal credential settings in configuration files.
It is a targeted guard, not a complete security or binary/history scan.
Use --ref REF to inspect a branch tip. Existing evidence URLs and server-local
backup, pod and node details are redacted; those files are historical records,
not runnable deployment configuration.

Normal cleanup commits only change current files. Earlier commits, other
branches, tags, forks, clones and GitHub cached views may still contain old
content. History rewriting changes commit IDs and requires coordination with
collaborators. Exposed credentials that remain in use require rotation.

