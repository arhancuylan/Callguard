package com.callguard;
import android.content.Context;import org.json.*;import java.net.*;import java.io.*;import java.nio.charset.StandardCharsets;
final class CloudClient {
    static boolean configured(){return BuildConfig.API_BASE.startsWith("https://");}
    static JSONObject request(Context context,String method,String route,JSONObject body,boolean authenticated) throws Exception {
        if(!configured())throw new IOException("Bulut sunucusu henüz etkinleştirilmedi.");
        URL url=new URL(BuildConfig.API_BASE.replaceAll("/+$","")+route);if(!"https".equals(url.getProtocol()))throw new IOException("Güvenli bağlantı gerekli.");
        HttpURLConnection connection=(HttpURLConnection)url.openConnection();connection.setConnectTimeout(12000);connection.setReadTimeout(15000);connection.setInstanceFollowRedirects(false);connection.setRequestMethod(method);connection.setRequestProperty("Accept","application/json");
        if(authenticated){String token=SessionVault.token(context);if(token.isEmpty())throw new IOException("Önce hesabına giriş yap.");connection.setRequestProperty("Authorization","Bearer "+token);}
        try{if(body!=null){connection.setDoOutput(true);connection.setRequestProperty("Content-Type","application/json; charset=utf-8");try(OutputStream out=connection.getOutputStream()){out.write(body.toString().getBytes(StandardCharsets.UTF_8));}}
            int status=connection.getResponseCode();InputStream stream=status>=400?connection.getErrorStream():connection.getInputStream();String text="";if(stream!=null)try(InputStream in=stream;ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] bytes=new byte[4096];int size;while((size=in.read(bytes))!=-1){if(out.size()+size>4*1024*1024)throw new IOException("Sunucu yanıtı çok büyük.");out.write(bytes,0,size);}text=out.toString("UTF-8");}
            JSONObject result=text.isEmpty()?new JSONObject():new JSONObject(text);if(status<200||status>=300)throw new IOException(result.optString("error","Sunucu isteği tamamlanamadı ("+status+")."));return result;
        }finally{connection.disconnect();}
    }
    static JSONObject refresh(Context context) throws Exception {JSONObject data=request(context,"GET","/v1/entitlement",null,true);LicenseManager.cache(context,data);return data;}
    static String accountHash(Context context) throws Exception {byte[] digest=java.security.MessageDigest.getInstance("SHA-256").digest(SessionVault.userId(context).getBytes(StandardCharsets.UTF_8));StringBuilder value=new StringBuilder();for(byte b:digest)value.append(String.format(java.util.Locale.ROOT,"%02x",b&255));return value.toString();}
}
