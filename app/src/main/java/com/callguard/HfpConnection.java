package com.callguard;

import android.content.Context;
import java.io.IOException;

/** Select a transport once per journaled operation. Never retry an uncertain write on another one. */
final class HfpConnection {
    static boolean ready(){return ShellBridge.ready()||LocalAdb.ready();}
    static Object token(){return ShellBridge.ready()?ShellBridge.token():LocalAdb.connectionToken();}
    static CarSilencePolicy.Backend backend(Context context)throws Exception{
        if(ShellBridge.ready())return ShellBridge.backend();
        if(!LocalAdb.ready())throw new IOException("No authenticated HFP transport");
        LocalAdb service=LocalAdb.get(context);
        return new CarSilencePolicy.Backend(){
            public int read(String address)throws Exception{return service.command("read",address);}
            public boolean write(String address,int policy)throws Exception{return service.command("write",address,Integer.toString(policy))==1;}
        };
    }
    private HfpConnection(){}
}
