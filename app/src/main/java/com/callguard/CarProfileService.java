package com.callguard;

import android.annotation.SuppressLint;
import android.bluetooth.*;
import android.content.Context;
import android.os.RemoteException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.json.*;

/** Shizuku user service. Only one paired device's HFP policy; no shell commands or audio routing. */
@SuppressLint("MissingPermission") // Executes under the explicitly authorized Shizuku shell/root identity.
public final class CarProfileService extends ICarProfile.Stub {
    private Context context;
    private BluetoothAdapter adapter;
    private volatile BluetoothHeadset headset;
    public CarProfileService(){}
    public CarProfileService(Context context){this.context=context;}
    private synchronized BluetoothHeadset profile()throws Exception{
        if(context==null){
            Class<?> thread=Class.forName("android.app.ActivityThread");
            Object instance=thread.getMethod("currentActivityThread").invoke(null);
            if(instance==null)instance=thread.getMethod("systemMain").invoke(null);
            context=(Context)thread.getMethod("getSystemContext").invoke(instance);
        }
        if(adapter==null){
            try{BluetoothManager manager=(BluetoothManager)context.getSystemService(Context.BLUETOOTH_SERVICE);if(manager!=null)adapter=manager.getAdapter();}catch(RuntimeException ignored){}
            if(adapter==null){
                // app_process may not have initialized the framework's Bluetooth registry.
                try{
                    Class<?> registry=Class.forName("android.bluetooth.BluetoothServiceManager");
                    Object service=registry.getDeclaredConstructor().newInstance();
                    Class.forName("android.bluetooth.BluetoothFrameworkInitializer").getMethod("setBluetoothServiceManager",registry).invoke(null,service);
                }catch(ReflectiveOperationException|RuntimeException ignored){}
                adapter=BluetoothAdapter.getDefaultAdapter();
            }
        }
        if(adapter==null||!adapter.isEnabled())throw new IllegalStateException("Bluetooth kapalı veya kullanılamıyor.");
        if(headset!=null)return headset;
        CountDownLatch ready=new CountDownLatch(1);
        boolean requested=adapter.getProfileProxy(context,new BluetoothProfile.ServiceListener(){
            @Override public void onServiceConnected(int type,BluetoothProfile proxy){headset=(BluetoothHeadset)proxy;ready.countDown();}
            @Override public void onServiceDisconnected(int type){headset=null;}
        },BluetoothProfile.HEADSET);
        if(!requested||!ready.await(6,TimeUnit.SECONDS)||headset==null)throw new IllegalStateException("Araç görüşme profiline erişilemedi.");
        return headset;
    }
    private BluetoothDevice device(String address)throws Exception{
        profile();if(address==null||!address.matches("(?i)([0-9a-f]{2}:){5}[0-9a-f]{2}"))throw new IllegalArgumentException();
        BluetoothDevice device=adapter.getRemoteDevice(address);
        if(device.getBondState()!=BluetoothDevice.BOND_BONDED)throw new IllegalStateException("Araç artık eşleşmiş değil.");
        return device;
    }
    private RemoteException failed(){return new RemoteException("Shizuku Bluetooth işlemi desteklenmedi. Bluetooth açık mı ve Shizuku izni var mı kontrol et.");}
    @Override public String pairedDevices()throws RemoteException{
        try{
            BluetoothHeadset proxy=profile();JSONArray devices=new JSONArray();
            for(BluetoothDevice device:adapter.getBondedDevices())devices.put(new JSONObject().put("address",device.getAddress()).put("name",device.getName()==null?"Bluetooth cihazı":device.getName()).put("state",proxy.getConnectionState(device)));
            return devices.toString();
        }catch(Exception ex){throw failed();}
    }
    @Override public int readPolicy(String address)throws RemoteException{
        try{BluetoothHeadset proxy=profile();return (Integer)BluetoothHeadset.class.getMethod("getConnectionPolicy",BluetoothDevice.class).invoke(proxy,device(address));}
        catch(Exception ex){throw failed();}
    }
    @Override public boolean writePolicy(String address,int policy)throws RemoteException{
        if(policy!=CarSilencePolicy.FORBIDDEN&&policy!=CarSilencePolicy.ALLOWED)throw failed();
        try{
            BluetoothHeadset proxy=profile();BluetoothDevice device=device(address);
            boolean changed=(Boolean)BluetoothHeadset.class.getMethod("setConnectionPolicy",BluetoothDevice.class,int.class).invoke(proxy,device,policy);
            // Restore connection availability. Failure to reconnect is distinct from restoring the policy.
            if(changed&&policy==CarSilencePolicy.ALLOWED)try{BluetoothHeadset.class.getMethod("connect",BluetoothDevice.class).invoke(proxy,device);}catch(ReflectiveOperationException ignored){}
            return changed;
        }catch(Exception ex){throw failed();}
    }
    @Override public int connectionState(String address)throws RemoteException{try{return profile().getConnectionState(device(address));}catch(Exception ex){throw failed();}}
    @Override public void destroy(){if(adapter!=null&&headset!=null)adapter.closeProfileProxy(BluetoothProfile.HEADSET,headset);System.exit(0);}
}
