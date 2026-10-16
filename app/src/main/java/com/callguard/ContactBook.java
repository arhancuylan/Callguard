package com.callguard;

import java.util.*;

/** Groups real contact IDs, never merges two people just because their names match. */
final class ContactBook {
    static final class Phone {final String number,label;Phone(String number,String label){this.number=number;this.label=label;}}
    static final class Group {
        final String id,name,searchName; final List<Phone> phones=new ArrayList<>(); String searchNumbers="";
        Group(String id,String name){this.id=id;this.name=name;this.searchName=Rules.label(name);}
        void index(){StringBuilder value=new StringBuilder();for(Phone phone:phones)value.append(phone.number).append(' ');searchNumbers=value.toString();}
    }
    static List<Group> group(List<String[]> rows){
        Map<String,Group> groups=new LinkedHashMap<>();
        for(String[] row:rows){String number=Rules.number(row[1]);if(number.isEmpty())continue;String id=row[2];Group group=groups.get(id);if(group==null){group=new Group(id,row[0]);groups.put(id,group);}boolean duplicate=false;for(Phone phone:group.phones)if(phone.number.equals(number))duplicate=true;if(!duplicate)group.phones.add(new Phone(number,row[3]==null||row[3].trim().isEmpty()?"Telefon":row[3]));}
        for(Group group:groups.values())group.index();
        return new ArrayList<>(groups.values());
    }
    private static boolean matchesIndexed(Group group,String query,String digits){return group.searchName.contains(query)||group.searchNumbers.contains(query)||(!digits.isEmpty()&&group.searchNumbers.contains(digits));}
    static boolean matches(Group group,String search){String query=Rules.label(search);return matchesIndexed(group,query,query.replaceAll("[^0-9]",""));}
    static List<Group> filter(List<Group> groups,String search){String query=Rules.label(search);String digits=query.replaceAll("[^0-9]","");ArrayList<Group> matches=new ArrayList<>();for(Group group:groups){if(Thread.currentThread().isInterrupted())break;if(matchesIndexed(group,query,digits))matches.add(group);}return matches;}
    private ContactBook(){}
}
