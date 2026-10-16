package com.callguard;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/** Suppress updates to one call, never a new call just because its notification ID is reused. */
final class CallSessions {
    private final Map<String, Entry> entries = new HashMap<>();
    boolean claim(String key, long startedAt, String caller, Rules.Media media, Object action, String policy) {
        Entry next = new Entry(startedAt, caller, media, action, policy);
        if (next.equals(entries.get(key))) return false;
        if (entries.size() >= 200) entries.clear();
        entries.put(key, next);
        return true;
    }
    void remove(String key) { entries.remove(key); }
    private static final class Entry {
        final long startedAt;
        final String caller, policy;
        final Rules.Media media;
        final Object action;
        Entry(long startedAt, String caller, Rules.Media media, Object action, String policy) {
            this.startedAt=startedAt; this.caller=caller; this.media=media; this.action=action; this.policy=policy;
        }
        @Override public boolean equals(Object other) {
            if (!(other instanceof Entry)) return false;
            Entry e=(Entry)other;
            return startedAt==e.startedAt && Objects.equals(caller,e.caller) && media==e.media
                && Objects.equals(action,e.action) && Objects.equals(policy,e.policy);
        }
        @Override public int hashCode() { return Objects.hash(startedAt,caller,media,action,policy); }
    }
}
