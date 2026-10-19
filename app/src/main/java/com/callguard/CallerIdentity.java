package com.callguard;

import java.util.*;

/** Numeric identity wins; name-only notifications may resolve to several explicit choices. */
final class CallerIdentity {
    static List<Rules.Contact> resolve(String caller,String raw,List<Rules.Contact> listed,List<ContactBook.Group> book){
        String number=Rules.number(raw);if(number.isEmpty())number=Rules.number(caller);
        Map<String,Rules.Contact> matches=new LinkedHashMap<>();
        String name=Rules.label(caller);
        boolean usable=!name.isEmpty()&&!name.equals(Rules.label("Bilinmeyen numara"));
        for(Rules.Contact c:listed)if(!number.isEmpty()?number.equals(c.number):usable&&name.equals(Rules.label(c.name)))matches.put(c.number,c);
        for(ContactBook.Group g:book)for(ContactBook.Phone p:g.phones)if(!number.isEmpty()?number.equals(p.number):usable&&name.equals(g.searchName)){
            if(!matches.containsKey(p.number))matches.put(p.number,new Rules.Contact(0,g.name,p.number,true));
        }
        if(!number.isEmpty()&&matches.isEmpty())matches.put(number,new Rules.Contact(0,caller,number,true));
        return new ArrayList<>(matches.values());
    }
    static Rules.Contact membership(String number,List<Rules.Contact> listed){String normalized=Rules.number(number);for(Rules.Contact c:listed)if(!normalized.isEmpty()&&normalized.equals(c.number))return c;return null;}
    private CallerIdentity(){}
}
