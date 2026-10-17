package com.callguard;

import java.util.*;
import org.json.*;

/** Device-local, independently recoverable configurations. Inactive preferences are retained. */
final class BluetoothSilenceStore {
    interface Storage {String current();String legacy();void write(String json,boolean removeLegacy)throws Exception;}
    static final class Entry {
        final String address,name;final Integer previous;
        Entry(String address,String name,Integer previous){this.address=address;this.name=name;this.previous=previous;}
        boolean recovery(){return previous!=null;}
    }
    private final Storage storage;private LinkedHashMap<String,Entry> values;
    BluetoothSilenceStore(Storage storage)throws Exception{
        this.storage=storage;values=decode(storage.current());String old=storage.legacy();
        if(old!=null){
            JSONObject legacy=new JSONObject(old);String address=address(legacy.getString("address"));int previous=legacy.getInt("previous");if(previous!=100)throw new IllegalStateException("Invalid legacy recovery");
            Entry existing=values.get(address);if(existing!=null&&existing.previous!=null&&existing.previous!=previous)throw new IllegalStateException("Conflicting recovery");
            LinkedHashMap<String,Entry> migrated=new LinkedHashMap<>(values);migrated.put(address,new Entry(address,name(legacy.optString("name","Bluetooth cihazı")),previous));
            storage.write(encode(migrated),true);values=migrated;
        }
    }
    static String address(String value){if(value==null||!value.matches("(?i)([0-9a-f]{2}:){5}[0-9a-f]{2}"))throw new IllegalArgumentException("Geçersiz cihaz adresi.");return value.toUpperCase(Locale.ROOT);}
    private static String name(String value){return value==null||value.trim().isEmpty()?"Bluetooth cihazı":value.trim();}
    private static LinkedHashMap<String,Entry> decode(String raw)throws Exception{
        LinkedHashMap<String,Entry> result=new LinkedHashMap<>();if(raw==null)return result;JSONObject document=new JSONObject(raw);if(document.getInt("version")!=1)throw new IllegalStateException("Unsupported configuration");JSONArray array=document.getJSONArray("devices");
        for(int i=0;i<array.length();i++){JSONObject object=array.getJSONObject(i);String key=address(object.getString("address"));if(result.containsKey(key))throw new IllegalStateException("Duplicate recovery");if(!object.has("previous"))throw new IllegalStateException("Missing recovery field");Integer previous=object.isNull("previous")?null:object.getInt("previous");if(previous!=null&&previous!=100)throw new IllegalStateException("Invalid recovery");result.put(key,new Entry(key,name(object.optString("name")),previous));}return result;
    }
    private static String encode(Map<String,Entry> entries)throws Exception{
        JSONArray array=new JSONArray();for(Entry entry:entries.values())array.put(new JSONObject().put("address",entry.address).put("name",entry.name).put("previous",entry.previous==null?JSONObject.NULL:entry.previous));return new JSONObject().put("version",1).put("devices",array).toString();
    }
    private void commit(LinkedHashMap<String,Entry> next)throws Exception{storage.write(encode(next),false);values=next;}
    synchronized List<Entry> entries(){return new ArrayList<>(values.values());}
    synchronized Entry get(String address){return values.get(address(address));}
    synchronized void remember(String address,String name)throws Exception{
        String key=address(address);Entry old=values.get(key);LinkedHashMap<String,Entry> next=new LinkedHashMap<>(values);next.put(key,new Entry(key,name(name),old==null?null:old.previous));commit(next);
    }
    synchronized void forget(String address)throws Exception{
        String key=address(address);Entry entry=values.get(key);if(entry!=null&&entry.recovery())throw new IllegalStateException("Önce bu cihazın önceki görüşme ayarını geri yükle.");LinkedHashMap<String,Entry> next=new LinkedHashMap<>(values);next.remove(key);commit(next);
    }
    CarSilencePolicy.Journal journal(String target){
        final String key=address(target);
        return new CarSilencePolicy.Journal(){
            public CarSilencePolicy.Saved load(){synchronized(BluetoothSilenceStore.this){Entry entry=values.get(key);return entry==null||entry.previous==null?null:new CarSilencePolicy.Saved(key,entry.name,entry.previous);}}
            public void save(CarSilencePolicy.Saved saved)throws Exception{synchronized(BluetoothSilenceStore.this){
                if(!key.equals(address(saved.address))||saved.previous!=100)throw new IllegalArgumentException();Entry old=values.get(key);if(old!=null&&old.recovery())throw new IllegalStateException("Bu cihazın geri yükleme kaydı zaten var.");
                LinkedHashMap<String,Entry> next=new LinkedHashMap<>(values);next.put(key,new Entry(key,name(saved.name),saved.previous));commit(next);
            }}
            public void clear()throws Exception{synchronized(BluetoothSilenceStore.this){Entry old=values.get(key);if(old==null)return;LinkedHashMap<String,Entry> next=new LinkedHashMap<>(values);next.put(key,new Entry(key,old.name,null));commit(next);}}
        };
    }
}
