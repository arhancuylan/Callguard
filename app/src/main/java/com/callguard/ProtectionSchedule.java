package com.callguard;

import java.time.*;

/** End is exclusive; an overnight interval belongs to the day on which it starts. */
public final class ProtectionSchedule {
    public static boolean active(boolean enabled,int days,int start,int end,ZonedDateTime now){
        if(!enabled)return true;
        if(days<1||days>127||start<0||start>1439||end<0||end>1439)return false;
        int minute=now.getHour()*60+now.getMinute();int day=now.getDayOfWeek().getValue()-1;
        if(start==end)return (days&(1<<day))!=0;
        if(start<end)return minute>=start&&minute<end&&(days&(1<<day))!=0;
        if(minute>=start)return (days&(1<<day))!=0;
        return minute<end&&(days&(1<<((day+6)%7)))!=0;
    }
    private ProtectionSchedule(){}
}
