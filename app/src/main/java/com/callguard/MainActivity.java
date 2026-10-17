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
import android.text.*;
import android.view.*;
import android.widget.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;

/** Native, responsive Turkish UI. No remote content or sample records. */
public final class MainActivity extends Activity {
    private static final int GREEN=Color.rgb(8,99,78), INK=Color.rgb(13,22,38), MUTED=Color.rgb(99,110,125), BG=Color.rgb(245,247,244), BORDER=Color.rgb(222,228,225);
    private final Handler statusHandler=new Handler(Looper.getMainLooper());
    private String displayedProtection="";
    private final Runnable statusTick=new Runnable(){public void run(){if(tab==0&&!isFinishing()&&!displayedProtection.equals(protectionSnapshot()))render();statusHandler.postDelayed(this,1000);}};
    private String driveAction="",driveAccount="";
    private ProBilling billing;
    private CarSilence carSilence;
    private AdbGuide adbGuide;
    private Store store; private LinearLayout root,body,nav; private ScrollView scroll;
    private final int[] tabScrollY=new int[5];
    private boolean backupPage=false;
    private boolean historyTodayOnly=false;
    private Object backupBackCallback;
    private int tab=0; private boolean white=true; private boolean rendering=false;
    private String search=""; private LinearLayout peopleRows;
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final BroadcastReceiver refresh=new BroadcastReceiver(){@Override public void onReceive(Context c,Intent i){if(!rendering)render();}};
    @Override public void onCreate(Bundle state){
        super.onCreate(state);store=new Store(this);carSilence=new CarSilence(this,()->{if(!isFinishing()&&!isDestroyed()){render();if(adbGuide!=null)adbGuide.refresh();}},this::toast);
        if(state!=null){tab=state.getInt("tab");historyTodayOnly=state.getBoolean("historyTodayOnly",false);white=state.getBoolean("white",true);search=state.getString("search","");backupPage=state.getBoolean("backupPage",false);driveAction=state.getString("driveAction","");driveAccount=state.getString("driveAccount","");int[] savedScroll=state.getIntArray("tabScrollY");if(savedScroll!=null)System.arraycopy(savedScroll,0,tabScrollY,0,Math.min(savedScroll.length,tabScrollY.length));}
        if(Build.VERSION.SDK_INT>=33)registerReceiver(refresh,new IntentFilter(getPackageName()+".CHANGED"),Context.RECEIVER_NOT_EXPORTED);
        else registerReceiver(refresh,new IntentFilter(getPackageName()+".CHANGED"));
        getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        render();if(state!=null&&state.getBoolean("adbGuideOpen",false))root.post(this::carSilenceDetails);
    }
    @Override protected void onResume(){super.onResume();BackgroundProtection.reconnect(this);if(store!=null)render();if(adbGuide!=null)adbGuide.resume();CloudSync.periodic(this);CloudSync.enqueue(this);statusHandler.removeCallbacks(statusTick);statusHandler.postDelayed(statusTick,1000);}
    @Override protected void onPause(){if(adbGuide!=null)adbGuide.pause();statusHandler.removeCallbacks(statusTick);super.onPause();}
    @Override protected void onSaveInstanceState(Bundle out){out.putBoolean("historyTodayOnly",historyTodayOnly);out.putBoolean("adbGuideOpen",adbGuide!=null&&adbGuide.showing());out.putBoolean("backupPage",backupPage);out.putString("driveAction",driveAction);out.putString("driveAccount",driveAccount);out.putIntArray("tabScrollY",tabScrollY.clone());out.putInt("tab",tab);out.putBoolean("white",white);out.putString("search",search);super.onSaveInstanceState(out);}
    @Override protected void onDestroy(){if(Build.VERSION.SDK_INT>=33&&backupBackCallback!=null)getOnBackInvokedDispatcher().unregisterOnBackInvokedCallback((android.window.OnBackInvokedCallback)backupBackCallback);unregisterReceiver(refresh);if(adbGuide!=null)adbGuide.close();if(carSilence!=null)carSilence.close();if(billing!=null)billing.close();worker.shutdown();store.close();super.onDestroy();}
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
    private boolean access(){return BackgroundProtection.hasAccess(this);}
    private void render(){
        if(isFinishing())return;rendering=true;
        root=column();root.setBackgroundColor(BG);
        root.setOnApplyWindowInsetsListener((view,insets)->{view.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());return insets.consumeSystemWindowInsets();});
        setContentView(root);root.requestApplyInsets();
        final int renderedTab=tab==3&&backupPage?4:tab;
        final int restoreY=tabScrollY[renderedTab];
        final boolean[] restoring={true};
        scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setClipToPadding(false);
        final ScrollView renderedScroll=scroll;
        renderedScroll.setOnScrollChangeListener((view,x,y,oldX,oldY)->{if(!restoring[0]&&scroll==renderedScroll)tabScrollY[renderedTab]=y;});
        body=column();body.setPadding(dp(22),dp(20),dp(22),dp(18));scroll.addView(body,new ScrollView.LayoutParams(-1,-2));root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        if(tab==0)home();else if(tab==1)people();else if(tab==2)history();else if(backupPage)backupSettings();else settings();
        nav=row();nav.setPadding(dp(8),dp(9),dp(8),dp(10));nav.setBackgroundColor(Color.WHITE);
        String[] names={"Koruma","Kişiler","Geçmiş","Ayarlar"};String[] icons={"shield","people","clock","settings"};
        for(int j=0;j<4;j++){final int index=j;LinearLayout item=column();item.setGravity(Gravity.CENTER);item.setPadding(dp(2),dp(2),dp(2),dp(2));item.addView(icon(icons[j],j==tab?GREEN:MUTED,23));gap(item,5);TextView label=text(names[j],12,j==tab?GREEN:MUTED,j==tab);label.setGravity(Gravity.CENTER);item.addView(label,new LinearLayout.LayoutParams(-1,-2));item.setContentDescription(names[j]+" sekmesi");item.setFocusable(true);item.setOnClickListener(v->{backupPage=false;if(index==2&&historyTodayOnly){historyTodayOnly=false;tabScrollY[2]=0;}tab=index;render();});nav.addView(item,new LinearLayout.LayoutParams(0,dp(56),1));}
        root.addView(nav,new LinearLayout.LayoutParams(-1,-2));
        // Restore only this render's view, after measuring its new content. Repeated cloud
        // broadcasts must not replace the saved position with a fresh view's initial zero.
        body.setFocusableInTouchMode(true);body.requestFocus();
        renderedScroll.post(()->{if(scroll!=renderedScroll)return;renderedScroll.scrollTo(0,restoreY);restoring[0]=false;tabScrollY[renderedTab]=renderedScroll.getScrollY();});
        if(tab==0)displayedProtection=protectionSnapshot();syncBackupBack();rendering=false;
    }
    private void home(){
        LinearLayout brand=row();brand.addView(icon("shield",GREEN,31));TextView word=text("CallGuard",22,INK,true);LinearLayout.LayoutParams wp=new LinearLayout.LayoutParams(0,-2,1);wp.leftMargin=dp(9);brand.addView(word,wp);
        TextView menu=text("⋮",26,MUTED,true);menu.setGravity(Gravity.CENTER);menu.setContentDescription("Diğer seçenekler");menu.setOnClickListener(v->{PopupMenu p=new PopupMenu(this,menu);p.getMenu().add("Ayarlar").setOnMenuItemClickListener(i->{backupPage=false;tab=3;render();return true;});p.getMenu().add("Program hakkında").setOnMenuItemClickListener(i->{about();return true;});p.show();});brand.addView(menu,new LinearLayout.LayoutParams(dp(48),dp(48)));body.addView(brand);gap(body,20);
        heading("Huzurla devam et.","WhatsApp aramalarını yönet.");
        boolean protecting=store.enabled()&&access()&&LicenseManager.allowed(this)&&!store.paused()&&store.scheduleActive()&&!store.recordOnly()&&CallNotificationService.connected;
        LinearLayout protect=card();GradientDrawable grad=new GradientDrawable(GradientDrawable.Orientation.TL_BR,protecting?new int[]{GREEN,Color.rgb(2,80,65)}:new int[]{Color.rgb(180,35,24),Color.rgb(127,29,29)});grad.setCornerRadius(dp(18));protect.setBackground(grad);
        LinearLayout pr=row();pr.addView(icon(protecting?"shield":"warning",Color.WHITE,60));LinearLayout lines=column();TextView status=text(!store.enabled()?"Koruma kapalı":ProtectionState.title(store.enabled(),access(),LicenseManager.allowed(this),store.paused(),store.scheduleActive(),store.recordOnly(),CallNotificationService.connected),18,Color.WHITE,true);lines.addView(status);gap(lines,5);lines.addView(text(protectionDetail(),12,protecting?Color.rgb(209,238,220):Color.rgb(255,226,222),false));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1);lp.leftMargin=dp(12);pr.addView(lines,lp);
        Switch toggle=new Switch(this);toggle.setContentDescription("Koruma anahtarı");toggle.setChecked(store.enabled());toggle.setThumbTintList(android.content.res.ColorStateList.valueOf(Color.WHITE));toggle.setTrackTintList(android.content.res.ColorStateList.valueOf(protecting?Color.rgb(139,210,153):Color.rgb(245,158,150)));toggle.setOnCheckedChangeListener((b,on)->{if(on&&!access()){toggle.setChecked(false);permissionExplanation();return;}if(on&&BuildConfig.COMMERCIAL_ENABLED&&!LicenseManager.allowed(this)){toggle.setChecked(false);beginTrial();return;}store.setting("enabled",on);});pr.addView(toggle);protect.addView(pr);
        if(!access()){TextView permission=button("Bildirim erişimini aç",false,this::permissionExplanation);permission.setTextColor(Color.WHITE);gap(protect,10);protect.addView(permission);}
        if(store.enabled()){gap(protect,10);TextView pause=button(store.paused()?"Şimdi devam et":"Korumayı duraklat",false,()->{if(store.paused()){store.pauseUntil(0);render();}else pauseChoices();});pause.setTextColor(Color.WHITE);protect.addView(pause);}
        LinearLayout accessCard=card();accessCard.addView(text(LicenseManager.status(this),14,GREEN,true));gap(accessCard,6);accessCard.addView(text(store.scheduleActive()?"Seçili profil: "+profileName(store.profile()):"Zamanlama dışında · otomatik koruma bekliyor",12,MUTED,false));
        LinearLayout rules=card();rules.addView(text("Engelleme kuralı",16,INK,true));gap(rules,8);
        rule(rules,"Tüm gelen aramaları reddet","Seçilen arama türünde herkesi reddet.",Rules.Mode.ALL);
        rule(rules,"Beyaz liste","Yalnızca izin verdiklerin ulaşabilsin.",Rules.Mode.WHITELIST);
        rule(rules,"Kara liste","Yalnızca listedeki kişileri reddet.",Rules.Mode.BLACKLIST);
        LinearLayout stats=row();List<Rules.Contact> contacts=store.contacts();int count=0;boolean showWhite=store.effectiveMode()!=Rules.Mode.BLACKLIST;for(Rules.Contact c:contacts)if(c.white==showWhite)count++;
        LinearLayout peopleStat=stat("people",String.valueOf(count),showWhite?"İzin verilen kişi":"Kara listedeki kişi");peopleStat.setFocusable(true);peopleStat.setContentDescription((showWhite?"Beyaz listeyi aç":"Kara listeyi aç")+", "+count+" kişi");peopleStat.setOnClickListener(v->{white=showWhite;search="";tabScrollY[1]=0;tab=1;render();});stats.addView(peopleStat,new LinearLayout.LayoutParams(0,-2,1));
        View spacer=new View(this);stats.addView(spacer,new LinearLayout.LayoutParams(dp(12),1));
        LinearLayout rejectedStat=stat("phone",String.valueOf(store.today()),"Bugünkü reddetme isteği");rejectedStat.setFocusable(true);rejectedStat.setContentDescription("Bugünkü reddetme isteklerini aç");rejectedStat.setOnClickListener(v->{historyTodayOnly=true;tabScrollY[2]=0;tab=2;render();});stats.addView(rejectedStat,new LinearLayout.LayoutParams(0,-2,1));body.addView(stats);gap(body,13);
        body.addView(button("Kişileri düzenle    ›",true,()->{white=store.effectiveMode()!=Rules.Mode.BLACKLIST;tab=1;render();}));gap(body,10);
        TextView notice=text("ⓘ  Bağlı cihazlarda kısa süreli zil duyulabilir.",12,GREEN,false);notice.setPadding(dp(12),dp(11),dp(12),dp(11));notice.setBackground(bg(Color.rgb(230,242,231),12));body.addView(notice);
    }
    private String protectionSnapshot(){return ProtectionState.title(store.enabled(),access(),LicenseManager.allowed(this),store.paused(),store.scheduleActive(),store.recordOnly(),CallNotificationService.connected)+":"+store.pauseUntil();}
    private String protectionDetail(){
        if(!store.enabled())return "Otomatik reddetme kapalı. Başlamak için koruma anahtarını aç.";
        if(!access())return "Aramaları yönetmek için bildirim erişimini aç.";
        if(!LicenseManager.allowed(this))return LicenseManager.status(this);
        if(store.paused())return "Devam: "+new SimpleDateFormat("d MMM HH:mm",new Locale("tr")).format(new Date(store.pauseUntil()));
        if(!store.scheduleActive())return "Seçili profilin gün ve saatleri dışında.";
        if(!CallNotificationService.connected)return "İzin açık; servis henüz bağlanmadı. Ayarlardaki arka plan kontrolünü aç.";
        if(store.recordOnly())return "Aramalar kaydedilir; otomatik reddedilmez.";
        return "Bildirim erişimi ve otomatik reddetme etkin.";
    }
    private void pauseChoices(){
        new AlertDialog.Builder(this).setTitle("Korumayı duraklat").setItems(new String[]{"15 dakika","1 saat","Bugünün sonuna kadar"},(dialog,index)->{
            long until=index==0?System.currentTimeMillis()+15*60000L:index==1?System.currentTimeMillis()+60*60000L:java.time.ZonedDateTime.now().toLocalDate().plusDays(1).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
            store.pauseUntil(until);render();toast("Süre dolunca koruma, seçili profil ve kullanım hakkına göre devam eder.");
        }).setNegativeButton("Vazgeç",null).show();
    }
    private LinearLayout stat(String type,String value,String label){LinearLayout c=column();c.setPadding(dp(14),dp(13),dp(14),dp(13));c.setBackground(bg(Color.WHITE,16));LinearLayout r=row();r.addView(icon(type,GREEN,24));TextView val=text(value,27,INK,true);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-2,-2);p.leftMargin=dp(9);r.addView(val,p);c.addView(r);gap(c,5);TextView desc=text(label,12,MUTED,false);desc.setMinHeight(dp(34));c.addView(desc);return c;}
    private void rule(LinearLayout parent,String title,String subtitle,Rules.Mode mode){
        LinearLayout r=row();r.setPadding(0,dp(8),0,dp(8));RadioButton radio=new RadioButton(this);radio.setButtonTintList(android.content.res.ColorStateList.valueOf(store.effectiveMode()==mode?GREEN:MUTED));radio.setChecked(store.effectiveMode()==mode);radio.setClickable(false);radio.setFocusable(false);r.addView(radio,new LinearLayout.LayoutParams(dp(38),dp(42)));LinearLayout labels=column();labels.addView(text(title,14,INK,true));gap(labels,2);labels.addView(text(subtitle,12,MUTED,false));r.addView(labels,new LinearLayout.LayoutParams(0,-2,1));r.setContentDescription(title);r.setFocusable(true);View.OnClickListener select=v->{store.setting("rule",mode.name());render();};r.setOnClickListener(select);radio.setClickable(true);radio.setOnClickListener(select);parent.addView(r);
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
    private void requestContacts(){if(checkSelfPermission(Manifest.permission.READ_CONTACTS)==PackageManager.PERMISSION_GRANTED)pickContacts();else new AlertDialog.Builder(this).setTitle("Rehberden kişi seç").setMessage("Kişi seçebilmek için rehber erişimi gerekir. Seçtiğin ad ve numara cihazda saklanır; istersen CallGuard hesabına yedeklenir. Rehberin tamamı yüklenmez.").setNegativeButton("Vazgeç",null).setPositiveButton("Devam",(d,w)->requestPermissions(new String[]{Manifest.permission.READ_CONTACTS},40)).show();}
    @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] results){super.onRequestPermissionsResult(request,permissions,results);if(request==43){if(results.length>0&&results[0]==PackageManager.PERMISSION_GRANTED)chooseSilentCar();else new AlertDialog.Builder(this).setTitle("Bluetooth izni verilmedi").setMessage("Araç listesini açmak için Yakındaki cihazlar iznini uygulama ayarlarından verebilirsin.").setNegativeButton("Kapat",null).setPositiveButton("Uygulama ayarları",(d,w)->startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,android.net.Uri.parse("package:"+getPackageName())))).show();return;}if(request==40){if(results.length>0&&results[0]==PackageManager.PERMISSION_GRANTED)pickContacts();else new AlertDialog.Builder(this).setTitle("Rehber izni verilmedi").setMessage("Elle numara ekleyebilir veya uygulama ayarlarından rehber iznini açabilirsin.").setNegativeButton("Kapat",null).setPositiveButton("Uygulama ayarları",(d,w)->startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,android.net.Uri.parse("package:"+getPackageName())))).show();}}
    private void pickContacts(){ProgressDialog loading=ProgressDialog.show(this,"Rehber","Kişiler yükleniyor…",true,false);worker.execute(()->{ArrayList<String[]> values=new ArrayList<>();String failure=null;
        try(Cursor c=getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,new String[]{ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,ContactsContract.CommonDataKinds.Phone.NUMBER,ContactsContract.CommonDataKinds.Phone.CONTACT_ID,ContactsContract.CommonDataKinds.Phone.TYPE,ContactsContract.CommonDataKinds.Phone.LABEL},null,null,ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME+" COLLATE LOCALIZED ASC")){
            if(c!=null)while(c.moveToNext()){String number=Rules.number(c.getString(1));String name=c.getString(0);if(!number.isEmpty())values.add(new String[]{name==null||name.trim().isEmpty()?number:name,number,c.getString(2),ContactsContract.CommonDataKinds.Phone.getTypeLabel(getResources(),c.getInt(3),c.getString(4)).toString()});}
        }catch(SecurityException e){failure="Rehber izni kaldırılmış. İzni yeniden aç.";}
        final String error=failure;final List<ContactBook.Group> groups=error==null?ContactBook.group(values):Collections.emptyList();runOnUiThread(()->{if(isFinishing()||isDestroyed())return;loading.dismiss();if(error!=null){toast(error);return;}showContactPicker(groups);});});}
    private TextView contactAvatar(String name){TextView avatar=text(initials(name),18,GREEN,true);avatar.setGravity(Gravity.CENTER);applyAvatarColor(avatar,name);avatar.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);return avatar;}
    private void applyAvatarColor(TextView avatar,String name){int[] colors={0xffdce9ff,0xfff9e0d9,0xffe8def8,0xffd6eee5,0xfff9eabf,0xffd7edf3};int[] inks={0xff315c9a,0xff984f40,0xff725195,0xff276653,0xff826219,0xff356b7b};int index=Math.floorMod(name.hashCode(),colors.length);avatar.setTextColor(inks[index]);avatar.setBackground(bg(colors[index],48));}
    private final class PickerRow {
        final LinearLayout cell=column(),line=row(),numbers=column();final TextView section,avatar,name,subtitle,expand;
        PickerRow(int accent){section=text("",13,accent,true);section.setPadding(dp(12),dp(10),0,dp(4));cell.addView(section);line.setPadding(dp(8),dp(8),dp(8),dp(8));line.setMinimumHeight(dp(68));avatar=contactAvatar("");line.addView(avatar,new LinearLayout.LayoutParams(dp(44),dp(44)));LinearLayout labels=column();name=text("",16,INK,true);name.setMaxLines(2);labels.addView(name);gap(labels,4);subtitle=text("",12,MUTED,false);labels.addView(subtitle);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1);lp.leftMargin=dp(12);lp.rightMargin=dp(8);line.addView(labels,lp);expand=text("",24,accent,false);expand.setGravity(Gravity.CENTER);expand.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);line.addView(expand,new LinearLayout.LayoutParams(dp(32),dp(32)));cell.addView(line);cell.addView(numbers);cell.setTag(this);}
    }
    private String contactLetter(String name){String clean=name.trim();return clean.isEmpty()?"#":clean.substring(0,clean.offsetByCodePoints(0,1)).toUpperCase(new Locale("tr"));}
    private void showContactPicker(List<ContactBook.Group> groups){
        final boolean destinationWhite=white;
        final int accent=destinationWhite?GREEN:0xff9c414c;
        final int tint=destinationWhite?0xffe0eee7:0xfff8e8e9;
        Set<String> already=new HashSet<>();for(Rules.Contact c:store.contacts())if(c.white==destinationWhite)already.add(c.number);
        Dialog dialog=new Dialog(this,android.R.style.Theme_Material_Light_NoActionBar);
        LinearLayout panel=column();panel.setBackgroundColor(BG);panel.setPadding(dp(20),dp(12),dp(20),dp(12));
        LinearLayout toolbar=row();TextView back=text("‹",34,INK,false);back.setGravity(Gravity.CENTER);back.setContentDescription("Rehberi kapat");back.setOnClickListener(v->dialog.dismiss());toolbar.addView(back,new LinearLayout.LayoutParams(dp(48),dp(48)));
        TextView heading=text("Rehberden seç",23,INK,true);LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(0,-2,1);hp.leftMargin=dp(8);toolbar.addView(heading,hp);panel.addView(toolbar);gap(panel,6);
        TextView badge=text(destinationWhite?"Beyaz listeye ekle":"Kara listeye ekle",13,accent,true);badge.setPadding(dp(14),dp(8),dp(14),dp(8));badge.setBackground(bg(tint,20));LinearLayout details=column();details.setPadding(dp(4),dp(10),dp(4),dp(8));details.addView(badge,new LinearLayout.LayoutParams(-2,-2));gap(details,8);details.addView(text("Kişiye dokun, numaralarını seç. Birden fazla kişi ekleyebilirsin.",13,MUTED,false));gap(details,8);details.addView(text("WhatsApp yalnızca ad bildirirse aynı kişinin numaraları ayırt edilemeyebilir.",11,MUTED,false));
        LinearLayout searchBox=row();searchBox.setPadding(dp(16),0,dp(6),0);searchBox.setBackground(bg(0xffe9eeea,28));searchBox.addView(icon("search",MUTED,22));
        EditText filter=input("Rehberde ara");filter.setBackgroundColor(Color.TRANSPARENT);filter.setTextSize(16);filter.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH);searchBox.addView(filter,new LinearLayout.LayoutParams(0,dp(50),1));
        TextView clear=text("×",24,MUTED,false);clear.setGravity(Gravity.CENTER);clear.setContentDescription("Rehber aramasını temizle");clear.setVisibility(View.GONE);clear.setOnClickListener(v->filter.setText(""));searchBox.addView(clear,new LinearLayout.LayoutParams(dp(44),dp(48)));panel.addView(searchBox);gap(panel,6);
        TextView count=text(groups.size()+" kişi",12,MUTED,true);count.setPadding(dp(10),0,0,dp(4));panel.addView(count);
        LinearLayout content=column();panel.addView(content,new LinearLayout.LayoutParams(-1,0,1));
        ListView list=new ListView(this);list.setDivider(null);list.setSelector(new android.graphics.drawable.ColorDrawable(0xffe2ebe5));list.setClipToPadding(false);list.setPadding(0,0,0,dp(16));list.setFastScrollEnabled(true);list.setContentDescription("Telefon rehberi");list.addHeaderView(details,null,false);content.addView(list,new LinearLayout.LayoutParams(-1,-1));
        TextView empty=text(groups.isEmpty()?"Rehberde geçerli numarası olan kişi bulunamadı.":"Aramana uygun kişi bulunamadı.",16,MUTED,false);empty.setGravity(Gravity.CENTER);empty.setPadding(dp(24),dp(40),dp(24),dp(40));content.addView(empty,new LinearLayout.LayoutParams(-1,-1));list.setEmptyView(empty);
        ArrayList<ContactBook.Group> filtered=new ArrayList<>(groups);
        Map<String,Rules.Contact> selected=new LinkedHashMap<>();Set<String> expanded=new HashSet<>();
        LinearLayout footer=column();footer.setPadding(0,dp(6),0,0);TextView selection=text("0 numara seçildi",13,MUTED,true);footer.addView(selection);gap(footer,8);
        TextView save=button("Seçilenleri ekle",true,()->{});save.setContentDescription("Seçilen numaraları ekle");footer.addView(save);panel.addView(footer);
        Runnable updateSelection=()->{selection.setText(selected.size()+" numara seçildi");save.setText(selected.isEmpty()?"Seçilenleri ekle":"Seçilenleri ekle ("+selected.size()+")");save.setEnabled(!selected.isEmpty());save.setAlpha(selected.isEmpty()?0.45f:1f);save.setBackground(bg(accent,16));};updateSelection.run();
        BaseAdapter adapter=new BaseAdapter(){
            @Override public int getCount(){return filtered.size();}
            @Override public Object getItem(int pos){return filtered.get(pos);}
            @Override public long getItemId(int pos){return pos;}
            @Override public View getView(int pos,View convert,ViewGroup parent){
                ContactBook.Group contact=filtered.get(pos);boolean open=expanded.contains(contact.id);int chosen=0;for(ContactBook.Phone phone:contact.phones)if(selected.containsKey(phone.number))chosen++;
                PickerRow holder;if(convert!=null&&convert.getTag() instanceof PickerRow){holder=(PickerRow)convert.getTag();}else holder=new PickerRow(accent);
                LinearLayout cell=holder.cell;String letter=contactLetter(contact.name);boolean section=pos==0||!letter.equals(contactLetter(filtered.get(pos-1).name));holder.section.setText(letter);holder.section.setVisibility(section?View.VISIBLE:View.GONE);
                holder.avatar.setText(initials(contact.name));applyAvatarColor(holder.avatar,contact.name);holder.name.setText(contact.name);holder.subtitle.setText(contact.phones.size()+" numara"+(chosen>0?" · "+chosen+" seçili":""));holder.line.setBackground(chosen>0?bg(tint,18):null);holder.expand.setText(open?"⌃":"⌄");
                holder.line.setContentDescription(contact.name+" numaralarını "+(open?"gizle":"göster"));holder.line.setOnClickListener(v->{if(open)expanded.remove(contact.id);else expanded.add(contact.id);notifyDataSetChanged();});holder.numbers.removeAllViews();
                if(open)for(ContactBook.Phone phone:contact.phones){boolean added=already.contains(phone.number);LinearLayout numberRow=row();numberRow.setPadding(dp(14),dp(8),dp(12),dp(8));numberRow.setMinimumHeight(dp(66));numberRow.setBackground(bg(Color.WHITE,16));
                    CheckBox check=new CheckBox(MainActivity.this);check.setButtonTintList(android.content.res.ColorStateList.valueOf(accent));check.setChecked(added||selected.containsKey(phone.number));check.setEnabled(!added);check.setFocusable(false);check.setContentDescription(contact.name+" "+phone.number+" numarasını seç");numberRow.addView(check,new LinearLayout.LayoutParams(dp(42),dp(48)));
                    LinearLayout phoneLabels=column();TextView phoneText=text(phone.number,15,INK,true);phoneText.setTextDirection(View.TEXT_DIRECTION_LTR);phoneLabels.addView(phoneText);gap(phoneLabels,3);phoneLabels.addView(text(phone.label+(added?" · Bu listede":""),12,MUTED,false));numberRow.addView(phoneLabels,new LinearLayout.LayoutParams(0,-2,1));
                    Runnable toggle=()->{if(added)return;if(selected.containsKey(phone.number))selected.remove(phone.number);else selected.put(phone.number,new Rules.Contact(0,contact.name,phone.number,destinationWhite));updateSelection.run();notifyDataSetChanged();};check.setOnClickListener(v->toggle.run());numberRow.setOnClickListener(v->toggle.run());LinearLayout.LayoutParams np=new LinearLayout.LayoutParams(-1,-2);np.leftMargin=dp(48);np.bottomMargin=dp(6);holder.numbers.addView(numberRow,np);
                }
                return cell;
            }
        };
        list.setAdapter(adapter);
        save.setOnClickListener(v->{if(selected.isEmpty())return;try{store.addAll(new ArrayList<>(selected.values()),destinationWhite);int size=selected.size();dialog.dismiss();render();toast(size+" numara "+(destinationWhite?"beyaz":"kara")+" listeye eklendi.");}catch(IllegalArgumentException|android.database.SQLException ex){toast("Kişiler eklenemedi. Seçimlerin korunuyor; yeniden dene.");}});
        Handler searchHandler=new Handler(Looper.getMainLooper());ExecutorService searchWorker=Executors.newSingleThreadExecutor();java.util.concurrent.atomic.AtomicInteger searchGeneration=new java.util.concurrent.atomic.AtomicInteger();
        filter.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence value,int start,int count,int after){}public void afterTextChanged(Editable e){}public void onTextChanged(CharSequence value,int start,int before,int countBefore){String query=value.toString();int generation=searchGeneration.incrementAndGet();clear.setVisibility(query.isEmpty()?View.GONE:View.VISIBLE);searchHandler.removeCallbacksAndMessages(null);searchHandler.postDelayed(()->searchWorker.execute(()->{List<ContactBook.Group> results=ContactBook.filter(groups,query);searchHandler.post(()->{if(!dialog.isShowing()||generation!=searchGeneration.get())return;count.setText(results.size()+" kişi");if(!filtered.equals(results)){filtered.clear();filtered.addAll(results);adapter.notifyDataSetChanged();list.setSelectionAfterHeaderView();}});}),100);}});
        filter.setOnEditorActionListener((view,action,event)->{if(action==android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH){android.view.inputmethod.InputMethodManager keyboard=(android.view.inputmethod.InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);keyboard.hideSoftInputFromWindow(filter.getWindowToken(),0);filter.clearFocus();return true;}return false;});
        dialog.setOnDismissListener(d->{searchGeneration.incrementAndGet();searchHandler.removeCallbacksAndMessages(null);searchWorker.shutdownNow();});
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(panel,(view,insets)->{androidx.core.graphics.Insets bars=insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars()|androidx.core.view.WindowInsetsCompat.Type.displayCutout());androidx.core.graphics.Insets ime=insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.ime());boolean typing=insets.isVisible(androidx.core.view.WindowInsetsCompat.Type.ime());view.setPadding(dp(16)+bars.left,dp(4)+bars.top,dp(16)+bars.right,dp(6)+Math.max(bars.bottom,ime.bottom));details.setVisibility(typing?View.GONE:View.VISIBLE);selection.setVisibility(typing?View.GONE:View.VISIBLE);heading.setText(typing?(destinationWhite?"Beyaz listeye ekle":"Kara listeye ekle"):"Rehberden seç");return insets;});
        dialog.setContentView(panel);Window window=dialog.getWindow();if(window!=null){androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window,false);window.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(BG));window.setStatusBarColor(BG);window.setNavigationBarColor(BG);window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE|WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);}
        panel.setFocusableInTouchMode(true);panel.requestFocus();dialog.show();if(window!=null)window.setLayout(-1,-1);panel.requestApplyInsets();
    }
    private void history(){heading(historyTodayOnly?"Bugünkü reddetme istekleri":"Arama geçmişi",historyTodayOnly?"Yalnızca bugün gönderilen reddetme istekleri.":getSharedPreferences("cloud",0).getBoolean("autoBackup",false)?"Bulut yedekleme açık · hesabına ait yedek":"Yalnızca bu cihazda saklanır.");if(historyTodayOnly){body.addView(button("Tüm arama geçmişini göster",false,()->{historyTodayOnly=false;tabScrollY[2]=0;render();}));gap(body,16);}boolean any=false;String previous="";SimpleDateFormat date=new SimpleDateFormat("d MMMM yyyy",new Locale("tr"));SimpleDateFormat time=new SimpleDateFormat("HH:mm",new Locale("tr"));String today=date.format(new Date());try(Cursor c=historyTodayOnly?store.todayRejected():store.history()){while(c.moveToNext()){any=true;String caller=c.getString(1);boolean video=c.getInt(2)==1;String result=c.getString(3);long at=c.getLong(4);String callNumber=c.getString(5);String day=date.format(new Date(at));if(!day.equals(previous)){gap(body,8);body.addView(text(day.equals(today)?"Bugün":day,16,MUTED,true));gap(body,12);previous=day;}LinearLayout card=card();LinearLayout r=row();Icon i=icon(video?"video":"phone",video?Color.rgb(159,55,65):GREEN,42);r.addView(i);LinearLayout lines=column();TextView person=text(caller,16,INK,true);person.setMaxLines(2);lines.addView(person);gap(lines,5);lines.addView(text((video?"Görüntülü arama":"Sesli arama")+" · "+time.format(new Date(at)),12,MUTED,false));gap(lines,4);lines.addView(text(result,12,MUTED,false));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,1);p.leftMargin=dp(15);r.addView(lines,p);card.addView(r);card.setContentDescription(caller+" arama kaydı");card.setOnClickListener(v->historyAction(caller,callNumber,result));}}
        if(!any){LinearLayout empty=card();empty.addView(text(historyTodayOnly?"Bugün reddetme isteği yok.":"Henüz arama kaydı yok.",17,INK,true));gap(empty,7);empty.addView(text("Seçili kurallara uyan yeni çağrılar burada görünecek.",14,MUTED,false));}
        gap(body,8);TextView clear=button(historyTodayOnly?"Tüm geçmişi temizle":"Geçmişi temizle",false,()->new AlertDialog.Builder(this).setTitle("Geçmiş temizlensin mi?").setMessage("Bu cihazdaki tüm arama kayıtları silinecek.").setNegativeButton("Vazgeç",null).setPositiveButton("Temizle",(d,w)->{store.clearHistory();render();}).show());clear.setEnabled(any);clear.setAlpha(any?1:0.45f);body.addView(clear);
    }
    private void historyAction(String caller,String number,String result){boolean destinationWhite=!ProtectionState.permitted(result);String label=destinationWhite?"Beyaz listeye ekle":"Kara listeye ekle";new AlertDialog.Builder(this).setTitle(caller).setItems(new String[]{label},(d,w)->{if(number.isEmpty())manual(caller.equals("Bilinmeyen numara")?"":caller,"",destinationWhite);else new AlertDialog.Builder(this).setTitle(label+"?").setMessage(caller+"\n"+number).setNegativeButton("Vazgeç",null).setPositiveButton("Ekle",(dialog,which)->{store.add(caller,number,destinationWhite);toast("Kişi "+(destinationWhite?"beyaz":"kara")+" listeye eklendi.");}).show();}).setNegativeButton("Kapat",null).show();}
    private void settings(){heading("Her şey kontrolünde","");
        LinearLayout type=card();type.addView(text("Arama türü",17,INK,true));gap(type,12);segmented(type,new String[]{"Sesli","Görüntülü","Her ikisi"},store.effectiveMedia().ordinal(),i->store.setting("media",Rules.Media.values()[i].name()));gap(type,8);type.addView(text("Kurallar seçtiğin arama türüne uygulanır.",12,MUTED,false));
        LinearLayout action=card();action.addView(text("İşlem modu",17,INK,true));gap(action,8);radio(action,"Otomatik reddet",!store.recordOnly(),()->store.setting("recordOnly",false));radio(action,"Yalnızca kaydet",store.recordOnly(),()->store.setting("recordOnly",true));gap(action,8);action.addView(text("Yalnızca kaydet modu çağrıyı susturmaz. Otomatik reddetmede, reddetme isteğinden sonra aynı çağrının bildirimi temizlenmeye çalışılır. Mesajlar ve ortak bildirim özetleri silinmez.",12,MUTED,false));
        LinearLayout permission=card();LinearLayout r=row();r.addView(text("Bildirim erişimi",17,INK,true),new LinearLayout.LayoutParams(0,-2,1));TextView badge=text(access()?"● Etkin":"○ Kapalı",12,access()?GREEN:MUTED,true);badge.setPadding(dp(9),dp(5),dp(9),dp(5));badge.setBackground(bg(Color.rgb(222,243,224),20));r.addView(badge);permission.addView(r);gap(permission,7);permission.addView(text("Uygulamanın WhatsApp aramalarını yönetebilmesi için gereklidir.",13,MUTED,false));gap(permission,10);permission.addView(button("İzni yönet",false,this::permissionExplanation));
        carSilenceCard();
        backgroundCard();
        scheduleCard();
        LinearLayout backup=card();backup.addView(button("Yedekleme ve geri yükleme    ›",true,()->{backupPage=true;render();}));gap(backup,7);backup.addView(text("Hesap · CallGuard bulutu · Google Drive",12,MUTED,false));
        diagnosticsCard();
        LinearLayout about=card();about.addView(text("Program hakkında",17,INK,true));gap(about,16);TextView sig=text("Call Guard by AC",25,GREEN,true);sig.setGravity(Gravity.CENTER);about.addView(sig);gap(about,4);TextView version=text("Sürüm "+BuildConfig.VERSION_NAME,12,MUTED,false);version.setGravity(Gravity.CENTER);about.addView(version);gap(about,12);TextView disclaimer=text("WhatsApp veya Meta ile bağlantılı değildir.",11,MUTED,false);disclaimer.setGravity(Gravity.CENTER);about.addView(disclaimer);
    }
    private void carSilenceCard(){
        LinearLayout card=card();card.addView(text("Araç sessizliği · CallGuard ADB",17,INK,true));gap(card,8);
        card.addView(text(carSilence.status(),13,MUTED,false));gap(card,8);
        card.addView(text("Seçtiğin aracın bütün Bluetooth görüşmelerini etkiler; beyaz liste istisnası yoktur. Telefonun kendisinde çağrı devam edebilir. WhatsApp koruma anahtarından bağımsızdır.",12,MUTED,false));gap(card,12);
        card.addView(button(carSilence.saved()?"Kapat ve önceki ayarı geri yükle":"Araç seç ve sessizliği aç",carSilence.saved(),()->{if(carSilence.saved())carSilence.restore();else chooseSilentCar();}));gap(card,8);
        card.addView(button("Kurulum ve durum",false,this::carSilenceDetails));
    }
    private void chooseSilentCar(){
        if(Build.VERSION.SDK_INT>=31&&checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)!=PackageManager.PERMISSION_GRANTED){
            new AlertDialog.Builder(this).setTitle("Eşleşmiş aracını listele").setMessage("Telefonla önceden eşleştirdiğin Bluetooth cihazlarını göstermek için Yakındaki cihazlar izni gerekir. Yeni cihaz taranmaz, konum izni istenmez; cihaz listesi buluta gönderilmez.").setNegativeButton("Vazgeç",null).setPositiveButton("İzin ver",(d,w)->requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT},43)).show();return;
        }
        if(!CarSilence.ready()){carSilenceDetails();return;}
        carSilence.list(devices->{
            if(isFinishing()||isDestroyed())return;
            if(devices.length()==0){toast("Eşleşmiş cihaz yok. Önce telefonun Bluetooth ayarlarından aracını eşleştir.");return;}
            String[] labels=new String[devices.length()];
            for(int i=0;i<labels.length;i++){org.json.JSONObject device=devices.optJSONObject(i);labels[i]=device.optString("name")+" · "+device.optString("address")+(device.optInt("state")==2?" · görüşme bağlantısı var":"");}
            new AlertDialog.Builder(this).setTitle("Yalnızca aracını seç").setItems(labels,(d,index)->{
                org.json.JSONObject device=devices.optJSONObject(index);String address=device.optString("address"),name=device.optString("name");
                new AlertDialog.Builder(this).setTitle("Araç görüşmeleri kapatılsın mı?").setMessage(name+"\nBeyaz listedekiler ve normal telefon aramaları dahil, bu aracın Bluetooth görüşme profili kapatılır. Müzik profili değiştirilmez. Bu işlem çağrıyı telefonun kendisinde reddetmez.\nUygulamayı silmeden veya verilerini temizlemeden önce bu modu kapat. ADB bağlantısı yoksa telefonun Bluetooth → araç → Aramalar ayarını elle açabilirsin.")
                    .setNegativeButton("Vazgeç",null).setPositiveButton("Anladım, aç",(dialog,which)->carSilence.enable(address,name)).show();
            }).setNegativeButton("Vazgeç",null).show();
        });
    }
    private void openWeb(String url){try{startActivity(new Intent(Intent.ACTION_VIEW,android.net.Uri.parse(url)));}catch(ActivityNotFoundException|SecurityException ex){toast("Bağlantı açılamadı: "+url);}}
    private void carSilenceDetails(){
        if(adbGuide==null)adbGuide=new AdbGuide(this,carSilence,this::chooseSilentCar,this::toast);
        adbGuide.show();
    }
    private void backgroundCard(){
        LinearLayout background=card();background.addView(button("Arka planda çalışma    ›",false,this::backgroundDetails));gap(background,7);
        background.addView(text(CallNotificationService.connected?"● Bildirim servisi bağlı":"○ Bildirim servisi bağlı değil",12,CallNotificationService.connected?GREEN:MUTED,true));
    }
    private void backgroundDetails(){
        LinearLayout panel=column();panel.setPadding(dp(22),dp(8),dp(22),dp(12));
        panel.addView(text(BackgroundProtection.status(this),13,INK,false));gap(panel,14);
        panel.addView(text("Ekranı veya son kullanılan uygulamalar listesini kapatsan da koruma normalde devam eder. Android bağlantıyı yönetir; servis koparsa yeniden bağlanma istenir. Zorla durdurduktan sonra CallGuard’ı tekrar açmalısın.",13,MUTED,false));gap(panel,14);
        panel.addView(button("CallGuard pil ayarını aç",true,()->openBackgroundSettings(false)));gap(panel,8);
        panel.addView(text("Açılan CallGuard uygulama bilgilerinde Pil → Kısıtlanmamış seçeneğini kontrol et. Kısıtlanmamış zaten seçiliyse değiştirme. Menü adı telefona göre değişebilir; bu ayar otomatik değiştirilmez.",12,MUTED,false));gap(panel,12);
        if(BackgroundProtection.samsung()){
            panel.addView(button("Samsung uyku listesi yardımı",false,this::samsungSleepHelp));gap(panel,8);
            panel.addView(text("CallGuard uyku listesinde görünmeyebilir; bu tek başına bir arıza değildir. Önce yukarıdaki CallGuard pil ayarını kontrol et. Samsung’un uyku listesi üyeliğini uygulamadan doğrulayamıyoruz.",12,MUTED,false));gap(panel,12);
        }
        panel.addView(button("Servisi yeniden bağla",false,()->{
            if(!access()){permissionExplanation();return;}
            if(CallNotificationService.connected){toast("Bildirim servisi zaten bağlı.");return;}
            toast(BackgroundProtection.reconnect(this)?"Android’e yeniden bağlanma isteği gönderildi.":"Yakın zamanda istek gönderildi. Bağlantı gelmezse bildirim erişimini kapatıp yeniden aç.");
        }));
        ScrollView content=new ScrollView(this);content.addView(panel);
        new AlertDialog.Builder(this).setTitle("Arka planda çalışma").setView(content).setPositiveButton("Kapat",null).show();
    }
    private void samsungSleepHelp(){
        String detected=BackgroundProtection.batteryExempt(this)
            ?"Android, CallGuard için pil optimizasyonu muafiyeti bildiriyor. Bu, Samsung uyku listesini doğruladığımız anlamına gelmez."
            :"Android genel pil optimizasyonu muafiyeti bildirmiyor. Bu bilgi tek başına Samsung’un uygulamayı uyuttuğu anlamına gelmez.";
        new AlertDialog.Builder(this).setTitle("CallGuard listede görünmüyor mu?")
            .setMessage(detected+"\n\nSamsung’un bazı sürümlerinde Kısıtlanmamış olan uygulamalar Asla uyumayan uygulamalar için ekleme listesinde görünmeyebilir.\n\nCallGuard → Pil → Kısıtlanmamış seçiliyse değiştirme; sırf listede görünmesi için Optimize edilmiş seçeneğine dönme. Listede ekleme yapmak istersen + düğmesine bak. Uyuyan / derin uyuyan listelerinde CallGuard varsa oradan çıkar.\n\nBu listeleri CallGuard kendisi değiştiremez.")
            .setPositiveButton("CallGuard pil ayarı",(dialog,which)->openBackgroundSettings(false))
            .setNeutralButton("Uyku listesini aç",(dialog,which)->openBackgroundSettings(true))
            .setNegativeButton("Kapat",null).show();
    }
    private void openBackgroundSettings(boolean samsung){
        if(samsung){
            for(String owner:new String[]{"com.samsung.android.lool","com.samsung.android.sm"})try{
                startActivity(new Intent("com.samsung.android.sm.ACTION_OPEN_CHECKABLE_LISTACTIVITY").setPackage(owner).putExtra("activity_type",2));return;
            }catch(ActivityNotFoundException|SecurityException ignored){}
            toast("Uyku listesi doğrudan açılamadı. Pil → Arka plan kullanım sınırları yolunu kullan.");
        }
        try{startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,android.net.Uri.parse("package:"+getPackageName())));}
        catch(ActivityNotFoundException|SecurityException ex){toast("Telefonun Ayarlar → Uygulamalar → CallGuard → Pil menüsünü aç.");}
    }
    private void backupSettings(){
        body.addView(button("‹  Ayarlara dön",false,this::closeBackupPage));gap(body,18);
        heading("Yedekleme ve geri yükleme","Hesabını ve yedeklerini buradan yönet.");
        accountCard();driveCard();
    }
    private void closeBackupPage(){backupPage=false;render();}
    private void syncBackupBack(){
        if(Build.VERSION.SDK_INT<33)return;
        if(tab==3&&backupPage&&backupBackCallback==null){
            android.window.OnBackInvokedCallback callback=this::closeBackupPage;
            backupBackCallback=callback;getOnBackInvokedDispatcher().registerOnBackInvokedCallback(android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT,callback);
        }else if((tab!=3||!backupPage)&&backupBackCallback!=null){
            getOnBackInvokedDispatcher().unregisterOnBackInvokedCallback((android.window.OnBackInvokedCallback)backupBackCallback);backupBackCallback=null;
        }
    }
    @Override public void onBackPressed(){if(tab==3&&backupPage)closeBackupPage();else super.onBackPressed();}
    private void runCloud(String title,java.util.concurrent.Callable<org.json.JSONObject> action,java.util.function.Consumer<org.json.JSONObject> done){
        ProgressDialog progress=ProgressDialog.show(this,title,"Lütfen bekle…",true,false);worker.execute(()->{try{org.json.JSONObject result=action.call();runOnUiThread(()->{if(isFinishing()||isDestroyed())return;progress.dismiss();done.accept(result);});}catch(Exception ex){runOnUiThread(()->{if(isFinishing()||isDestroyed())return;progress.dismiss();toast(ex instanceof java.io.IOException?ex.getMessage():"İşlem tamamlanamadı. Yeniden dene.");});}});
    }
    private void accountCard(){
        LinearLayout card=card();card.addView(text("Hesap ve bulut yedekleme",17,INK,true));gap(card,8);card.addView(text(LicenseManager.status(this),14,GREEN,true));gap(card,8);
        if(!CloudClient.configured()){card.addView(text("Hesap ve bulut hizmeti sunucu kurulumu tamamlandığında açılacak. Bu önizlemede ödeme alınmaz; özellikler kullanımına açık.",13,MUTED,false));return;}
        if(SessionVault.userId(this).isEmpty()){card.addView(text(BuildConfig.COMMERCIAL_ENABLED?"Yedeklerini geri almak ve deneme süreni korumak için aynı hesabı kullan. Deneme ilk koruma etkinleştirmesinde başlar.":"Google hesabınla veya e-posta adresinle giriş yap. Listelerin, ayarların ve isteğe bağlı arama geçmişin CallGuard sunucusunda yedeklenir. Bu önizlemede ödeme alınmaz.",13,MUTED,false));gap(card,12);if(!BuildConfig.GOOGLE_CLIENT_ID.isEmpty()){card.addView(button("Google ile devam et",true,this::googleLogin));gap(card,8);}card.addView(button("E-posta ile giriş / kayıt",false,this::emailLogin));return;}
        card.addView(text(SessionVault.email(this),14,INK,true));gap(card,10);
        if(BuildConfig.COMMERCIAL_ENABLED){card.addView(button("7 günlük denemeyi başlat",true,this::beginTrial));gap(card,8);card.addView(button("Pro · tek seferlik satın al",false,()->{if(billing==null)billing=new ProBilling(this,worker,this::toast);billing.purchase();}));gap(card,8);card.addView(button("Satın almayı geri yükle",false,()->{if(billing==null)billing=new ProBilling(this,worker,this::toast);billing.restore();}));gap(card,8);}
        android.content.SharedPreferences cloud=getSharedPreferences("cloud",0);boolean auto=cloud.getBoolean("autoBackup",false);card.addView(button(auto?"Bulut yedeği açık · şimdi yedekle":"Bulut yedeklemeyi aç",true,this::backupCloud));gap(card,8);card.addView(button("Buluttan geri yükle",false,this::restoreCloud));gap(card,8);
        long last=cloud.getLong("lastBackup",0);if(last>0)card.addView(text("Son yedek: "+new SimpleDateFormat("d MMM yyyy HH:mm",new Locale("tr")).format(new Date(last)),12,MUTED,false));String error=cloud.getString("lastError","");if(!error.isEmpty())card.addView(text(error,12,MUTED,false));gap(card,8);
        if(auto){card.addView(button("Otomatik yedeklemeyi kapat",false,()->{cloud.edit().putBoolean("autoBackup",false).apply();render();}));gap(card,8);}
        card.addView(button("Hesaptan çıkış yap",false,()->new AlertDialog.Builder(this).setTitle("Çıkış yapılsın mı?").setMessage("Cihazındaki listeler kalır. Hesaba bağlı deneme veya Pro korumasını kullanmak için tekrar giriş yapman gerekir.").setNegativeButton("Vazgeç",null).setPositiveButton("Çıkış yap",(d,w)->runCloud("Çıkış yapılıyor",()->CloudClient.request(this,"POST","/v1/auth/logout",new org.json.JSONObject(),true),result->{CloudSync.cancel(this);SessionVault.clear(this);if(BuildConfig.COMMERCIAL_ENABLED)store.setting("enabled",false);render();})).show()));gap(card,8);
        card.addView(button("Hesabı ve bulut verilerini sil",false,()->new AlertDialog.Builder(this).setTitle("Hesabı kalıcı olarak sil?").setMessage("CallGuard sunucusundaki yedeğin, hesabın ve bu cihazdaki listeler/geçmiş silinir. Google Drive kopyaların ayrı saklanır; onları Google Drive bölümünden ayrıca silebilirsin. Satın alma ve deneme için gerekli asgari kayıtlar korunur.").setNegativeButton("Vazgeç",null).setPositiveButton("Hesabımı sil",(d,w)->runCloud("Hesap siliniyor",()->CloudClient.request(this,"DELETE","/v1/account",null,true),result->{CloudSync.cancel(this);SessionVault.clear(this);store.getWritableDatabase().delete("contacts",null,null);store.clearHistory();store.prefs().edit().clear().apply();render();toast("Hesabın ve uygulama verilerin silindi.");})).show()));
    }
    private void driveCard(){
        LinearLayout card=card();card.addView(text("Google Drive yedeği",17,INK,true));gap(card,8);
        card.addView(text("İsteğe bağlı ek yedekleme. Google veya e-posta ile girişten bağımsızdır. Yalnızca CallGuard'ın Drive uygulama alanına erişilir; diğer dosyaların okunmaz.",13,MUTED,false));gap(card,12);
        card.addView(button("Google Drive'a şimdi yedekle",true,()->new AlertDialog.Builder(this).setTitle("Google Drive'a yedekle?").setMessage("CallGuard listeleri, ayarları ve en fazla 500 arama kaydı seçeceğin Google hesabının uygulamaya özel Drive alanına gönderilecek. Her işlem ayrı bir tarihli kopya oluşturur. Drive yedeklemesi bu sürümde elle başlatılır; CallGuard bulut yedeği ayrı yönetilir.").setNegativeButton("Vazgeç",null).setPositiveButton("Hesap seç",(d,w)->startDrive("backup")).show()));gap(card,8);
        card.addView(button("Drive yedeklerini görüntüle / geri yükle",false,()->startDrive("list")));gap(card,8);
        android.content.SharedPreferences drive=getSharedPreferences("drive",0);long last=drive.getLong("lastBackup",0);
        if(last>0)card.addView(text("Bu cihazdan son Drive yedeği: "+new SimpleDateFormat("d MMM yyyy HH:mm",new Locale("tr")).format(new Date(last))+"\nGoogle hesabı: "+drive.getString("account",""),12,MUTED,false));
        gap(card,8);card.addView(text("Kopyalar Drive'ın normal dosya listesinde görünmez. Geri yüklemek veya tek bir kopyayı silmek için yukarıdaki düğmeyi kullan. Google erişimini hesabının Üçüncü taraf bağlantıları bölümünden kaldırabilirsin; bu, CallGuard sunucusundaki hesabını silmez.",11,MUTED,false));
    }
    private void startDrive(String action){
        if(!driveAction.isEmpty()){toast("Önce açık Google hesap seçimini tamamla.");return;}
        driveAction=action;
        try{Intent pick=com.google.android.gms.common.AccountPicker.newChooseAccountIntent(new com.google.android.gms.common.AccountPicker.AccountChooserOptions.Builder().setAllowableAccountsTypes(Arrays.asList("com.google")).build());startActivityForResult(pick,5900);}
        catch(Exception ex){driveAction="";toast("Google hesap seçimi açılamadı. Google Play Hizmetlerini kontrol et.");}
    }
    @Override protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);
        if(request==5900){
            if(result!=RESULT_OK||data==null){driveAction="";return;}
            String name=data.getStringExtra(android.accounts.AccountManager.KEY_ACCOUNT_NAME);
            if(name==null||name.isEmpty()){driveAction="";toast("Google hesabı seçilmedi.");return;}
            driveAccount=name;
            com.google.android.gms.auth.api.identity.AuthorizationRequest authorization=com.google.android.gms.auth.api.identity.AuthorizationRequest.builder().setAccount(new android.accounts.Account(name,"com.google")).setRequestedScopes(Collections.singletonList(new com.google.android.gms.common.api.Scope(DriveBackup.SCOPE))).build();
            com.google.android.gms.auth.api.identity.Identity.getAuthorizationClient(this).authorize(authorization).addOnSuccessListener(this,response->{
                if(response.hasResolution()){try{startIntentSenderForResult(response.getPendingIntent().getIntentSender(),5901,null,0,0,0);}catch(Exception ex){driveAction="";toast("Drive izin ekranı açılamadı.");}}
                else finishDriveAuthorization(response);
            }).addOnFailureListener(this,error->{driveAction="";toast("Drive izni alınamadı. Google Cloud yapılandırmasını ve test hesabını kontrol et.");});
        }else if(request==5901){
            if(result!=RESULT_OK||data==null){driveAction="";toast("Drive izni verilmedi; mevcut yedekler değişmedi.");return;}
            try{finishDriveAuthorization(com.google.android.gms.auth.api.identity.Identity.getAuthorizationClient(this).getAuthorizationResultFromIntent(data));}
            catch(Exception ex){driveAction="";toast("Drive yetkilendirmesi tamamlanamadı.");}
        }
    }
    private void finishDriveAuthorization(com.google.android.gms.auth.api.identity.AuthorizationResult result){
        final String action=driveAction,account=driveAccount,token=result.getAccessToken();driveAction="";
        if(action.isEmpty())return;
        if(token==null||token.isEmpty()||!result.getGrantedScopes().contains(DriveBackup.SCOPE)){toast("CallGuard Drive uygulama alanı izni verilmedi.");return;}
        final DriveBackup drive=new DriveBackup(token);
        if(action.equals("backup"))runCloud("Drive'a yedekleniyor",()->{org.json.JSONObject saved=drive.save(BackupData.export(store));getSharedPreferences("drive",0).edit().putLong("lastBackup",saved.getLong("savedAt")).putString("account",account).apply();return saved;},saved->{render();toast("Google Drive yedeği oluşturuldu.");});
        else runCloud("Drive yedekleri alınıyor",()->new org.json.JSONObject().put("files",drive.list()),response->{
            try{org.json.JSONArray files=response.getJSONArray("files");if(files.length()==0){toast("Seçilen Google hesabında CallGuard Drive yedeği yok.");return;}
                String[] labels=new String[files.length()];for(int i=0;i<labels.length;i++){String time=files.getJSONObject(i).optString("createdTime","");try{labels[i]=new SimpleDateFormat("d MMM yyyy HH:mm:ss",new Locale("tr")).format(Date.from(java.time.Instant.parse(time)));}catch(Exception e){labels[i]=time;}}
                new AlertDialog.Builder(this).setTitle("Drive yedekleri · "+account).setItems(labels,(dialog,index)->{try{driveSnapshot(drive,files.getJSONObject(index).getString("id"),labels[index],account);}catch(Exception ex){toast("Drive yedeği seçilemedi.");}}).setNegativeButton("Kapat",null).show();
            }catch(Exception ex){toast("Drive yedek listesi okunamadı.");}
        });
    }
    private void driveSnapshot(DriveBackup drive,String id,String date,String account){
        new AlertDialog.Builder(this).setTitle(date).setItems(new String[]{"Bu yedeği geri yükle","Bu Drive yedeğini sil"},(dialog,index)->{
            if(index==1)new AlertDialog.Builder(this).setTitle("Drive kopyası kalıcı olarak silinsin mi?").setMessage(account+"\n"+date+"\nYalnızca bu kopya silinir; cihazındaki veriler ve CallGuard bulut yedeği korunur.").setNegativeButton("Vazgeç",null).setPositiveButton("Sil",(d,w)->runCloud("Drive yedeği siliniyor",()->{drive.delete(id);return new org.json.JSONObject();},done->toast("Seçilen Drive kopyası silindi."))).show();
            else new AlertDialog.Builder(this).setTitle("Drive yedeği geri yüklensin mi?").setMessage(account+"\n"+date+"\nCihazındaki CallGuard listeleri, ayarlar ve geçmiş bu kopyayla değiştirilecek. Koruma ve otomatik CallGuard bulut yedeklemesi kapatılır; buluttaki mevcut yedek korunur. Daha sonra kendin açabilirsin.").setNegativeButton("Vazgeç",null).setPositiveButton("Geri yükle",(d,w)->runCloud("Drive'dan geri yükleniyor",()->{
                org.json.JSONObject backup=drive.read(id);
                synchronized(CloudSync.BACKUP_LOCK){getSharedPreferences("cloud",0).edit().putBoolean("autoBackup",false).putBoolean("backupReady",false).apply();CloudSync.cancel(this);BackupData.restore(store,backup);}
                return new org.json.JSONObject();
            },done->{render();toast("Drive yedeği geri yüklendi. Koruma ve otomatik bulut yedeklemesini kontrol et.");})).show();
        }).setNegativeButton("Kapat",null).show();
    }
    private void diagnosticsCard(){
        LinearLayout card=card();card.addView(text("Çağrı tanılaması",17,INK,true));gap(card,8);
        card.addView(text("Algılanmayan çağrıların teknik nedenini gösterir. Kayıtlar yalnızca bu cihazda tutulur; isim, numara, mesaj içeriği veya hesap bilgisi içermez.",12,MUTED,false));gap(card,10);
        card.addView(button("Teknik kaydı göster / kopyala",false,()->{
            String report=CallDiagnostics.report(this)+"\nKoruma="+store.enabled()+", bildirimIzni="+access()+", baglanti="+CallNotificationService.connected+", secim="+store.effectiveMedia()+", mod="+store.effectiveMode()+", zaman="+store.scheduleActive()+", yalnizKayit="+store.recordOnly();
            TextView details=text(report,12,INK,false);details.setTextIsSelectable(true);details.setPadding(dp(16),dp(12),dp(16),dp(12));
            ScrollView panel=new ScrollView(this);panel.addView(details);
            new AlertDialog.Builder(this).setTitle("Çağrı tanılaması").setView(panel).setNegativeButton("Kapat",null).setNeutralButton("Temizle",(d,w)->{CallDiagnostics.clear(this);toast("Yerel teknik kayıt temizlendi.");}).setPositiveButton("Kopyala",(d,w)->{android.content.ClipboardManager clipboard=(android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE);clipboard.setPrimaryClip(ClipData.newPlainText("CallGuard teknik kayıt",report));toast("İsim ve numara içermeyen teknik kayıt kopyalandı.");}).show();
        }));
    }
    private void acceptSession(org.json.JSONObject result){
        try{SessionVault.save(this,result.getString("token"),result.getString("userId"),result.getString("email"));CloudSync.periodic(this);CloudSync.enqueue(this);render();toast("Giriş yapıldı. Mevcut bulut yedeğini geri yükleyebilirsin.");}catch(Exception ex){toast("Oturum güvenli biçimde kaydedilemedi.");}
    }
    private void googleLogin(){
        runCloud("Google girişi hazırlanıyor",()->CloudClient.request(this,"POST","/v1/auth/challenge",new org.json.JSONObject(),false),challenge->{
            try{com.google.android.libraries.identity.googleid.GetGoogleIdOption option=new com.google.android.libraries.identity.googleid.GetGoogleIdOption.Builder().setServerClientId(BuildConfig.GOOGLE_CLIENT_ID).setFilterByAuthorizedAccounts(false).setAutoSelectEnabled(false).setNonce(challenge.getString("nonce")).build();
                androidx.credentials.GetCredentialRequest request=new androidx.credentials.GetCredentialRequest.Builder().addCredentialOption(option).build();
                androidx.credentials.CredentialManager.create(this).getCredentialAsync(this,request,new android.os.CancellationSignal(),androidx.core.content.ContextCompat.getMainExecutor(this),new androidx.credentials.CredentialManagerCallback<androidx.credentials.GetCredentialResponse,androidx.credentials.exceptions.GetCredentialException>(){
                    @Override public void onResult(androidx.credentials.GetCredentialResponse response){try{String token=com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.createFrom(response.getCredential().getData()).getIdToken();runCloud("Hesap doğrulanıyor",()->CloudClient.request(MainActivity.this,"POST","/v1/auth/google",new org.json.JSONObject().put("idToken",token),false),MainActivity.this::acceptSession);}catch(Exception ex){toast("Google hesabı doğrulanamadı.");}}
                    @Override public void onError(androidx.credentials.exceptions.GetCredentialException error){toast("Google ile giriş tamamlanmadı. E-posta ile giriş yapabilirsin.");}
                });
            }catch(Exception ex){toast("Google girişi başlatılamadı.");}
        });
    }
    private ScrollView accountFormScroll(LinearLayout form){ScrollView view=new ScrollView(this);view.setFillViewport(false);view.addView(form,new ScrollView.LayoutParams(-1,-2));return view;}
    private void emailLogin(){
        LinearLayout form=column();form.setPadding(dp(24),dp(12),dp(24),dp(8));EditText email=input("E-posta adresi");email.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);EditText password=input("Şifre (kayıt için en az 12 karakter)");password.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);form.addView(email);gap(form,12);form.addView(password);gap(form,8);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("CallGuard hesabı").setView(accountFormScroll(form)).setNegativeButton("Kapat",null).create();
        java.util.function.Consumer<String> submit=route->{String mail=email.getText().toString().trim(),pass=password.getText().toString();if(mail.isEmpty()){email.setError("E-posta adresini yaz.");return;}if((route.equals("login")||route.equals("register"))&&pass.isEmpty()){password.setError("Şifreni yaz.");return;}if(route.equals("register")&&pass.codePointCount(0,pass.length())<12){password.setError("En az 12 karakter kullan.");return;}
            runCloud("Hesap işlemi",()->CloudClient.request(this,"POST","/v1/auth/"+route,new org.json.JSONObject().put("email",mail).put("password",pass),false),result->{if(result.has("token")){dialog.dismiss();acceptSession(result);}else toast(result.optString("message","E-postanı kontrol et."));});};
        form.addView(button("Giriş yap",true,()->submit.accept("login")));gap(form,8);form.addView(button("Yeni hesap oluştur",false,()->submit.accept("register")));gap(form,8);form.addView(button("Şifremi unuttum",false,()->submit.accept("reset")));gap(form,8);form.addView(button("Doğrulama e-postasını tekrar gönder",false,()->submit.accept("resend")));dialog.show();if(dialog.getWindow()!=null)dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
    }
    private void beginTrial(){
        if(!CloudClient.configured()){toast("Bu önizlemede deneme ve ödeme henüz etkin değil.");return;}
        if(SessionVault.userId(this).isEmpty()){toast("Denemeyi başlatmak için önce hesabına giriş yap.");tab=3;render();return;}
        if(!access()){permissionExplanation();return;}
        runCloud("Deneme süresi doğrulanıyor",()->CloudClient.request(this,"POST","/v1/trial",new org.json.JSONObject(),true),data->{LicenseManager.cache(this,data);if(LicenseManager.allowed(this)){store.setting("enabled",true);CloudSync.periodic(this);if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},41);toast("Koruma etkin. Deneme bitince otomatik ödeme alınmaz.");}else toast("Bu hesabın denemesi sona ermiş. Pro satın alabilir veya satın almanı geri yükleyebilirsin.");render();});
    }
    private void backupCloud(){
        runCloud("Bulut yedeği kontrol ediliyor",()->CloudClient.request(this,"GET","/v1/backup",null,true),existing->{new AlertDialog.Builder(this).setTitle(existing.optInt("revision")>0?"Bulut yedeği güncellensin mi?":"Bulut yedeklemeyi aç?").setMessage("Bu cihazdaki CallGuard kişi listeleri, ayarlar ve en fazla 500 arama kaydı kendi sunucumuza gönderilir. Telefon rehberinin tamamı yüklenmez. Mevcut bulut yedeği bu cihazdaki verilerle değiştirilir.").setNegativeButton("Vazgeç",null).setPositiveButton("Yedekle",(d,w)->runCloud("Yedekleniyor",()->{org.json.JSONObject result=CloudClient.request(this,"PUT","/v1/backup",new org.json.JSONObject().put("revision",existing.optInt("revision")).put("backup",BackupData.export(store)),true);getSharedPreferences("cloud",0).edit().putInt("revision",result.getInt("revision")).putBoolean("backupReady",true).putBoolean("autoBackup",true).putBoolean("dirty",false).putLong("lastBackup",System.currentTimeMillis()).remove("lastError").apply();return result;},result->{CloudSync.periodic(this);render();toast("Bulut yedeği tamamlandı. Değişiklikler internet olduğunda otomatik yedeklenecek.");})).show();});
    }
    private void restoreCloud(){
        runCloud("Bulut yedeği alınıyor",()->CloudClient.request(this,"GET","/v1/backup",null,true),result->{if(result.isNull("backup")||!result.has("backup")){toast("Bu hesapta bulut yedeği yok.");return;}new AlertDialog.Builder(this).setTitle("Yedeği geri yükle?").setMessage("Bu cihazdaki CallGuard listeleri, geçmiş ve profil ayarları bulut yedeğiyle değiştirilecek. Android izinleri yeniden verilebilir; korumayı geri yüklemeden sonra kendin açmalısın.").setNegativeButton("Vazgeç",null).setPositiveButton("Geri yükle",(d,w)->runCloud("Geri yükleniyor",()->{BackupData.restore(store,result.getJSONObject("backup"));getSharedPreferences("cloud",0).edit().putInt("revision",result.getInt("revision")).putBoolean("backupReady",true).putBoolean("dirty",false).apply();return result;},done->{render();toast("Veriler geri yüklendi. Koruma anahtarını kontrol et.");})).show();});
    }
    private String profileName(String id){return id.equals("work")?"İş":id.equals("sleep")?"Uyku":id.equals("meeting")?"Toplantı":"Kişisel";}
    private String clockText(int minutes){return String.format(new Locale("tr"),"%02d:%02d",minutes/60,minutes%60);}
    private void scheduleCard(){
        LinearLayout c=card();c.addView(text("Profiller ve zamanlama",17,INK,true));gap(c,12);
        String[] keys={"custom","work","sleep","meeting"};int selected=Arrays.asList(keys).indexOf(store.profile());
        segmented(c,new String[]{"Kişisel","İş","Uyku","Toplantı"},Math.max(0,selected),index->{store.selectProfile(keys[index]);render();});gap(c,12);
        LinearLayout row=row();row.addView(text("Saatlere göre çalıştır",14,INK,true),new LinearLayout.LayoutParams(0,-2,1));Switch enabled=new Switch(this);enabled.setChecked(store.scheduled());enabled.setContentDescription("Zamanlama anahtarı");enabled.setOnCheckedChangeListener((button,on)->{store.scheduleEnabled(on);render();});row.addView(enabled);c.addView(row);gap(c,8);
        c.addView(text("Her profil engelleme kuralını, arama türünü ve saatlerini ayrı saklar. Kişi listeleri ortaktır.",12,MUTED,false));gap(c,12);
        c.addView(button("Günler ve saatler · "+clockText(store.scheduleStart())+"–"+clockText(store.scheduleEnd()),false,this::editSchedule));gap(c,6);c.addView(text("Başlangıç ve bitiş aynıysa seçili günün tamamında çalışır. Geceyi aşan saatler başlangıç gününe aittir.",11,MUTED,false));
    }
    private void editSchedule(){
        LinearLayout form=column();form.setPadding(dp(24),dp(12),dp(24),dp(8));
        String[] days={"Pazartesi","Salı","Çarşamba","Perşembe","Cuma","Cumartesi","Pazar"};CheckBox[] checks=new CheckBox[7];for(int i=0;i<7;i++){checks[i]=new CheckBox(this);checks[i].setText(days[i]);checks[i].setChecked((store.scheduleDays()&(1<<i))!=0);form.addView(checks[i]);}
        int[] times={store.scheduleStart(),store.scheduleEnd()};TextView start=button("Başlangıç · "+clockText(times[0]),false,()->{});TextView end=button("Bitiş · "+clockText(times[1]),false,()->{});
        start.setOnClickListener(v->new TimePickerDialog(this,(picker,h,m)->{times[0]=h*60+m;start.setText("Başlangıç · "+clockText(times[0]));},times[0]/60,times[0]%60,true).show());end.setOnClickListener(v->new TimePickerDialog(this,(picker,h,m)->{times[1]=h*60+m;end.setText("Bitiş · "+clockText(times[1]));},times[1]/60,times[1]%60,true).show());form.addView(start);gap(form,8);form.addView(end);
        ScrollView scroll=new ScrollView(this);scroll.addView(form);AlertDialog dialog=new AlertDialog.Builder(this).setTitle(profileName(store.profile())+" saatleri").setView(scroll).setNegativeButton("Vazgeç",null).setPositiveButton("Kaydet",null).create();dialog.setOnShowListener(d->dialog.getButton(-1).setOnClickListener(v->{int mask=0;for(int i=0;i<7;i++)if(checks[i].isChecked())mask|=1<<i;if(mask==0){toast("En az bir gün seç.");return;}store.profileSetting("days",mask);store.profileSetting("start",times[0]);store.profileSetting("end",times[1]);dialog.dismiss();render();}));dialog.show();
    }
    private void radio(LinearLayout parent,String label,boolean checked,Runnable change){LinearLayout r=row();RadioButton b=new RadioButton(this);b.setChecked(checked);b.setButtonTintList(android.content.res.ColorStateList.valueOf(checked?GREEN:MUTED));b.setClickable(false);b.setFocusable(false);r.addView(b,new LinearLayout.LayoutParams(dp(39),dp(40)));r.addView(text(label,14,INK,false));r.setContentDescription(label);r.setFocusable(true);r.setOnClickListener(v->change.run());parent.addView(r);}
    private void permissionExplanation(){new AlertDialog.Builder(this).setTitle("Bildirim erişimine izin ver").setMessage("Bildirim erişimi Android tarafından diğer uygulamaların bildirimlerine de erişim verebilir. CallGuard yalnızca WhatsApp ve WhatsApp Business sesli/görüntülü çağrı bildirimlerini, uygulama açık değilken de, seçtiğin kuralları uygulamak için işler. Arayan adı/numarası, çağrı türü ve Reddet eylemi kullanılır; mesaj bildirimleri işlenmez. Seçtiğin kişi listeleri ve en fazla 500 çağrı geçmişi cihazında tutulur. CallGuard bulutuna veya Google Drive’a aktarım yalnızca ilgili yedekleme seçeneğine ayrıca onay verdiğinde yapılır. İzni Android ayarlarından her zaman kapatabilirsin.").setNegativeButton("Vazgeç",null).setPositiveButton("Kabul et ve ayarları aç",(d,w)->openAccess()).show();}
    private void openAccess(){try{if(Build.VERSION.SDK_INT>=30)startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS).putExtra(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,new ComponentName(this,CallNotificationService.class).flattenToString()));else startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));BackgroundProtection.reconnect(this);}catch(ActivityNotFoundException e){startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));}}
    private void about(){new AlertDialog.Builder(this).setTitle("Program hakkında").setMessage("Call Guard by AC\nSürüm "+BuildConfig.VERSION_NAME+"\n\nWhatsApp veya Meta ile bağlantılı değildir.").setPositiveButton("Tamam",null).show();}
    private void toast(String value){Toast.makeText(this,value,Toast.LENGTH_LONG).show();}
    private final class Icon extends View {
        final String kind; final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG); final int color;
        Icon(String kind,int color){super(MainActivity.this);this.kind=kind;this.color=color;setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);}
        @Override protected void onDraw(Canvas c){super.onDraw(c);c.save();c.scale(getWidth()/32f,getHeight()/32f);p.setColor(color);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2.2f);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);
            if(kind.equals("shield")){Path s=new Path();s.moveTo(16,3);s.lineTo(28,8);s.lineTo(26,20);s.quadTo(23,27,16,30);s.quadTo(9,27,6,20);s.lineTo(4,8);s.close();c.drawPath(s,p);c.drawLine(10,16,14,20,p);c.drawLine(14,20,22,12,p);}
            else if(kind.equals("warning")){Path triangle=new Path();triangle.moveTo(16,3);triangle.lineTo(30,28);triangle.lineTo(2,28);triangle.close();c.drawPath(triangle,p);c.drawLine(16,12,16,19,p);p.setStyle(Paint.Style.FILL);c.drawCircle(16,24,1.4f,p);}
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
