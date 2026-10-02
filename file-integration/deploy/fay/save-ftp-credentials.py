#!/usr/bin/python3
"""Save Fay FTP credentials locally; run as root on Fay app2."""
import getpass
import grp
import os
from pathlib import Path


def write_secret(path, value, group):
    temp = path.with_name(path.name + '.tmp')
    fd = os.open(str(temp), os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o640)
    try:
        os.fchown(fd, 0, group)
        os.fchmod(fd, 0o640)
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
        raise SystemExit('Run as root on Fay app2')
    os.umask(0o077)
    group = grp.getgrnam('cwmsfayitem').gr_gid
    username = input('Fay Oracle FTP username: ').strip()
    password = getpass.getpass('Fay Oracle FTP password: ')
    if not username or not password or any(c in username + password for c in '\r\n'):
        raise SystemExit('Empty or multiline credentials; nothing saved')
    directory = Path('/etc/cwms-fay-oracle-items')
    directory.mkdir(mode=0o750, exist_ok=True)
    os.chown(str(directory), 0, group)
    os.chmod(str(directory), 0o750)
    write_secret(directory / 'ftp-user', username, group)
    write_secret(directory / 'ftp-password', password, group)
    print('Fay FTP credentials saved locally. No password printed.')


if __name__ == '__main__':
    main()
