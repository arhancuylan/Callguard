"""Direct installed-release UI verification, independent of Espresso's window-focus lifecycle."""
import subprocess
import re
import xml.etree.ElementTree as ET
from pathlib import Path

def adb(*args):
    return subprocess.run(['adb', 'shell', *args], text=True, capture_output=True, check=True, timeout=20).stdout

def tree():
    adb('uiautomator', 'dump', '/sdcard/release-ui.xml')
    xml = adb('cat', '/sdcard/release-ui.xml')
    return ET.fromstring(xml)

def node(text=None, description=None):
    for item in tree().iter('node'):
        if text is not None and item.get('text') == text:
            return item
        if description is not None and item.get('content-desc') == description:
            return item
    raise AssertionError('Missing release UI element: ' + str(text or description))

def tap_node(item):
    x1, y1, x2, y2 = map(int, re.findall(r'\d+', item.get('bounds')))
    assert x2 > x1 and y2 > y1, 'UI control has zero area'
    adb('input', 'tap', str((x1+x2)//2), str((y1+y2)//2))

def tap(text=None, description=None):
    tap_node(node(text, description))

def nav(name):
    tap(description=name + ' sekmesi')

adb('input', 'keyevent', 'KEYCODE_WAKEUP')
adb('wm', 'dismiss-keyguard')
node(text='Huzurla devam et.')
node(text='İzin verilen kişi')
nav('Kişiler')
node(text='Senin kuralların')
tap(text='＋  Kişi ekle')
tap(text='Elle numara ekle')
fields=[n for n in tree().iter('node') if n.get('class')=='android.widget.EditText']
assert len(fields)==2
tap_node(fields[0]);adb('input', 'text', 'CI%20Test')
tap_node(fields[1]);adb('input', 'text', '05550001111')
adb('input', 'keyevent', 'KEYCODE_BACK')
tap(text='Kaydet')
node(text='CI Test')
tap(description='CI Test kişisini kaldır')
tap(text='Kaldır')
node(text='İzin verilenler · 0 kişi')
tap(text='Kara liste')
node(text='Engellenenler · 0 kişi')
nav('Geçmiş')
node(text='Henüz arama kaydı yok.')
nav('Ayarlar')
node(text='Her şey kontrolünde')
tap(text='Görüntülü')
tap(description='Yalnızca kaydet')
nav('Koruma')
tap(description='Tüm gelen aramaları reddet')
nav('Ayarlar');nav('Koruma')
rule=node(description='Tüm gelen aramaları reddet')
assert any(n.get('checked')=='true' for n in rule.iter('node')), 'Rule did not persist across tabs'
adb('screencap', '-p', '/sdcard/release-screen.png')
Path('evidence/release-ui.xml').write_text(adb('cat','/sdcard/release-ui.xml'))
print('RELEASE_UI_OK: launch, all four tabs, manual add/remove, both lists, settings and rule persistence',flush=True)
