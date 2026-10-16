package com.callguard;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.*;
import android.os.SystemClock;
import java.util.*;

/** Conservative UI adapter. No coordinate taps, no chat-message parsing. */
public final class CallGuardService extends AccessibilityService {
    private Store store;
    private String sessionKey="";
    private long lastSeen=0;
    private boolean handled=false;
    private int attempts=0;
    @Override protected void onServiceConnected() { store=new Store(this); }
    @Override public void onAccessibilityEvent(AccessibilityEvent event) {
        if (!getSharedPreferences("settings",MODE_PRIVATE).getBoolean("enabled",true)) return;
        String pkg=event.getPackageName()==null?"":event.getPackageName().toString();
        if (!pkg.equals("com.whatsapp") && !pkg.equals("com.whatsapp.w4b")) return;
        AccessibilityNodeInfo root=getRootInActiveWindow();
        if(root==null)return;
        try {
            if(!pkg.contentEquals(root.getPackageName()))return;
            List<AccessibilityNodeInfo> nodes=new ArrayList<>(); collect(root,nodes,0);
            try {
                boolean voice=false,answer=false,video=false;
                AccessibilityNodeInfo reject=null;
                String caller=null;
                for(AccessibilityNodeInfo node:nodes) {
                    String text=Policy.normalize(str(node.getText()));
                    String desc=Policy.normalize(str(node.getContentDescription()));
                    voice |= Policy.voiceLabel(text)||Policy.voiceLabel(desc);
                    answer |= Policy.answerLabel(text)||Policy.answerLabel(desc);
                    video |= text.contains("video call")||text.contains("görüntülü arama")||desc.contains("video call")||desc.contains("görüntülü arama");
                    if(Policy.rejectLabel(text)||Policy.rejectLabel(desc))reject=node;
                    String id=str(node.getViewIdResourceName());
                    // Only dedicated call-screen IDs may supply caller identity.
                    if(id.endsWith(":id/call_screen_caller_name") || id.endsWith(":id/caller_name") || id.endsWith(":id/call_screen_contact_name")) {
                        if(node.getText()!=null && !node.getText().toString().trim().isEmpty())caller=node.getText().toString();
                    }
                }
                if(!voice || !answer || reject==null || video) return;
                long now=SystemClock.elapsedRealtime();
                String key=pkg+":"+(caller==null?"?":caller);
                if(!key.equals(sessionKey) || now-lastSeen>15000) {sessionKey=key;handled=false;attempts=0;}
                lastSeen=now;
                if(handled)return;
                if(store==null)store=new Store(this);
                if(store.permits(caller)){handled=true;return;}
                if(getSharedPreferences("settings",MODE_PRIVATE).getString("mode","reject").equals("ignore")) {
                    store.log(caller,"Yok sayıldı · ses susturulmadı");handled=true;return;
                }
                AccessibilityNodeInfo target=reject;
                // Walk only inside the identified decline control.
                AccessibilityNodeInfo parent=null;
                boolean clicked=false;
                try {
                    for(int i=0;i<4 && target!=null;i++) {
                        if(target.isEnabled() && target.isClickable()) {clicked=target.performAction(AccessibilityNodeInfo.ACTION_CLICK);break;}
                        AccessibilityNodeInfo next=target.getParent();
                        if(parent!=null)parent.recycle();
                        parent=next;target=parent;
                    }
                } finally {if(parent!=null)parent.recycle();}
                attempts++;
                if(clicked){store.log(caller,"Reddetme komutu gönderildi");handled=true;}
                else if(attempts>=3){store.log(caller,"Reddedilemedi · düğme erişilemiyor");handled=true;}
            } finally {for(AccessibilityNodeInfo n:nodes)if(n!=root)n.recycle();}
        } finally {root.recycle();}
    }
    private static String str(CharSequence s){return s==null?"":s.toString();}
    private void collect(AccessibilityNodeInfo n,List<AccessibilityNodeInfo> out,int depth){
        out.add(n);if(depth>=20 || out.size()>300)return;
        for(int i=0;i<n.getChildCount() && out.size()<300;i++){AccessibilityNodeInfo child=n.getChild(i);if(child!=null)collect(child,out,depth+1);}
    }
    @Override public void onInterrupt() {}
    @Override public void onDestroy(){if(store!=null)store.close();super.onDestroy();}
}
