package com.callguard;

import android.Manifest;
import android.bluetooth.*;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import java.util.*;
import org.json.JSONArray;

/** Pair enumeration belongs to the normal Android app, not the privileged HFP control process. */
final class CarDevices {
    static JSONArray load(Context context)throws Exception{
        if(Build.VERSION.SDK_INT>=31&&context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)!=PackageManager.PERMISSION_GRANTED)throw new SecurityException("Bluetooth izni gerekli");
        BluetoothManager manager=context.getSystemService(BluetoothManager.class);
        BluetoothAdapter adapter=manager==null?null:manager.getAdapter();
        if(adapter==null||!adapter.isEnabled())throw new IllegalStateException("Bluetooth kapalı veya kullanılamıyor.");
        Set<BluetoothDevice> devices=adapter.getBondedDevices();
        if(devices==null)throw new IllegalStateException("Bluetooth cihaz listesi alınamadı.");
        List<PairedCarCatalog.Device> values=new ArrayList<>();
        for(BluetoothDevice device:devices)if(device!=null)values.add(new PairedCarCatalog.Device(device.getAddress(),device.getName()));
        return PairedCarCatalog.encode(values);
    }
    private CarDevices(){}
}
