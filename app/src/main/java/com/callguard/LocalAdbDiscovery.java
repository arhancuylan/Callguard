package com.callguard;

import android.content.Context;
import android.net.nsd.*;
import android.os.*;
import java.net.*;
import java.util.*;
import java.util.function.IntConsumer;

/** Short-lived DNS-SD observation, accepting only addresses belonging to this phone. */
final class LocalAdbDiscovery implements AutoCloseable {
    private final NsdManager manager;private final Handler main=new Handler(Looper.getMainLooper());
    private final IntConsumer changed;private final ArrayDeque<NsdServiceInfo> queue=new ArrayDeque<>();
    private boolean closed,registered,resolving;private String selected="";
    private final NsdManager.DiscoveryListener discovery=new NsdManager.DiscoveryListener(){
        public void onDiscoveryStarted(String type){main.post(()->{registered=true;if(closed)stop();});}
        public void onDiscoveryStopped(String type){main.post(()->registered=false);}
        public void onStartDiscoveryFailed(String type,int error){main.post(()->{if(!closed)changed.accept(0);});}
        public void onStopDiscoveryFailed(String type,int error){}
        public void onServiceFound(NsdServiceInfo info){main.post(()->{if(!closed){queue.add(info);resolveNext();}});}
        public void onServiceLost(NsdServiceInfo info){main.post(()->{queue.removeIf(value->value.getServiceName().equals(info.getServiceName()));if(!closed&&selected.equals(info.getServiceName())){selected="";changed.accept(0);}});}
    };
    LocalAdbDiscovery(Context context,boolean pairing,IntConsumer changed){
        manager=(NsdManager)context.getSystemService(Context.NSD_SERVICE);this.changed=changed;
        try{manager.discoverServices(pairing?"_adb-tls-pairing._tcp.":"_adb-tls-connect._tcp.",NsdManager.PROTOCOL_DNS_SD,discovery);}catch(RuntimeException ex){main.post(()->{if(!closed)changed.accept(0);});}
    }
    private void resolveNext(){
        if(closed||resolving||queue.isEmpty())return;NsdServiceInfo info=queue.removeFirst();resolving=true;
        try{manager.resolveService(info,new NsdManager.ResolveListener(){
            public void onResolveFailed(NsdServiceInfo failed,int error){main.post(()->{resolving=false;resolveNext();});}
            public void onServiceResolved(NsdServiceInfo found){main.post(()->{
                resolving=false;
                if(!closed&&local(found.getHost())&&found.getPort()>0&&found.getPort()<=65535){selected=found.getServiceName();changed.accept(found.getPort());}
                resolveNext();
            });}
        });}catch(RuntimeException ex){resolving=false;resolveNext();}
    }
    private static boolean local(InetAddress address){
        if(address==null)return false;if(address.isLoopbackAddress())return true;
        try{return NetworkInterface.getByInetAddress(address)!=null;}catch(SocketException ex){return false;}
    }
    private void stop(){if(registered){registered=false;try{manager.stopServiceDiscovery(discovery);}catch(RuntimeException ignored){}}}
    public void close(){closed=true;queue.clear();stop();}
}
