package com.callguard;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.json.*;

/** Drive v3 REST. Short-lived token stays in memory and goes only to www.googleapis.com. */
final class DriveBackup {
    static final String SCOPE="https://www.googleapis.com/auth/drive.appdata";
    private static final String MARKER="callguard-backup-v1";
    interface Connections { HttpURLConnection open(URL url) throws IOException; }
    private final String token;
    private final Connections connections;
    DriveBackup(String token){this(token,url->(HttpURLConnection)url.openConnection());}
    DriveBackup(String token,Connections connections){this.token=token;this.connections=connections;}
    private static String escape(String value) throws Exception {return URLEncoder.encode(value,"UTF-8");}
    private static String id(String value) throws IOException {if(value==null||!value.matches("[A-Za-z0-9_-]{1,200}"))throw new IOException("Geçersiz Drive yedek kimliği.");return value;}
    private byte[] request(String method,String path,String contentType,byte[] body) throws Exception {
        URL url=new URL("https://www.googleapis.com"+path);
        HttpURLConnection connection=connections.open(url);connection.setConnectTimeout(12000);connection.setReadTimeout(20000);connection.setInstanceFollowRedirects(false);connection.setRequestMethod(method);
        connection.setRequestProperty("Authorization","Bearer "+token);connection.setRequestProperty("Accept","application/json");
        try{
            if(body!=null){connection.setDoOutput(true);connection.setRequestProperty("Content-Type",contentType);connection.setFixedLengthStreamingMode(body.length);try(OutputStream out=connection.getOutputStream()){out.write(body);}}
            int status=connection.getResponseCode();
            if(status<200||status>=300){
                if(status==401)throw new IOException("Drive izni sona ermiş. İşlemi tekrar başlatıp Google hesabını seç.");
                if(status==403)throw new IOException("Drive erişimi reddedildi. Drive API'nin açık olduğunu, uygulama iznini ve Google Drive kotanı kontrol et.");
                if(status==404)throw new IOException("Drive yedeği bulunamadı. Yedek listesini yenile.");
                throw new IOException("Drive işlemi tamamlanamadı ("+status+"). Önceki yedeklerin korunuyor.");
            }
            try(InputStream in=connection.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){
                byte[] buffer=new byte[8192];int n;while((n=in.read(buffer))!=-1){if(out.size()+n>DriveBackupFormat.MAX_BYTES)throw new IOException("Drive yanıtı çok büyük.");out.write(buffer,0,n);}return out.toByteArray();
            }
        }finally{connection.disconnect();}
    }
    JSONArray list() throws Exception {
        JSONArray result=new JSONArray();String page="";
        do{
            String q="trashed = false and appProperties has { key='kind' and value='"+MARKER+"' }";
            String path="/drive/v3/files?spaces=appDataFolder&pageSize=100&orderBy=createdTime%20desc&fields="+escape("nextPageToken,files(id,name,createdTime,size)")+"&q="+escape(q);
            if(!page.isEmpty())path+="&pageToken="+escape(page);
            JSONObject response=new JSONObject(new String(request("GET",path,null,null),StandardCharsets.UTF_8));
            JSONArray files=response.optJSONArray("files");if(files!=null)for(int i=0;i<files.length();i++)result.put(files.getJSONObject(i));
            page=response.optString("nextPageToken","");
            if(result.length()>1000)throw new IOException("Çok fazla Drive yedeği var. Drive uygulama verilerini yöneterek alanı temizleyebilirsin.");
        }while(!page.isEmpty());return result;
    }
    JSONObject save(JSONObject backup) throws Exception {
        long at=System.currentTimeMillis();byte[] media=DriveBackupFormat.encode(backup,at);
        JSONObject metadata=new JSONObject().put("name","CallGuard-"+at+".json").put("mimeType","application/json").put("parents",new JSONArray().put("appDataFolder")).put("appProperties",new JSONObject().put("kind",MARKER));
        String boundary="callguard_"+UUID.randomUUID().toString().replace("-","");
        ByteArrayOutputStream out=new ByteArrayOutputStream();
        out.write(("--"+boundary+"\r\nContent-Type: application/json; charset=UTF-8\r\n\r\n"+metadata+"\r\n--"+boundary+"\r\nContent-Type: application/json; charset=UTF-8\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        out.write(media);out.write(("\r\n--"+boundary+"--\r\n").getBytes(StandardCharsets.UTF_8));
        // Immutable snapshots avoid overwriting another phone's newer backup.
        JSONObject response=new JSONObject(new String(request("POST","/upload/drive/v3/files?uploadType=multipart&fields=id,createdTime","multipart/related; boundary="+boundary,out.toByteArray()),StandardCharsets.UTF_8));
        id(response.getString("id"));return response.put("savedAt",at);
    }
    JSONObject read(String fileId) throws Exception {return DriveBackupFormat.decode(request("GET","/drive/v3/files/"+id(fileId)+"?alt=media",null,null));}
    void delete(String fileId) throws Exception {request("DELETE","/drive/v3/files/"+id(fileId),null,null);}
}
