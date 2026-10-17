package com.callguard;

import org.junit.Test;
import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static org.junit.Assert.*;

public class DriveBackupTest {
    private JSONObject backup() throws Exception {return new JSONObject().put("version",1).put("contacts",new JSONArray()).put("settings",new JSONObject()).put("history",new JSONArray());}
    @Test public void snapshotRoundTripPreservesUnicode() throws Exception {
        JSONObject b=backup().put("contacts",new JSONArray().put(new JSONObject().put("name","Çağrı Şahin").put("number","+905551112233").put("white",true)));
        JSONObject restored=DriveBackupFormat.decode(DriveBackupFormat.encode(b,123));
        assertEquals("Çağrı Şahin",restored.getJSONArray("contacts").getJSONObject(0).getString("name"));assertEquals(0,restored.getJSONObject("settings").length());
    }
    @Test public void foreignSnapshotIsRejected() throws Exception {
        byte[] bytes=new JSONObject().put("app","another.app").put("format",1).put("createdAt",1).put("backup",backup()).toString().getBytes(StandardCharsets.UTF_8);
        assertThrows(IOException.class,()->DriveBackupFormat.decode(bytes));
    }
    @Test public void unsupportedVersionAndOversizedDataAreRejected() throws Exception {
        assertThrows(IOException.class,()->DriveBackupFormat.decode(DriveBackupFormat.encode(backup().put("version",2),1)));
        assertThrows(IOException.class,()->DriveBackupFormat.decode(new byte[DriveBackupFormat.MAX_BYTES+1]));
    }
    @Test public void uploadUsesOnlyPrivateAppDataAndNeverOverwrites() throws Exception {
        List<Fake> calls=new ArrayList<>();DriveBackup d=new DriveBackup("private-token",u->{Fake f=new Fake(u,200,"{\"id\":\"snapshot1\"}");calls.add(f);return f;});
        assertTrue(d.save(backup()).getLong("savedAt")>0);d.save(backup());assertEquals(2,calls.size());
        for(Fake f:calls){assertEquals("www.googleapis.com",f.getURL().getHost());assertEquals("POST",f.getRequestMethod());assertFalse(f.getInstanceFollowRedirects());String body=f.sent.toString("UTF-8");assertTrue(body.contains("appDataFolder"));assertTrue(body.contains("callguard-backup-v1"));assertFalse(body.contains("private-token"));}
    }
    @Test public void listFollowsPagesWithinAppData() throws Exception {
        List<URL> urls=new ArrayList<>();DriveBackup d=new DriveBackup("token",u->{urls.add(u);return new Fake(u,200,urls.size()==1?"{\"files\":[{\"id\":\"a\"}],\"nextPageToken\":\"page 2\"}":"{\"files\":[{\"id\":\"b\"}]}");});
        assertEquals(2,d.list().length());for(URL u:urls){assertTrue(u.getQuery().contains("spaces=appDataFolder"));assertTrue(URLDecoder.decode(u.getQuery(),"UTF-8").contains("callguard-backup-v1"));}assertTrue(urls.get(1).getQuery().contains("pageToken=page+2"));
    }
    @Test public void apiFailureNeverBecomesSuccessfulBackup() throws Exception {
        DriveBackup d=new DriveBackup("secret",u->new Fake(u,403,"sensitive upstream error"));
        IOException failure=assertThrows(IOException.class,()->d.save(backup()));assertFalse(failure.getMessage().contains("secret"));assertFalse(failure.getMessage().contains("sensitive"));
    }
    @Test public void readChecksEnvelopeAndDeleteOnlyUsesSelectedId() throws Exception {
        List<Fake> calls=new ArrayList<>();String contents=new String(DriveBackupFormat.encode(backup(),5),StandardCharsets.UTF_8);
        DriveBackup d=new DriveBackup("token",u->{Fake f=new Fake(u,200,contents);calls.add(f);return f;});assertEquals(1,d.read("chosen").getInt("version"));d.delete("chosen");assertEquals("DELETE",calls.get(1).getRequestMethod());assertEquals("/drive/v3/files/chosen",calls.get(1).getURL().getPath());
        assertThrows(IOException.class,()->d.delete("../other"));assertEquals(2,calls.size());
    }
    @Test public void truncatedNetworkResponseDoesNotProduceBackup() throws Exception {
        DriveBackup d=new DriveBackup("token",u->new Fake(u,200,"{\"app\":"));assertThrows(JSONException.class,()->d.read("snapshot"));
    }
    private static final class Fake extends HttpURLConnection {
        final ByteArrayOutputStream sent=new ByteArrayOutputStream();final int status;final byte[] bytes;
        Fake(URL u,int status,String response){super(u);this.status=status;bytes=response.getBytes(StandardCharsets.UTF_8);}
        @Override public void disconnect(){}@Override public boolean usingProxy(){return false;}@Override public void connect(){}
        @Override public int getResponseCode(){return status;}@Override public InputStream getInputStream(){return new ByteArrayInputStream(bytes);}@Override public OutputStream getOutputStream(){return sent;}
    }
}
