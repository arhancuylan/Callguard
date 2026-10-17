package com.callguard;

import java.io.IOException;
import java.util.*;
import org.json.*;

/** Local typed device list: no nullable JSON string received from a Shizuku Binder. */
final class PairedCarCatalog {
    static final class Device {
        final String address,name;
        Device(String address,String name){this.address=address;this.name=name;}
    }
    static JSONArray encode(Collection<Device> values)throws Exception{
        if(values==null)throw new IOException("Bluetooth cihaz listesi alınamadı.");
        List<Device> accepted=new ArrayList<>();Set<String> seen=new HashSet<>();
        for(Device value:values){
            if(value==null||value.address==null||!value.address.matches("(?i)([0-9a-f]{2}:){5}[0-9a-f]{2}"))continue;
            String address=value.address.toUpperCase(Locale.ROOT);
            if(seen.add(address))accepted.add(new Device(address,value.name==null||value.name.trim().isEmpty()?"Bluetooth cihazı":value.name.trim()));
        }
        accepted.sort(Comparator.comparing(d->d.name.toLowerCase(Locale.ROOT)));
        JSONArray result=new JSONArray();for(Device value:accepted)result.put(new JSONObject().put("address",value.address).put("name",value.name).put("state",-1));return result;
    }
    private PairedCarCatalog(){}
}
