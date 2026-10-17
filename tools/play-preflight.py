"""Fast release checks. Absence of native code is checked, not a claimed ELF alignment test."""
from pathlib import Path
import json, sys, zipfile, xml.etree.ElementTree as ET
r=Path(__file__).resolve().parents[1]
metadata=json.loads((r/'distribution/play/metadata.json').read_text())
assert metadata['applicationId']=='com.callguard.mobile'
assert len(metadata['appName'])<=30
listing=(r/'distribution/play/listing-tr-TR.txt').read_text()
short=listing.split('KISA AÇIKLAMA\n',1)[1].split('\n',1)[0]
assert len(short)<=80
assert 'targetSdk 36' in (r/'app/build.gradle').read_text()
ns='{http://schemas.android.com/apk/res/android}'
m=ET.parse(r/'app/src/main/AndroidManifest.xml').getroot()
assert not {'android.permission.READ_CALL_LOG','android.permission.SEND_SMS','android.permission.QUERY_ALL_PACKAGES','com.google.android.gms.permission.AD_ID'} & {v.attrib[ns+'name'] for v in m.findall('uses-permission')}
if len(sys.argv)>1:
    with zipfile.ZipFile(sys.argv[1]) as z:
        native=[name for name in z.namelist() if name.endswith('.so')]
        if native:raise SystemExit('Native libraries detected; perform 16 KB ELF and ZIP alignment review before Play upload: '+', '.join(native))
print('OK: metadata limits, target API, restricted permission scope and native-library inventory')
