package com.callguard;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.json.*;

/** Portable snapshots contain only BackupData's allowlisted settings, never login tokens. */
final class DriveBackupFormat {
    static final String APP="com.callguard.mobile";
    static final int MAX_BYTES=4*1024*1024;
    static byte[] encode(JSONObject backup,long at) throws Exception {
        byte[] bytes=new JSONObject().put("app",APP).put("format",1).put("createdAt",at).put("backup",backup).toString().getBytes(StandardCharsets.UTF_8);
        if(bytes.length>MAX_BYTES)throw new IOException("Drive yedeği 4 MB sınırını aşıyor.");
        return bytes;
    }
    static JSONObject decode(byte[] bytes) throws Exception {
        if(bytes.length>MAX_BYTES)throw new IOException("Drive yedeği çok büyük.");
        JSONObject envelope=new JSONObject(new String(bytes,StandardCharsets.UTF_8));
        if(!APP.equals(envelope.optString("app"))||envelope.optInt("format")!=1||envelope.optLong("createdAt",-1)<0)throw new IOException("Geçerli bir CallGuard Drive yedeği değil.");
        JSONObject backup=envelope.getJSONObject("backup");
        if(backup.getInt("version")!=1)throw new IOException("Bu yedek sürümü desteklenmiyor.");
        return backup;
    }
    private DriveBackupFormat(){}
}
