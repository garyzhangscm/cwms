"""Set WIS FTP and UniFi VPN credentials on the importer host; run as root."""
import argparse
import getpass
import grp
import os
from pathlib import Path


def write_secret(path, value, mode, group):
    path = Path(path)
    temp = path.with_name(path.name + '.tmp')
    fd = os.open(str(temp), os.O_WRONLY | os.O_CREAT | os.O_EXCL, mode)
    try:
        os.fchown(fd, 0, group)
        os.fchmod(fd, mode)
        with os.fdopen(fd, 'w') as stream:
            stream.write(value + '\n')
            stream.flush()
            os.fsync(stream.fileno())
        os.replace(str(temp), str(path))
    finally:
        if temp.exists():
            temp.unlink()


def main():
    if os.geteuid() != 0:
        raise SystemExit('Run as root on k8s-app2')
    parser = argparse.ArgumentParser(description=__doc__)
    group_options = parser.add_mutually_exclusive_group()
    group_options.add_argument('--vpn-only', action='store_true')
    group_options.add_argument('--ftp-only', action='store_true')
    args = parser.parse_args()
    group = grp.getgrnam('cwmsitem').gr_gid
    ftp_user = input('WIS FTP username: ').strip() if not args.vpn_only else None
    ftp_password = getpass.getpass('WIS FTP password: ') if not args.vpn_only else None
    vpn_user = input('UniFi VPN username: ').strip() if not args.ftp_only else None
    vpn_password = getpass.getpass('UniFi VPN password: ') if not args.ftp_only else None
    values = [value for value in (ftp_user, ftp_password, vpn_user, vpn_password)
              if value is not None]
    if not all(values):
        raise SystemExit('No credential may be empty; nothing was saved')
    if any('\n' in value or '\r' in value for value in values):
        raise SystemExit('Credentials cannot contain newlines; nothing was saved')
    if ftp_user is not None:
        directory = Path('/etc/cwms-oracle-items')
        directory.mkdir(mode=0o750, exist_ok=True)
        os.chown(str(directory), 0, group)
        os.chmod(str(directory), 0o750)
        write_secret(directory / 'ftp-user', ftp_user, 0o640, group)
        write_secret(directory / 'ftp-password', ftp_password, 0o640, group)
    if vpn_user is not None:
        write_secret('/etc/openvpn/client/unifi-oracle.auth',
                     vpn_user + '\n' + vpn_password, 0o600, 0)
    print('Credentials saved locally on k8s-app2; no passwords printed')


if __name__ == '__main__':
    main()
