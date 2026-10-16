"""Build an isolated cPanel upload ZIP; never include real config/keys or unrelated app files."""
from pathlib import Path
import zipfile
root = Path(__file__).resolve().parents[1]
target = root / 'CallGuard-cPanel-PHP-MySQL.zip'
if not (root / 'private/vendor/autoload.php').is_file():
    raise SystemExit('Dependencies must be installed in CI before packaging.')
with zipfile.ZipFile(target, 'w', zipfile.ZIP_DEFLATED) as archive:
    for path in (root / 'public').rglob('*'):
        if path.is_file():
            archive.write(path, 'public_html/callguard/' + path.relative_to(root / 'public').as_posix())
    for path in (root / 'private').rglob('*'):
        relative = path.relative_to(root / 'private')
        if not path.is_file() or relative.parts[0] == 'tests' or path.name in {'.gitignore', 'config.php', 'play-service-account.json', '.env'}:
            continue
        if relative.parts[0] == 'cache':
            continue
        archive.write(path, 'callguard-private/' + relative.as_posix())
    archive.write(root / 'README.md', 'README-TR.md')
    archive.write(root / 'tools/Prepare-CallGuard.ps1', 'tools/Prepare-CallGuard.ps1')
with zipfile.ZipFile(target) as archive:
    names = archive.namelist()
    assert 'public_html/callguard/.htaccess' in names
    assert 'callguard-private/vendor/autoload.php' in names
    assert 'callguard-private/config.example.php' in names
    assert all(not name.startswith('public_html/mktech/') for name in names)
    assert 'public_html/.htaccess' not in names
    assert 'callguard-private/config.php' not in names
print(f'{target.name}: public and private files separated; no production credentials or MK Tech files')
