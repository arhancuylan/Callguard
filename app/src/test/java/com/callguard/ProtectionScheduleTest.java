package com.callguard;
import java.time.*;
import org.junit.Test;
import static org.junit.Assert.*;
public class ProtectionScheduleTest {
    private ZonedDateTime at(String date){return ZonedDateTime.parse(date+"+03:00[Europe/Istanbul]");}
    @Test public void workWeekAndEndBoundary(){assertTrue(ProtectionSchedule.active(true,31,540,1080,at("2026-10-16T09:00:00")));assertFalse(ProtectionSchedule.active(true,31,540,1080,at("2026-10-16T18:00:00")));assertFalse(ProtectionSchedule.active(true,31,540,1080,at("2026-10-17T10:00:00")));}
    @Test public void fridayOvernightEndsOnSaturday(){assertTrue(ProtectionSchedule.active(true,16,1320,420,at("2026-10-17T06:59:00")));assertFalse(ProtectionSchedule.active(true,16,1320,420,at("2026-10-17T07:00:00")));assertFalse(ProtectionSchedule.active(true,16,1320,420,at("2026-10-18T01:00:00")));}
    @Test public void sundayOvernightWrapsWeek(){assertTrue(ProtectionSchedule.active(true,64,1320,420,at("2026-10-19T01:00:00")));}
    @Test public void allDayAndDisabled(){assertTrue(ProtectionSchedule.active(true,127,0,0,at("2026-10-18T14:00:00")));assertTrue(ProtectionSchedule.active(false,0,0,0,at("2026-10-18T14:00:00")));assertFalse(ProtectionSchedule.active(true,0,0,0,at("2026-10-18T14:00:00")));}
}
