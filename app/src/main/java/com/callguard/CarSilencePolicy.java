package com.callguard;

/** Persist before changing Bluetooth. A failed RPC must never discard recovery information. */
final class CarSilencePolicy {
    static final int FORBIDDEN=0,ALLOWED=100;
    static final class Saved {
        final String address,name;final int previous;
        Saved(String address,String name,int previous){this.address=address;this.name=name;this.previous=previous;}
    }
    interface Backend {int read(String address)throws Exception;boolean write(String address,int policy)throws Exception;}
    interface Journal {Saved load()throws Exception;void save(Saved value)throws Exception;void clear()throws Exception;}
    static void enable(Backend backend,Journal journal,String address,String name)throws Exception{
        if(journal.load()!=null)throw new IllegalStateException("Önce bu cihazın önceki görüşme ayarını geri yükle.");
        if(address==null||!address.matches("(?i)([0-9a-f]{2}:){5}[0-9a-f]{2}"))throw new IllegalArgumentException("Geçersiz araç adresi.");
        int previous=backend.read(address);
        if(previous==FORBIDDEN)throw new IllegalStateException("Görüşme profili zaten kapalı veya bu cihazda desteklenmiyor. Ayarı değiştirmedim.");
        if(previous!=ALLOWED)throw new IllegalStateException("Önceki görüşme ayarı okunamadı; değişiklik yapılmadı.");
        journal.save(new Saved(address,name,previous));
        if(!backend.write(address,FORBIDDEN)||backend.read(address)!=FORBIDDEN)throw new IllegalStateException("Kapatma doğrulanamadı. Geri yükleme kaydı korundu.");
    }
    static void restore(Backend backend,Journal journal)throws Exception{
        Saved saved=journal.load();if(saved==null)return;
        if(saved.previous!=ALLOWED)throw new IllegalStateException("Geri yükleme kaydı geçersiz. Bluetooth ayarından Aramalar’ı kontrol et.");
        int current=backend.read(saved.address);
        if(current!=saved.previous){
            if(current!=FORBIDDEN)throw new IllegalStateException("Araç ayarı okunamadı. Geri yükleme kaydı korundu.");
            if(!backend.write(saved.address,saved.previous)||backend.read(saved.address)!=saved.previous)throw new IllegalStateException("Geri yükleme doğrulanamadı. Kayıt korundu.");
        }
        journal.clear();
    }
    private CarSilencePolicy(){}
}
