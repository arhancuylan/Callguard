"""SDK gerektirmeyen manifest/kaynak bütünlüğü kontrolü; Android derlemesi değildir."""
from pathlib import Path
import xml.etree.ElementTree as ET
root = Path(__file__).resolve().parents[1]
ns = '{http://schemas.android.com/apk/res/android}'
for path in root.glob('app/src/main/**/*.xml'):
    ET.parse(path)
manifest = ET.parse(root / 'app/src/main/AndroidManifest.xml').getroot()
assert {p.attrib[ns+'name'] for p in manifest.findall('uses-permission')} == {'android.permission.READ_CONTACTS','android.permission.INTERNET','android.permission.POST_NOTIFICATIONS','com.android.vending.BILLING','android.permission.BLUETOOTH','android.permission.BLUETOOTH_CONNECT'}
app = manifest.find('application')
assert app.attrib[ns+'allowBackup'] == 'false'
for element in list(app):
    name = element.attrib.get(ns+'name', '')
    if name.startswith('.'):
        assert (root / 'app/src/main/java/com/callguard' / (name[1:] + '.java')).exists()
services = app.findall('service')
assert len(services) == 1
service = services[0]
assert service.attrib[ns+'name'] == '.CallNotificationService'
assert service.attrib[ns+'permission'] == 'android.permission.BIND_NOTIFICATION_LISTENER_SERVICE'
assert service.find('intent-filter/action').attrib[ns+'name'] == 'android.service.notification.NotificationListenerService'
assert app.attrib[ns+'usesCleartextTraffic'] == 'false'
assert app.find('provider') is None
assert not any('shizuku' in p.read_text().lower() for p in (root/'app/src/main/java').rglob('*.java'))
print('OK: XML, manifest classes, permissions, notification listener and no external ADB manager')
