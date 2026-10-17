package com.callguard;

import java.util.Objects;

/** An exact notification generation, never a caller/time-window guess or an app-wide dismissal. */
final class RejectedNotification {
    final String key,owner,category;
    final long posted,when;
    final int callType;
    final boolean summary;
    final Object decline;
    RejectedNotification(String key,String owner,String category,long posted,long when,int callType,boolean summary,Object decline){
        this.key=key;this.owner=owner;this.category=category;this.posted=posted;this.when=when;
        this.callType=callType;this.summary=summary;this.decline=decline;
    }
    boolean matches(RejectedNotification current){
        return current!=null&&key!=null&&!key.isEmpty()&&posted>0&&decline!=null
            &&("com.whatsapp".equals(owner)||"com.whatsapp.w4b".equals(owner))
            &&"call".equals(category)&&!summary&&!current.summary&&(callType==0||callType==1)
            &&Objects.equals(key,current.key)&&Objects.equals(owner,current.owner)
            &&Objects.equals(category,current.category)&&posted==current.posted&&when==current.when
            &&callType==current.callType&&Objects.equals(decline,current.decline);
    }
}
