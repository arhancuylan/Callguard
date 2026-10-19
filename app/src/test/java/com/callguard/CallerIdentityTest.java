package com.callguard;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;
public class CallerIdentityTest {
    private Rules.Contact contact(String name,String number,boolean white){return new Rules.Contact(1,name,number,white);}
    @Test public void nameOnlyResolvesNormalizedContactWithoutDuplicate(){
        List<Rules.Contact> listed=Collections.singletonList(contact("Yaşar Karahan","+905551234567",false));
        List<ContactBook.Group> book=ContactBook.group(Collections.singletonList(new String[]{"YAŞAR KARAHAN","05551234567","5","Cep"}));
        List<Rules.Contact> found=CallerIdentity.resolve("Yaşar Karahan","",listed,book);
        assertEquals(1,found.size());assertFalse(CallerIdentity.membership(found.get(0).number,listed).white);
    }
    @Test public void realNumberNeverFallsBackToDifferentPersonsName(){
        List<Rules.Contact> found=CallerIdentity.resolve("Ali","+905559999999",Collections.singletonList(contact("Ali","+905551234567",true)),Collections.emptyList());
        assertEquals(1,found.size());assertEquals("+905559999999",found.get(0).number);
    }
    @Test public void sameNamePeopleAndMultipleNumbersRequireChoice(){
        List<ContactBook.Group> book=ContactBook.group(Arrays.asList(new String[]{"Ali","05551234567","1","Cep"},new String[]{"Ali","05557654321","2","Cep"}));
        assertEquals(2,CallerIdentity.resolve("Ali","",Collections.emptyList(),book).size());
        assertTrue(CallerIdentity.resolve("Al","",Collections.emptyList(),book).isEmpty());
    }
    @Test public void unknownCallerDoesNotResolveByName(){assertTrue(CallerIdentity.resolve("Bilinmeyen numara","",Collections.singletonList(contact("Bilinmeyen numara","+905551234567",true)),Collections.emptyList()).isEmpty());}
    @Test public void numericCallerAndLocalFormattingMatchExisting(){
        List<Rules.Contact> listed=Collections.singletonList(contact("Ali","+905551234567",true));
        assertEquals("Ali",CallerIdentity.resolve("05551234567","",listed,Collections.emptyList()).get(0).name);
        assertNotNull(CallerIdentity.membership("05551234567",listed));
    }
}
