package com.callguard;

import org.junit.Test;
import java.io.IOException;
import static org.junit.Assert.*;

public class CarCommandProtocolTest {
    private static final String APK="/data/app/~~abc==/com.callguard.mobile-xy==/base.apk",MAC="AA:BB:CC:DD:EE:FF";
    @Test public void onlyFixedBluetoothOperationsCanBecomeCommands(){
        assertTrue(CarCommandProtocol.command(APK,"read",MAC).endsWith("read "+MAC));
        assertTrue(CarCommandProtocol.command(APK,"write",MAC,"0").endsWith("write "+MAC+" 0"));
        assertTrue(CarCommandProtocol.command(APK,"write",MAC,"100").endsWith("write "+MAC+" 100"));
        assertThrows(IllegalArgumentException.class,()->CarCommandProtocol.command(APK,"shell","id"));
        assertThrows(IllegalArgumentException.class,()->CarCommandProtocol.command(APK,"write",MAC,"1"));
        assertThrows(IllegalArgumentException.class,()->CarCommandProtocol.command(APK,"read",MAC,"extra"));
    }
    @Test public void untrustedPathsAndAddressesCannotInjectShellSyntax(){
        for(String path:new String[]{"/sdcard/base.apk",APK+";id","/data/app/$(id).apk","/data/app/x'foo.apk",null})assertThrows(IllegalArgumentException.class,()->CarCommandProtocol.command(path,"probe"));
        for(String address:new String[]{MAC+";id","$(id)","AA:BB",null})assertThrows(IllegalArgumentException.class,()->CarCommandProtocol.command(APK,"read",address));
    }
    @Test public void missingOrAmbiguousReplyNeverCountsAsVerifiedSuccess()throws Exception{
        assertEquals(0,CarCommandProtocol.parse("warning\r\nCGCAR1:0\r\n"));
        assertEquals(100,CarCommandProtocol.parse("CGCAR1:100\n"));
        for(String output:new String[]{"","0","CGCAR1:ERROR","CGCAR1:0\nCGCAR1:100","CGCAR1:0suffix"})assertThrows(IOException.class,()->CarCommandProtocol.parse(output));
    }
    @Test public void portValidationRejectsShellTextAndOutOfRangeValues(){
        assertEquals(12345,CarCommandProtocol.port("12345"));
        for(String value:new String[]{"0","65536","-1","123;id",""})assertThrows(IllegalArgumentException.class,()->CarCommandProtocol.port(value));
    }
    @Test public void completedFrameDoesNotDependOnAdbEof()throws Exception{
        assertNull(CarCommandProtocol.completed("CGCAR1:20"));
        assertNull(CarCommandProtocol.completed("runtime warning\nCGCAR1:20"));
        assertEquals(Integer.valueOf(2000),CarCommandProtocol.completed("runtime warning\r\nCGCAR1:2000\r\n"));
        assertThrows(IOException.class,()->CarCommandProtocol.completed("CGCAR1:ERROR\n"));
    }
}
