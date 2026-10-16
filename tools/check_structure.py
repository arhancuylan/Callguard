"""SDK gerektirmeyen manifest/kaynak bütünlüğü kontrolü; Android derlemesi değildir."""
from pathlib import Path
import xml.etree.ElementTree as ET
root = Path(__file__).resolve().parents[1]
ns = '{http://schemas.android.com/apk/res/android}'
for path in root.glob('app/src/main/**/*.xml'):
    ET.parse(path)
manifest = ET.parse(root / 'app/src/main/AndroidManifest.xml').getroot()
assert [p.attrib[ns+'name'] for p in manifest.findall('uses-permission')] == ['android.permission.READ_CONTACTS']
app = manifest.find('application')
assert app.attrib[ns+'allowBackup'] == 'false'
for element in list(app):
    name = element.attrib.get(ns+'name', '')
    if name.startswith('.'):
        assert (root / 'app/src/main/java/com/callguard' / (name[1:] + '.java')).exists()
service = app.find('service')
assert service.attrib[ns+'permission'] == 'android.permission.BIND_ACCESSIBILITY_SERVICE'
config = ET.parse(root / 'app/src/main/res/xml/accessibility_service.xml').getroot()
assert set(config.attrib[ns+'packageNames'].split(',')) == {'com.whatsapp','com.whatsapp.w4b'}
assert config.attrib[ns+'canRetrieveWindowContent'] == 'true'
print('OK: XML, manifest sınıfları, izinler ve servis kapsamı')
