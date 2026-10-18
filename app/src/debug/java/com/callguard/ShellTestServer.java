package com.callguard;

import android.os.*;
import java.util.*;

/** Debug-only fake HFP hardware behind the real shell Binder. Refuses physical devices. */
public final class ShellTestServer {
    public static void main(String[] args){
        if(!Build.HARDWARE.contains("ranchu")&&!Build.HARDWARE.contains("goldfish"))throw new SecurityException("Emulator only");
        if(android.os.Process.myUid()!=2000||args.length!=0)throw new SecurityException("Shell only");
        try{
            Looper.prepareMainLooper();android.content.Context context=CallGuardShell.shellContext();
            ShellAppIdentity identity=ShellAppIdentity.load(context);
            Map<String,Integer> policies=new HashMap<>();policies.put("AA:BB:CC:DD:EE:01",100);policies.put("AA:BB:CC:DD:EE:02",100);
            CallGuardShell.run(context,identity,new HfpShellService.Backend(){
                private void known(String address){if(!policies.containsKey(address))throw new IllegalArgumentException();}
                public int read(String address){known(address);return policies.get(address);}
                public boolean write(String address,int policy){known(address);policies.put(address,policy);return true;}
                public int state(String address){return read(address)==100?2:0;}
            });
        }catch(Exception ex){ex.printStackTrace();System.exit(1);}
    }
    private ShellTestServer(){}
}
