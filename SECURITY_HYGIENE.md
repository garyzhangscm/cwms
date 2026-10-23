# Repository history cleanup — 2026-10-22

All writable branches have been rewritten to remove committed private addresses,
credential defaults, provider keys, JVM crash logs and account seed records.
Existing authorship is retained. Dependency versions/lockfiles and unrelated
application data are preserved. The original recovery backup is retained privately
on the maintainer's computer; never push it or an old clone back to GitHub.

Re-clone this repository before continuing development. Do not merge or push a
pre-cleanup clone, as doing so can reintroduce the removed history. Move any local
uncommitted work as reviewed patches into the new clone.

Provide site addresses and credentials outside Git through your deployment's
private configuration or secret store. Auth/gateways require AUTH_JWT_SECRET
(the auth.jwt.secret Spring property); use the same configured key on both sides.
Mail credentials use MES_SMTP_USERNAME and MES_SMTP_PASSWORD. Legacy service-client
login examples use MES_API_USERNAME and MES_API_PASSWORD. Database/encryption
examples require their externally supplied password/key settings.
Legacy OAuth clients use MES_OAUTH_CLIENT_ID and MES_OAUTH_CLIENT_SECRET.
Legacy QuickBooks clients use MES_QUICKBOOK_CLIENT_ID and MES_QUICKBOOK_CLIENT_SECRET.

Account initialization CSVs retain their headers only. Provision users/roles
through your site's approved account-management process; real account records and
password hashes must not be committed. Existing production databases were not
modified by this cleanup.

Browser Google Maps configuration must be injected before Angular starts:
window.__MES_PUBLIC_CONFIG__ = { googleMapsApiKey: <site-provided browser key> }.
Set this outside the source repository. Browser keys remain visible to end users;
restrict them by the authorized domains/APIs. Never put a server secret here.

Run python3 scripts/check_repository_security.py before committing. It only prints
paths and line numbers. This is a text-oriented guard; binary/encrypted design
assets are retained, and this is not a full security certification.

No production service/image is deployed by these repository changes. Before a
future source build/deployment, provision the required signing, mail, Maps and
site-address settings; do not deploy without reviewing private configuration.

GitHub pull-request refs/cached commits and other people's forks/clones are outside
normal branch rewriting. GitHub Support may need to purge cached sensitive views.
Any exposed credential still in use needs separate revocation/rotation; deleting
history does not invalidate credentials.
