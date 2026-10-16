package com.callguard;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.provider.*;
import android.service.notification.NotificationListenerService;
import android.text.*;
import android.view.*;
import android.widget.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;

/** Native, responsive Turkish UI. No remote content or sample records. */
public final class MainActivity extends Activity {
    private static final int GREEN=Color.rgb(8,99,78), INK=Color.rgb(13,22,38), MUTED=Color.rgb(99,110,125), BG=Color.rgb(245,247,244), BORDER=Color.rgb(222,228,225);
    private Store store; private LinearLayout root,body,nav; private ScrollView scroll;
    private int tab=0; private boolean white=true; private boolean rendering=false;
    private String search=""; private LinearLayout peopleRows;
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final BroadcastReceiver refresh=new BroadcastReceiver(){@Override public void onReceive(Context c,Intent i){if(!rendering)render();}};
    @Override public void onCreate(Bundle state){
        super.onCreate(state);store=new Store(this);
        if(state!=null){tab=state.getInt("tab");white=state.getBoolean("white",true);search=state.getString("search","");}
        if(Build.VERSION.SDK_INT>=33)registerReceiver(refresh,new IntentFilter(getPackageName()+".CHANGED"),Context.RECEIVER_NOT_EXPORTED);
        else registerReceiver(refresh,new IntentFilter(getPackageName()+".CHANGED"));
        getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        render();
    }
    @Override protected void onResume(){super.onResume();if(store!=null)render();}
    @Override protected void onSaveInstanceState(Bundle out){out.putInt("tab",tab);out.putBoolean("white",white);out.putString("search",search);super.onSaveInstanceState(out);}
    @Override protected void onDestroy(){unregisterReceiver(refresh);worker.shutdownNow();store.close();super.onDestroy();}
    private int dp(float n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private GradientDrawable bg(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
    private GradientDrawable outline(int color){GradientDrawable d=bg(color,14);d.setStroke(dp(1),BORDER);return d;}
    private LinearLayout column(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
    private LinearLayout row(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.HORIZONTAL);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
    private void gap(LinearLayout l,int n){View v=new View(this);l.addView(v,new LinearLayout.LayoutParams(1,dp(n)));}
    private TextView text(String value,int sp,int color,boolean bold){TextView t=new TextView(this);t.setText(value);t.setTextSize(sp);t.setTextColor(color);t.setFontFeatureSettings("kern");if(bold)t.setTypeface(android.graphics.Typeface.create("sans-serif-medium",android.graphics.Typeface.NORMAL));return t;}
    private TextView title(String value){return text(value,27,INK,true);}
    private void heading(String title,String subtitle){body.addView(title(title));gap(body,4);body.addView(text(subtitle,14,MUTED,false));gap(body,22);}
    private LinearLayout card(){LinearLayout c=column();c.setPadding(dp(16),dp(15),dp(16),dp(15));c.setBackground(bg(Color.WHITE,18));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=dp(12);body.addView(c,p);return c;}
    private TextView button(String label,boolean filled,Runnable action){TextView b=text(label,15,filled?Color.WHITE:INK,true);b.setGravity(Gravity.CENTER);b.setMinHeight(dp(48));b.setPadding(dp(12),dp(10),dp(12),dp(10));b.setBackground(filled?bg(GREEN,16):outline(Color.TRANSPARENT));b.setClickable(true);b.setFocusable(true);b.setOnClickListener(v->action.run());return b;}
    private Icon icon(String type,int color,int size){Icon i=new Icon(type,color);i.setLayoutParams(new LinearLayout.LayoutParams(dp(size),dp(size)));return i;}
    private boolean access(){String value=Settings.Secure.getString(getContentResolver(),"enabled_notification_listeners");if(value==null)return false;ComponentName expected=new ComponentName(this,CallNotificationService.class);for(String item:value.split(":")){if(expected.equals(ComponentName.unflattenFromString(item)))return true;}return false;}
    private void render(){
        if(isFinishing())return;rendering=true;
        root=column();root.setBackgroundColor(BG);
        root.setOnApplyWindowInsetsListener((view,insets)->{view.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());return insets.consumeSystemWindowInsets();});
        setContentView(root);root.requestApplyInsets();
        scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setClipToPadding(false);
        body=column();body.setPadding(dp(22),dp(20),dp(22),dp(18));scroll.addView(body,new ScrollView.LayoutParams(-1,-2));root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        if(tab==0)home();else if(tab==1)people();else if(tab==2)history();else settings();
        nav=row();nav.setPadding(dp(8),dp(9),dp(8),dp(10));nav.setBackgroundColor(Color.WHITE);
        String[] names={"Koruma","Kişiler","Geçmiş","Ayarlar"};String[] icons={"shield","people","clock","settings"};
        for(int j=0;j<4;j++){final int index=j;LinearLayout item=column();item.setGravity(Gravity.CENTER);item.setPadding(dp(2),dp(2),dp(2),dp(2));item.addView(icon(icons[j],j==tab?GREEN:MUTED,23));gap(item,5);TextView label=text(names[j],12,j==tab?GREEN:MUTED,j==tab);label.setGravity(Gravity.CENTER);item.addView(label,new LinearLayout.LayoutParams(-1,-2));item.setContentDescription(names[j]+" sekmesi");item.setFocusable(true);item.setOnClickListener(v->{tab=index;render();});nav.addView(item,new LinearLayout.LayoutParams(0,dp(56),1));}
        root.addView(nav,new LinearLayout.LayoutParams(-1,-2));rendering=false;
    }
    private void home(){
        LinearLayout brand=row();brand.addView(icon("shield",GREEN,31));TextView word=text("CallGuard",22,INK,true);LinearLayout.LayoutParams wp=new LinearLayout.LayoutParams(0,-2,1);wp.leftMargin=dp(9);brand.addView(word,wp);
        TextView menu=text("⋮",26,MUTED,true);menu.setGravity(Gravity.CENTER);menu.setContentDescription("Diğer seçenekler");menu.setOnClickListener(v->{PopupMenu p=new PopupMenu(this,menu);p.getMenu().add("Ayarlar").setOnMenuItemClickListener(i->{tab=3;render();return true;});p.getMenu().add("Program hakkında").setOnMenuItemClickListener(i->{about();return true;});p.show();});brand.addView(menu,new LinearLayout.LayoutParams(dp(48),dp(48)));body.addView(brand);gap(body,20);
        heading("Huzurla devam et.","WhatsApp aramalarını yönet.");
        LinearLayout protect=card();GradientDrawable grad=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{GREEN,Color.rgb(2,80,65)});grad.setCornerRadius(dp(18));protect.setBackground(grad);
        LinearLayout pr=row();pr.addView(icon("shield",Color.WHITE,60));LinearLayout lines=column();TextView status=text(store.enabled()?"Koruma açık":"Koruma kapalı",18,Color.WHITE,true);lines.addView(status);gap(lines,5);lines.addView(text(access()?"● Bildirim erişimi etkin":"○ Bildirim erişimi gerekli",12,Color.rgb(209,238,220),false));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1);lp.leftMargin=dp(12);pr.addView(lines,lp);
        Switch toggle=new Switch(this);toggle.setContentDescription("Koruma anahtarı");toggle.setChecked(store.enabled());toggle.setThumbTintList(android.content.res.ColorStateList.valueOf(Color.WHITE));toggle.setTrackTintList(android.content.res.ColorStateList.valueOf(Color.rgb(139,210,153)));toggle.setOnCheckedChangeListener((b,on)->{if(on&&!access()){toggle.setChecked(false);permissionExplanation();return;}store.setting("enabled",on);});pr.addView(toggle);protect.addView(pr);
        if(!access()){TextView permission=button("Bildirim erişimini aç",false,this::permissionExplanation);permission.setTextColor(Color.WHITE);gap(protect,10);protect.addView(permission);}
        LinearLayout rules=card();rules.addView(text("Engelleme kuralı",16,INK,true));gap(rules,8);
        rule(rules,"Tüm gelen aramaları reddet","Seçilen arama türünde herkesi reddet.",Rules.Mode.ALL);
        rule(rules,"Beyaz liste","Yalnızca izin verdiklerin ulaşabilsin.",Rules.Mode.WHITELIST);
        rule(rules,"Kara liste","Yalnızca listedeki kişileri reddet.",Rules.Mode.BLACKLIST);
        LinearLayout stats=row();List<Rules.Contact> contacts=store.contacts();int count=0;boolean showWhite=store.mode()!=Rules.Mode.BLACKLIST;for(Rules.Contact c:contacts)if(c.white==showWhite)count++;
        stats.addView(stat("people",String.valueOf(count),showWhite?"İzin verilen kişi":"Kara listedeki kişi"),new LinearLayout.LayoutParams(0,-2,1));View spacer=new View(this);stats.addView(spacer,new LinearLayout.LayoutParams(dp(12),1));stats.addView(stat("phone",String.valueOf(store.today()),"Bugünkü reddetme isteği"),new LinearLayout.LayoutParams(0,-2,1));body.addView(stats);gap(body,13);
        body.addView(button("Kişileri düzenle    ›",true,()->{white=store.mode()!=Rules.Mode.BLACKLIST;tab=1;render();}));gap(body,10);
        TextView notice=text("ⓘ  Bağlı cihazlarda kısa süreli zil duyulabilir.",12,GREEN,false);notice.setPadding(dp(12),dp(11),dp(12),dp(11));notice.setBackground(bg(Color.rgb(230,242,231),12));body.addView(notice);
    }
    private LinearLayout stat(String type,String value,String label){LinearLayout c=column();c.setPadding(dp(14),dp(13),dp(14),dp(13));c.setBackground(bg(Color.WHITE,16));LinearLayout r=row();r.addView(icon(type,GREEN,24));TextView val=text(value,27,INK,true);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-2,-2);p.leftMargin=dp(9);r.addView(val,p);c.addView(r);gap(c,5);TextView desc=text(label,12,MUTED,false);desc.setMinHeight(dp(34));c.addView(desc);return c;}
    private void rule(LinearLayout parent,String title,String subtitle,Rules.Mode mode){
        LinearLayout r=row();r.setPadding(0,dp(8),0,dp(8));RadioButton radio=new RadioButton(this);radio.setButtonTintList(android.content.res.ColorStateList.valueOf(store.mode()==mode?GREEN:MUTED));radio.setChecked(store.mode()==mode);radio.setClickable(false);radio.setFocusable(false);r.addView(radio,new LinearLayout.LayoutParams(dp(38),dp(42)));LinearLayout labels=column();labels.addView(text(title,14,INK,true));gap(labels,2);labels.addView(text(subtitle,12,MUTED,false));r.addView(labels,new LinearLayout.LayoutParams(0,-2,1));r.setContentDescription(title);r.setFocusable(true);View.OnClickListener select=v->{store.setting("rule",mode.name());render();};r.setOnClickListener(select);radio.setClickable(true);radio.setOnClickListener(select);parent.addView(r);
    }
    private void segmented(LinearLayout parent,String[] labels,int selected,java.util.function.IntConsumer choice){LinearLayout s=row();s.setBackground(outline(Color.rgb(239,243,240)));for(int i=0;i<labels.length;i++){final int index=i;TextView b=text(labels[i],13,i==selected?Color.WHITE:MUTED,i==selected);b.setGravity(Gravity.CENTER);b.setPadding(dp(4),dp(12),dp(4),dp(12));b.setMinHeight(dp(44));if(i==selected)b.setBackground(bg(GREEN,14));b.setFocusable(true);b.setOnClickListener(v->choice.accept(index));s.addView(b,new LinearLayout.LayoutParams(0,-2,1));}parent.addView(s);}
    private EditText input(String hint){EditText e=new EditText(this);e.setSingleLine(true);e.setTextSize(14);e.setTextColor(INK);e.setHintTextColor(MUTED);e.setHint(hint);e.setPadding(dp(14),dp(13),dp(14),dp(13));e.setBackground(outline(Color.WHITE));return e;}
    private void people(){heading("Senin kuralların","Kimlerin ulaşabileceğini seç.");segmented(body,new String[]{"Beyaz liste","Kara liste"},white?0:1,i->{white=i==0;search="";render();});gap(body,15);
        EditText filter=input("Ad veya numara ara");filter.setText(search);body.addView(filter);gap(body,16);peopleRows=column();body.addView(peopleRows);fillPeople();filter.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int start,int count,int after){}public void onTextChanged(CharSequence s,int start,int before,int count){search=s.toString();fillPeople();}public void afterTextChanged(Editable e){}});
        gap(body,18);body.addView(button("＋  Kişi ekle",true,this::addChoice));gap(body,10);body.addView(text("Ad eşleştirmesi WhatsApp'ta görünen ada göre yapılır. Aynı adı kullanan kişileri ayırt etmek için numara bilgisi gerekir.",12,MUTED,false));
    }
    private void fillPeople(){peopleRows.removeAllViews();List<Rules.Contact> all=store.contacts();int total=0;for(Rules.Contact c:all)if(c.white==white)total++;peopleRows.addView(text((white?"İzin verilenler":"Engellenenler")+" · "+total+" kişi",14,MUTED,true));gap(peopleRows,12);int shown=0;
        for(Rules.Contact c:all)if(c.white==white&&(Rules.label(c.name).contains(Rules.label(search))||c.number.contains(search))){shown++;LinearLayout r=row();r.setPadding(dp(12),dp(16),dp(12),dp(16));r.setBackground(bg(Color.WHITE,17));
            TextView avatar=contactAvatar(c.name);r.addView(avatar,new LinearLayout.LayoutParams(dp(48),dp(48)));
            LinearLayout labels=column();TextView name=text(c.name,16,INK,true);name.setMaxLines(2);labels.addView(name);gap(labels,4);labels.addView(text(c.number,12,MUTED,false));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,1);p.leftMargin=dp(12);p.rightMargin=dp(6);r.addView(labels,p);
            TextView remove=button("Kaldır",false,()->new AlertDialog.Builder(this).setTitle("Kişiyi kaldır?").setMessage(c.name+" bu listeden kaldırılacak.").setNegativeButton("Vazgeç",null).setPositiveButton("Kaldır",(d,w)->{store.remove(c.id);render();}).show());remove.setTextSize(12);remove.setContentDescription(c.name+" kişisini kaldır");r.addView(remove,new LinearLayout.LayoutParams(dp(65),dp(44)));LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(-1,-2);rp.bottomMargin=dp(10);peopleRows.addView(r,rp);
        }if(shown==0){TextView empty=text(total==0?"Listen henüz boş. Kişi ekleyerek başlayabilirsin.":"Aramana uygun kişi bulunamadı.",15,MUTED,false);empty.setPadding(dp(6),dp(26),dp(6),dp(26));peopleRows.addView(empty);}
    }
    private String initials(String name){String[] parts=name.trim().split("\\s+");String first=parts[0].isEmpty()?"?":parts[0].substring(0,1);return (first+(parts.length>1?parts[parts.length-1].substring(0,1):"")).toUpperCase(new Locale("tr"));}
    private void addChoice(){
        LinearLayout panel=column();panel.setPadding(dp(24),dp(24),dp(24),dp(12));
        panel.addView(text(white?"Beyaz listeye ekle":"Kara listeye ekle",22,INK,true));gap(panel,8);panel.addView(text("Kişiyi nasıl eklemek istersin?",14,MUTED,false));gap(panel,20);
        AlertDialog dialog=new AlertDialog.Builder(this).setView(panel).setNegativeButton("Vazgeç",null).create();
        String[] titles={"Rehberden seç","Elle numara ekle"};String[] descriptions={"Telefonundaki kişiler arasından seç.","Ad ve telefon numarasıyla yeni kayıt oluştur."};
        for(int index=0;index<2;index++){final int choice=index;LinearLayout option=row();option.setPadding(dp(16),dp(16),dp(16),dp(16));option.setBackground(bg(index==0?0xffe4efe8:0xfff0f2ef,18));option.addView(icon(index==0?"people":"phone",GREEN,28));LinearLayout labels=column();labels.addView(text(titles[index],16,INK,true));gap(labels,5);labels.addView(text(descriptions[index],12,MUTED,false));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1);lp.leftMargin=dp(16);option.addView(labels,lp);option.setFocusable(true);option.setOnClickListener(v->{dialog.dismiss();if(choice==0)requestContacts();else manual();});panel.addView(option,new LinearLayout.LayoutParams(-1,-2));gap(panel,12);}
        dialog.show();if(dialog.getWindow()!=null)dialog.getWindow().setBackgroundDrawable(bg(Color.WHITE,24));
    }
    private void manual(){manual("","",white);}
    private void manual(String preName,String preNumber,boolean destinationWhite){LinearLayout form=column();form.setPadding(dp(24),dp(8),dp(24),dp(8));EditText name=input("WhatsApp'ta görünen ad");EditText number=input("Telefon numarası (+90…)");number.setInputType(android.text.InputType.TYPE_CLASS_PHONE);name.setText(preName);number.setText(preNumber);form.addView(name);gap(form,12);form.addView(number);gap(form,8);form.addView(text("Türkiye numaraları 05… biçiminde de yazılabilir. Diğer ülkeler için +ülke kodu kullan.",12,MUTED,false));AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Kişi ekle").setView(form).setNegativeButton("Vazgeç",null).setPositiveButton("Kaydet",null).create();dialog.setOnShowListener(d->dialog.getButton(-1).setOnClickListener(v->{try{store.add(name.getText().toString(),number.getText().toString(),destinationWhite);dialog.dismiss();render();}catch(IllegalArgumentException ex){number.setError(ex.getMessage());}}));dialog.show();}
    private void requestContacts(){if(checkSelfPermission(Manifest.permission.READ_CONTACTS)==PackageManager.PERMISSION_GRANTED)pickContacts();else new AlertDialog.Builder(this).setTitle("Rehberden kişi seç").setMessage("Kişi seçebilmek için rehber erişimi gerekir. Seçtiğin ad ve numara yalnızca bu cihazda saklanır.").setNegativeButton("Vazgeç",null).setPositiveButton("Devam",(d,w)->requestPermissions(new String[]{Manifest.permission.READ_CONTACTS},40)).show();}
    @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] results){super.onRequestPermissionsResult(request,permissions,results);if(request==40){if(results.length>0&&results[0]==PackageManager.PERMISSION_GRANTED)pickContacts();else new AlertDialog.Builder(this).setTitle("Rehber izni verilmedi").setMessage("Elle numara ekleyebilir veya uygulama ayarlarından rehber iznini açabilirsin.").setNegativeButton("Kapat",null).setPositiveButton("Uygulama ayarları",(d,w)->startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,android.net.Uri.parse("package:"+getPackageName())))).show();}}
    private void pickContacts(){ProgressDialog loading=ProgressDialog.show(this,"Rehber","Kişiler yükleniyor…",true,false);worker.execute(()->{ArrayList<String[]> values=new ArrayList<>();String failure=null;
        try(Cursor c=getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,new String[]{ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,ContactsContract.CommonDataKinds.Phone.NUMBER,ContactsContract.CommonDataKinds.Phone.CONTACT_ID,ContactsContract.CommonDataKinds.Phone.TYPE,ContactsContract.CommonDataKinds.Phone.LABEL},null,null,ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME+" COLLATE LOCALIZED ASC")){
            if(c!=null)while(c.moveToNext()){String number=Rules.number(c.getString(1));String name=c.getString(0);if(!number.isEmpty())values.add(new String[]{name==null||name.trim().isEmpty()?number:name,number,c.getString(2),ContactsContract.CommonDataKinds.Phone.getTypeLabel(getResources(),c.getInt(3),c.getString(4)).toString()});}
        }catch(SecurityException e){failure="Rehber izni kaldırılmış. İzni yeniden aç.";}
        final String error=failure;runOnUiThread(()->{if(isFinishing()||isDestroyed())return;loading.dismiss();if(error!=null){toast(error);return;}showContactPicker(values);});});}
    private TextView contactAvatar(String name){
        int[] colors={0xffdce9ff,0xfff9e0d9,0xffe8def8,0xffd6eee5,0xfff9eabf,0xffd7edf3};
        int[] inks={0xff315c9a,0xff984f40,0xff725195,0xff276653,0xff826219,0xff356b7b};
        int index=Math.floorMod(Rules.label(name).hashCode(),colors.length);
        TextView avatar=text(initials(name),18,inks[index],true);avatar.setGravity(Gravity.CENTER);avatar.setBackground(bg(colors[index],48));
        avatar.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);return avatar;
    }
    private String contactLetter(String name){String clean=name.trim();return clean.isEmpty()?"#":clean.substring(0,clean.offsetByCodePoints(0,1)).toUpperCase(new Locale("tr"));}
    private void showContactPicker(ArrayList<String[]> values){
        final boolean destinationWhite=white;
        final List<ContactBook.Group> groups=ContactBook.group(values);
        final int accent=destinationWhite?GREEN:0xff9c414c;
        final int tint=destinationWhite?0xffe0eee7:0xfff8e8e9;
        Set<String> already=new HashSet<>();for(Rules.Contact c:store.contacts())if(c.white==destinationWhite)already.add(c.number);
        Dialog dialog=new Dialog(this,android.R.style.Theme_Material_Light_NoActionBar);
        LinearLayout panel=column();panel.setBackgroundColor(BG);panel.setPadding(dp(20),dp(12),dp(20),dp(12));
        LinearLayout toolbar=row();TextView back=text("‹",34,INK,false);back.setGravity(Gravity.CENTER);back.setContentDescription("Rehberi kapat");back.setOnClickListener(v->dialog.dismiss());toolbar.addView(back,new LinearLayout.LayoutParams(dp(48),dp(48)));
        TextView heading=text("Rehberden seç",23,INK,true);LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(0,-2,1);hp.leftMargin=dp(8);toolbar.addView(heading,hp);panel.addView(toolbar);gap(panel,16);
        TextView badge=text(destinationWhite?"Beyaz listeye ekle":"Kara listeye ekle",13,accent,true);badge.setPadding(dp(14),dp(8),dp(14),dp(8));badge.setBackground(bg(tint,20));panel.addView(badge,new LinearLayout.LayoutParams(-2,-2));gap(panel,10);
        panel.addView(text("Kişiye dokun, numaralarını seç. Birden fazla kişi ekleyebilirsin.",14,MUTED,false));gap(panel,18);
        LinearLayout searchBox=row();searchBox.setPadding(dp(16),0,dp(6),0);searchBox.setBackground(bg(0xffe9eeea,28));searchBox.addView(icon("search",MUTED,22));
        EditText filter=input("Rehberde ara");filter.setBackgroundColor(Color.TRANSPARENT);filter.setTextSize(16);filter.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH);searchBox.addView(filter,new LinearLayout.LayoutParams(0,dp(54),1));
        TextView clear=text("×",24,MUTED,false);clear.setGravity(Gravity.CENTER);clear.setContentDescription("Rehber aramasını temizle");clear.setVisibility(View.GONE);clear.setOnClickListener(v->filter.setText(""));searchBox.addView(clear,new LinearLayout.LayoutParams(dp(44),dp(48)));panel.addView(searchBox);gap(panel,18);
        TextView count=text(groups.size()+" kişi",12,MUTED,true);count.setPadding(dp(10),0,0,dp(8));panel.addView(count);
        LinearLayout content=column();panel.addView(content,new LinearLayout.LayoutParams(-1,0,1));
        ListView list=new ListView(this);list.setDivider(null);list.setSelector(new android.graphics.drawable.ColorDrawable(0xffe2ebe5));list.setClipToPadding(false);list.setPadding(0,0,0,dp(16));list.setFastScrollEnabled(true);list.setContentDescription("Telefon rehberi");content.addView(list,new LinearLayout.LayoutParams(-1,-1));
        TextView empty=text(values.isEmpty()?"Rehberde geçerli numarası olan kişi bulunamadı.":"Aramana uygun kişi bulunamadı.",16,MUTED,false);empty.setGravity(Gravity.CENTER);empty.setPadding(dp(24),dp(40),dp(24),dp(40));content.addView(empty,new LinearLayout.LayoutParams(-1,-1));list.setEmptyView(empty);
        ArrayList<ContactBook.Group> filtered=new ArrayList<>(groups);
        Map<String,Rules.Contact> selected=new LinkedHashMap<>();Set<String> expanded=new HashSet<>();
        LinearLayout footer=column();footer.setPadding(0,dp(10),0,0);TextView selection=text("0 numara seçildi",13,MUTED,true);footer.addView(selection);gap(footer,8);
        TextView save=button("Seçilenleri ekle",true,()->{});save.setContentDescription("Seçilen numaraları ekle");footer.addView(save);gap(footer,8);footer.addView(text("WhatsApp yalnızca ad bildirirse aynı kişinin numaraları ayırt edilemeyebilir.",11,MUTED,false));panel.addView(footer);
        Runnable updateSelection=()->{selection.setText(selected.size()+" numara seçildi");save.setText(selected.isEmpty()?"Seçilenleri ekle":"Seçilenleri ekle ("+selected.size()+")");save.setEnabled(!selected.isEmpty());save.setAlpha(selected.isEmpty()?0.45f:1f);save.setBackground(bg(accent,16));};updateSelection.run();
        BaseAdapter adapter=new BaseAdapter(){
            @Override public int getCount(){return filtered.size();}
            @Override public Object getItem(int pos){return filtered.get(pos);}
            @Override public long getItemId(int pos){return pos;}
            @Override public View getView(int pos,View convert,ViewGroup parent){
                ContactBook.Group contact=filtered.get(pos);boolean open=expanded.contains(contact.id);int chosen=0;for(ContactBook.Phone phone:contact.phones)if(selected.containsKey(phone.number))chosen++;
                LinearLayout cell=column();String letter=contactLetter(contact.name);
                if(pos==0||!letter.equals(contactLetter(filtered.get(pos-1).name))){TextView section=text(letter,13,accent,true);section.setPadding(dp(12),dp(18),0,dp(6));cell.addView(section);}
                LinearLayout line=row();line.setPadding(dp(8),dp(12),dp(8),dp(12));line.setMinimumHeight(dp(80));line.addView(contactAvatar(contact.name),new LinearLayout.LayoutParams(dp(48),dp(48)));if(chosen>0)line.setBackground(bg(tint,18));
                LinearLayout labels=column();TextView name=text(contact.name,16,INK,true);name.setMaxLines(2);labels.addView(name);gap(labels,4);labels.addView(text(contact.phones.size()+" numara"+(chosen>0?" · "+chosen+" seçili":""),13,MUTED,false));
                LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1);lp.leftMargin=dp(16);lp.rightMargin=dp(12);line.addView(labels,lp);
                TextView expand=text(open?"⌃":"⌄",24,accent,false);expand.setGravity(Gravity.CENTER);expand.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);line.addView(expand,new LinearLayout.LayoutParams(dp(32),dp(32)));cell.addView(line);
                line.setContentDescription(contact.name+" numaralarını "+(open?"gizle":"göster"));line.setFocusable(true);line.setOnClickListener(v->{if(open)expanded.remove(contact.id);else expanded.add(contact.id);notifyDataSetChanged();});
                if(open)for(ContactBook.Phone phone:contact.phones){boolean added=already.contains(phone.number);LinearLayout numberRow=row();numberRow.setPadding(dp(14),dp(8),dp(12),dp(8));numberRow.setMinimumHeight(dp(66));numberRow.setBackground(bg(Color.WHITE,16));
                    CheckBox check=new CheckBox(MainActivity.this);check.setButtonTintList(android.content.res.ColorStateList.valueOf(accent));check.setChecked(added||selected.containsKey(phone.number));check.setEnabled(!added);check.setFocusable(false);check.setContentDescription(contact.name+" "+phone.number+" numarasını seç");numberRow.addView(check,new LinearLayout.LayoutParams(dp(42),dp(48)));
                    LinearLayout phoneLabels=column();TextView phoneText=text(phone.number,15,INK,true);phoneText.setTextDirection(View.TEXT_DIRECTION_LTR);phoneLabels.addView(phoneText);gap(phoneLabels,3);phoneLabels.addView(text(phone.label+(added?" · Bu listede":""),12,MUTED,false));numberRow.addView(phoneLabels,new LinearLayout.LayoutParams(0,-2,1));
                    Runnable toggle=()->{if(added)return;if(selected.containsKey(phone.number))selected.remove(phone.number);else selected.put(phone.number,new Rules.Contact(0,contact.name,phone.number,destinationWhite));updateSelection.run();notifyDataSetChanged();};check.setOnClickListener(v->toggle.run());numberRow.setOnClickListener(v->toggle.run());LinearLayout.LayoutParams np=new LinearLayout.LayoutParams(-1,-2);np.leftMargin=dp(48);np.bottomMargin=dp(6);cell.addView(numberRow,np);
                }
                return cell;
            }
        };
        list.setAdapter(adapter);
        save.setOnClickListener(v->{if(selected.isEmpty())return;try{store.addAll(new ArrayList<>(selected.values()),destinationWhite);int size=selected.size();dialog.dismiss();render();toast(size+" numara "+(destinationWhite?"beyaz":"kara")+" listeye eklendi.");}catch(IllegalArgumentException|android.database.SQLException ex){toast("Kişiler eklenemedi. Seçimlerin korunuyor; yeniden dene.");}});
        filter.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence value,int start,int count,int after){}public void afterTextChanged(Editable e){}public void onTextChanged(CharSequence value,int start,int before,int countBefore){String query=value.toString();filtered.clear();for(ContactBook.Group group:groups)if(ContactBook.matches(group,query))filtered.add(group);clear.setVisibility(query.isEmpty()?View.GONE:View.VISIBLE);count.setText(filtered.size()+" kişi");adapter.notifyDataSetChanged();list.setSelection(0);}});
        panel.setOnApplyWindowInsetsListener((view,insets)->{view.setPadding(dp(20)+insets.getSystemWindowInsetLeft(),dp(12)+insets.getSystemWindowInsetTop(),dp(20)+insets.getSystemWindowInsetRight(),dp(12)+insets.getSystemWindowInsetBottom());return insets.consumeSystemWindowInsets();});
        dialog.setContentView(panel);Window window=dialog.getWindow();if(window!=null){window.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(BG));window.setStatusBarColor(BG);window.setNavigationBarColor(BG);window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE|WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);}
        panel.setFocusableInTouchMode(true);panel.requestFocus();dialog.show();if(window!=null)window.setLayout(-1,-1);panel.requestApplyInsets();
    }
    private void history(){heading("Arama geçmişi","Yalnızca bu cihazda saklanır.");boolean any=false;String previous="";SimpleDateFormat date=new SimpleDateFormat("d MMMM yyyy",new Locale("tr"));SimpleDateFormat time=new SimpleDateFormat("HH:mm",new Locale("tr"));String today=date.format(new Date());try(Cursor c=store.history()){while(c.moveToNext()){any=true;String caller=c.getString(1);boolean video=c.getInt(2)==1;String result=c.getString(3);long at=c.getLong(4);String callNumber=c.getString(5);String day=date.format(new Date(at));if(!day.equals(previous)){gap(body,8);body.addView(text(day.equals(today)?"Bugün":day,16,MUTED,true));gap(body,12);previous=day;}LinearLayout card=card();LinearLayout r=row();Icon i=icon(video?"video":"phone",video?Color.rgb(159,55,65):GREEN,42);r.addView(i);LinearLayout lines=column();TextView person=text(caller,16,INK,true);person.setMaxLines(2);lines.addView(person);gap(lines,5);lines.addView(text((video?"Görüntülü arama":"Sesli arama")+" · "+time.format(new Date(at)),12,MUTED,false));gap(lines,4);lines.addView(text(result,12,MUTED,false));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,1);p.leftMargin=dp(15);r.addView(lines,p);card.addView(r);card.setContentDescription(caller+" arama kaydı");card.setOnClickListener(v->historyAction(caller,callNumber,result));}}
        if(!any){LinearLayout empty=card();empty.addView(text("Henüz arama kaydı yok.",17,INK,true));gap(empty,7);empty.addView(text("Seçili kurallara uyan yeni çağrılar burada görünecek.",14,MUTED,false));}
        gap(body,8);TextView clear=button("Geçmişi temizle",false,()->new AlertDialog.Builder(this).setTitle("Geçmiş temizlensin mi?").setMessage("Bu cihazdaki tüm arama kayıtları silinecek.").setNegativeButton("Vazgeç",null).setPositiveButton("Temizle",(d,w)->{store.clearHistory();render();}).show());clear.setEnabled(any);clear.setAlpha(any?1:0.45f);body.addView(clear);
    }
    private void historyAction(String caller,String number,String result){boolean destinationWhite=!"İzin verildi".equals(result);String label=destinationWhite?"Beyaz listeye ekle":"Kara listeye ekle";new AlertDialog.Builder(this).setTitle(caller).setItems(new String[]{label},(d,w)->{if(number.isEmpty())manual(caller.equals("Bilinmeyen numara")?"":caller,"",destinationWhite);else new AlertDialog.Builder(this).setTitle(label+"?").setMessage(caller+"\n"+number).setNegativeButton("Vazgeç",null).setPositiveButton("Ekle",(dialog,which)->{store.add(caller,number,destinationWhite);toast("Kişi "+(destinationWhite?"beyaz":"kara")+" listeye eklendi.");}).show();}).setNegativeButton("Kapat",null).show();}
    private void settings(){heading("Her şey kontrolünde","");LinearLayout permission=card();LinearLayout r=row();r.addView(text("Bildirim erişimi",17,INK,true),new LinearLayout.LayoutParams(0,-2,1));TextView badge=text(access()?"● Etkin":"○ Kapalı",12,access()?GREEN:MUTED,true);badge.setPadding(dp(9),dp(5),dp(9),dp(5));badge.setBackground(bg(Color.rgb(222,243,224),20));r.addView(badge);permission.addView(r);gap(permission,7);permission.addView(text("Uygulamanın WhatsApp aramalarını yönetebilmesi için gereklidir.",13,MUTED,false));gap(permission,10);permission.addView(button("İzni yönet",false,this::permissionExplanation));
        LinearLayout type=card();type.addView(text("Arama türü",17,INK,true));gap(type,12);segmented(type,new String[]{"Sesli","Görüntülü","Her ikisi"},store.media().ordinal(),i->store.setting("media",Rules.Media.values()[i].name()));gap(type,8);type.addView(text("Kurallar seçtiğin arama türüne uygulanır.",12,MUTED,false));
        LinearLayout action=card();action.addView(text("İşlem modu",17,INK,true));gap(action,8);radio(action,"Otomatik reddet",!store.recordOnly(),()->store.setting("recordOnly",false));radio(action,"Yalnızca kaydet",store.recordOnly(),()->store.setting("recordOnly",true));gap(action,8);action.addView(text("Yalnızca kaydet modu çağrıyı susturmaz.",12,MUTED,false));
        LinearLayout about=card();about.addView(text("Program hakkında",17,INK,true));gap(about,16);TextView sig=text("Call Guard by AC",25,GREEN,true);sig.setGravity(Gravity.CENTER);about.addView(sig);gap(about,4);TextView version=text("Sürüm "+BuildConfig.VERSION_NAME,12,MUTED,false);version.setGravity(Gravity.CENTER);about.addView(version);gap(about,12);TextView disclaimer=text("WhatsApp veya Meta ile bağlantılı değildir.",11,MUTED,false);disclaimer.setGravity(Gravity.CENTER);about.addView(disclaimer);
    }
    private void radio(LinearLayout parent,String label,boolean checked,Runnable change){LinearLayout r=row();RadioButton b=new RadioButton(this);b.setChecked(checked);b.setButtonTintList(android.content.res.ColorStateList.valueOf(checked?GREEN:MUTED));b.setClickable(false);b.setFocusable(false);r.addView(b,new LinearLayout.LayoutParams(dp(39),dp(40)));r.addView(text(label,14,INK,false));r.setContentDescription(label);r.setFocusable(true);r.setOnClickListener(v->change.run());parent.addView(r);}
    private void permissionExplanation(){new AlertDialog.Builder(this).setTitle("Bildirim erişimine izin ver").setMessage("CallGuard, WhatsApp sesli ve görüntülü çağrı bildirimlerindeki arayan bilgisini ve Reddet düğmesini kullanır. Mesaj bildirimlerini işlemez. Seçtiğin kişiler ve çağrı geçmişi yalnızca cihazında saklanır. Android ayarlarında CallGuard erişimini istediğin zaman kapatabilirsin.").setNegativeButton("Vazgeç",null).setPositiveButton("Ayarlara git",(d,w)->openAccess()).show();}
    private void openAccess(){try{if(Build.VERSION.SDK_INT>=30)startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS).putExtra(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,new ComponentName(this,CallNotificationService.class).flattenToString()));else startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));NotificationListenerService.requestRebind(new ComponentName(this,CallNotificationService.class));}catch(ActivityNotFoundException e){startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));}}
    private void about(){new AlertDialog.Builder(this).setTitle("Program hakkında").setMessage("Call Guard by AC\nSürüm "+BuildConfig.VERSION_NAME+"\n\nWhatsApp veya Meta ile bağlantılı değildir.").setPositiveButton("Tamam",null).show();}
    private void toast(String value){Toast.makeText(this,value,Toast.LENGTH_LONG).show();}
    private final class Icon extends View {
        final String kind; final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG); final int color;
        Icon(String kind,int color){super(MainActivity.this);this.kind=kind;this.color=color;setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);}
        @Override protected void onDraw(Canvas c){super.onDraw(c);c.save();c.scale(getWidth()/32f,getHeight()/32f);p.setColor(color);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2.2f);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);
            if(kind.equals("shield")){Path s=new Path();s.moveTo(16,3);s.lineTo(28,8);s.lineTo(26,20);s.quadTo(23,27,16,30);s.quadTo(9,27,6,20);s.lineTo(4,8);s.close();c.drawPath(s,p);c.drawLine(10,16,14,20,p);c.drawLine(14,20,22,12,p);}
            else if(kind.equals("search")){c.drawCircle(13,13,8,p);c.drawLine(19,19,28,28,p);}
            else if(kind.equals("clock")){c.drawCircle(16,16,12,p);c.drawLine(16,8,16,16,p);c.drawLine(16,16,22,19,p);}
            else if(kind.equals("people")){c.drawCircle(12,9,4,p);c.drawCircle(23,10,3,p);c.drawArc(4,17,20,31,180,180,false,p);c.drawArc(19,18,29,29,220,140,false,p);c.drawLine(4,24,4,28,p);c.drawLine(4,28,20,28,p);c.drawLine(20,24,20,28,p);}
            else if(kind.equals("video")){c.drawRoundRect(3,8,22,25,3,3,p);Path s=new Path();s.moveTo(22,13);s.lineTo(29,9);s.lineTo(29,24);s.lineTo(22,20);c.drawPath(s,p);}
            else if(kind.equals("phone")){Path s=new Path();s.moveTo(8,4);s.lineTo(13,10);s.lineTo(10,14);s.quadTo(14,21,20,22);s.lineTo(23,18);s.lineTo(29,23);s.quadTo(29,29,24,29);s.quadTo(8,25,3,10);s.quadTo(2,4,8,4);c.drawPath(s,p);}
            else {c.drawCircle(16,16,5,p);c.drawCircle(16,16,10,p);for(int i=0;i<8;i++){double a=i*Math.PI/4;c.drawLine(16+(float)Math.cos(a)*10,16+(float)Math.sin(a)*10,16+(float)Math.cos(a)*14,16+(float)Math.sin(a)*14,p);}}
            c.restore();
        }
    }
}
