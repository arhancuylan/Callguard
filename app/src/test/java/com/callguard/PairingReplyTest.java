package com.callguard;
import org.junit.Test;
import static org.junit.Assert.*;
public class PairingReplyTest {
    @Test public void discoveredPortAllowsCodeOnly(){PairingReply value=PairingReply.parse("001234",32123);assertEquals(32123,value.port);assertEquals("001234",value.code);}
    @Test public void explicitPortWorksWhenDiscoveryIsUnavailable(){PairingReply value=PairingReply.parse(" 43210  001234 ",0);assertEquals(43210,value.port);assertEquals("001234",value.code);}
    @Test public void lostPortDoesNotReuseOldDiscovery(){assertThrows(IllegalArgumentException.class,()->PairingReply.parse("123456",0));}
    @Test public void malformedInputIsNeverSubmittedToAdb(){for(String value:new String[]{"12345","1234567","0 123456","65536 123456","32;id 123456","1234 123456 extra","abc123",""})assertThrows(IllegalArgumentException.class,()->PairingReply.parse(value,33333));assertThrows(IllegalArgumentException.class,()->PairingReply.parse(null,33333));}
}
