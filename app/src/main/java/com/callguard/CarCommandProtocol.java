package com.callguard;

import java.io.IOException;
import java.util.Locale;

/** Fixed operations only. Device names, account data and user command text never enter the shell. */
final class CarCommandProtocol {
    static final class HelperFailure extends IOException {HelperFailure(){super("Bluetooth helper operation failed");}}
    static void validate(String[] args){
        if(args==null||args.length==0)throw new IllegalArgumentException();
        if(args[0].equals("probe")&&args.length==1)return;
        if(args.length<2||args[1]==null||!args[1].matches("(?i)([0-9a-f]{2}:){5}[0-9a-f]{2}"))throw new IllegalArgumentException();
        if((args[0].equals("read")||args[0].equals("state"))&&args.length==2)return;
        if(args[0].equals("write")&&args.length==3&&(args[2].equals("0")||args[2].equals("100")))return;
        throw new IllegalArgumentException();
    }
    static String command(String apk,String...args){
        validate(args);
        if(apk==null||!apk.startsWith("/data/app/")||!apk.endsWith(".apk")||!apk.matches("[a-zA-Z0-9_./=+~-]+"))throw new IllegalArgumentException();
        return "CLASSPATH='"+apk+"' /system/bin/timeout 18 /system/bin/app_process -Xhidden-api-policy:disabled /system/bin com.callguard.CarCommand "+String.join(" ",args);
    }
    static int parse(String output)throws IOException{
        Integer found=null;
        for(String line:output.split("\\r?\\n"))if(line.startsWith("CGCAR1:")){
            if(found!=null)throw new IOException("Invalid helper response");
            String value=line.substring(7);if(value.equals("ERROR"))throw new HelperFailure();
            try{found=Integer.valueOf(value);}catch(NumberFormatException ex){throw new IOException("Car helper failed");}
        }
        if(found==null)throw new IOException("Car helper did not respond");return found;
    }
    static Integer completed(String output)throws IOException{
        int newline=output.lastIndexOf('\n');if(newline<0)return null;
        String complete=output.substring(0,newline+1);
        if(!complete.startsWith("CGCAR1:")&&!complete.contains("\nCGCAR1:"))return null;
        return parse(complete);
    }
    static int port(String raw){int port=Integer.parseInt(raw.trim());if(port<1||port>65535)throw new IllegalArgumentException();return port;}
    private CarCommandProtocol(){}
}
