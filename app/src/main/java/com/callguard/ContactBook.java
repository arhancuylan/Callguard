package com.callguard;

import java.util.*;

/** Groups real contact IDs, never merges two people just because their names match. */
final class ContactBook {
    static final class Phone {final String number,label;Phone(String number,String label){this.number=number;this.label=label;}}
    static final class Group {final String id,name;final List<Phone> phones=new ArrayList<>();Group(String id,String name){this.id=id;this.name=name;}}
    static List<Group> group(List<String[]> rows){
        Map<String,Group> groups=new LinkedHashMap<>();
        for(String[] row:rows){String number=Rules.number(row[1]);if(number.isEmpty())continue;String id=row[2];Group group=groups.get(id);if(group==null){group=new Group(id,row[0]);groups.put(id,group);}boolean duplicate=false;for(Phone phone:group.phones)if(phone.number.equals(number))duplicate=true;if(!duplicate)group.phones.add(new Phone(number,row[3]==null||row[3].trim().isEmpty()?"Telefon":row[3]));}
        return new ArrayList<>(groups.values());
    }
    static boolean matches(Group group,String search){String query=Rules.label(search);if(Rules.label(group.name).contains(query))return true;String digits=query.replaceAll("[^0-9]","");for(Phone phone:group.phones)if(phone.number.contains(query)||(!digits.isEmpty()&&phone.number.contains(digits)))return true;return false;}
    private ContactBook(){}
}
