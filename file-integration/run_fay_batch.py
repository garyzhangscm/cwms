"""Run the one batch explicitly selected by the Fay operator."""
import json
import os
import subprocess
import sys
from pathlib import Path

import adapter


def main():
    os.umask(0o077)
    config_path = Path('/etc/cwms-fay-oracle-items/config.json')
    config = json.loads(config_path.read_text())
    name = Path('/etc/cwms-fay-oracle-items/batch-file').read_text().strip()
    if config.get('sourceFormat') != 'oracle-items-v1' or not adapter.published_file(name, config):
        raise ValueError('batch-file must contain one Oracle Item batch filename')
    if config.get('deleteSourceFiles') is not False:
        raise ValueError('Fay pilot requires deleteSourceFiles=false')
    return subprocess.call([
        sys.executable, str(Path(__file__).with_name('adapter.py')), 'run',
        '--config', str(config_path), '--send', '--once', '--file', name])


if __name__ == '__main__':
    try:
        sys.exit(main())
    except Exception as error:
        print('Failed: ' + type(error).__name__, file=sys.stderr)
        sys.exit(1)
