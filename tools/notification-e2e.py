"""Exercise the real listener and foreign PendingIntent outside the UI instrumentation process."""
import subprocess
import time

APP = 'com.callguard.mobile'
CONTROL = APP + '/com.callguard.TestControlReceiver'
CALLS = 'com.whatsapp/com.callguard.fixture.Calls'
NUMBER = '+905550001111'

def adb(*args):
    return subprocess.run(['adb', 'shell', *args], text=True, capture_output=True, check=True, timeout=12).stdout

def broadcast(component, action, **extras):
    args = ['am', 'broadcast', '--receiver-foreground', '-a', action, '-n', component]
    for key, value in extras.items():
        args += ['--ez' if isinstance(value, bool) else '--es', key, str(value).lower() if isinstance(value, bool) else str(value)]
    return adb(*args)

def state():
    return adb('content', 'query', '--uri', 'content://com.callguard.mobile.teststate/state')

def fixture():
    return adb('content', 'query', '--uri', 'content://com.whatsapp.callguardfixture/state')

def wait_for(predicate, description, seconds=10):
    until = time.monotonic() + seconds
    while time.monotonic() < until:
        if predicate():
            return
        time.sleep(0.15)
    raise AssertionError(description + '\nAPP: ' + state() + '\nFIXTURE: ' + fixture())

def configure(**extras):
    broadcast(CALLS, 'fixture.CLEAR')
    broadcast(CONTROL, 'com.callguard.debug.CONFIGURE', **extras)

def post(video=False):
    broadcast(CALLS, 'fixture.POST', video=video, number=NUMBER)

def check(case, expected, video=False, decline=True, **settings):
    configure(**settings)
    post(video)
    wait_for(lambda: 'count=1' in state(), case + ': no history record')
    value = state()
    assert 'result=' + expected in value, case + ': ' + value
    assert 'video=' + str(int(video)) in value, case + ': wrong media ' + value
    if decline:
        wait_for(lambda: 'declined=1' in fixture(), case + ': foreign Decline PendingIntent did not arrive')
    else:
        assert 'declined=0' in fixture(), case + ': call was unexpectedly declined'
    assert 'answered=0' in fixture(), case + ': Answer must NEVER be invoked'
    print('PASS: ' + case, flush=True)

adb('am', 'start', '-W', '-n', APP + '/com.callguard.MainActivity')
adb('cmd', 'notification', 'disallow_listener', APP + '/com.callguard.CallNotificationService')
adb('cmd', 'notification', 'allow_listener', APP + '/com.callguard.CallNotificationService')
broadcast(CONTROL, 'com.callguard.debug.REBIND')
wait_for(lambda: 'connected=1' in state(), 'Listener must connect after UI instrumentation has finished')
check('voice decline', 'Reddetme isteği gönderildi')
check('video decline', 'Reddetme isteği gönderildi', video=True)
check('whitelist permits', 'İzin verildi', decline=False, whiteNumber=NUMBER)
check('blacklist rejects', 'Reddetme isteği gönderildi', rule='BLACKLIST', blackNumber=NUMBER)
check('blacklist permits unlisted', 'İzin verildi', decline=False, rule='BLACKLIST')
check('all incoming overrides whitelist', 'Reddetme isteği gönderildi', rule='ALL', whiteNumber=NUMBER)
check('record-only never declines', 'Yalnızca kaydedildi', decline=False, recordOnly=True)
assert 'today=0' in state(), 'Record-only must not increment decline requests'
configure(media='VOICE')
post(video=True)
time.sleep(1)
assert 'count=0' in state() and 'declined=0' in fixture(), 'Unselected video was processed'
broadcast(CALLS, 'fixture.CLEAR')
post(video=False)
wait_for(lambda: 'count=1' in state() and 'declined=1' in fixture(), 'Selected voice must be declined')
assert 'answered=0' in fixture()
print('PASS: media filter and selected voice', flush=True)
configure(enabled=False)
post()
time.sleep(1)
assert 'count=0' in state() and 'declined=0' in fixture() and 'answered=0' in fixture(), 'Disabled protection acted'
print('PASS: disabled protection', flush=True)
broadcast(CALLS, 'fixture.CLEAR')
print('NOTIFICATION_E2E_OK (9 cases)', flush=True)
