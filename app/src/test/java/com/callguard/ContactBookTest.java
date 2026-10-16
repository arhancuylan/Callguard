package com.callguard;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;
public class ContactBookTest {
    private List<String[]> rows(){return Arrays.asList(new String[]{"Ayşe","05551112233","10","Cep"},new String[]{"Ayşe","+905551112244","10","İş"},new String[]{"Ayşe","+905551112255","20","Ev"});}
    @Test public void numbersAreGroupedByContactIdNotDisplayName(){List<ContactBook.Group> groups=ContactBook.group(rows());assertEquals(2,groups.size());assertEquals(2,groups.get(0).phones.size());assertEquals(1,groups.get(1).phones.size());assertEquals("İş",groups.get(0).phones.get(1).label);}
    @Test public void duplicateNumberInSameContactAppearsOnce(){List<String[]> rows=new ArrayList<>(rows());rows.add(new String[]{"Ayşe","+905551112233","10","Cep"});assertEquals(2,ContactBook.group(rows).get(0).phones.size());}
    @Test public void searchFindsAnyNumberAndTurkishName(){ContactBook.Group group=ContactBook.group(rows()).get(0);assertTrue(ContactBook.matches(group,"AYŞE"));assertTrue(ContactBook.matches(group,"555 111 22 44"));assertFalse(ContactBook.matches(group,"905551112255"));}
    @Test public void invalidNumbersDoNotCreateEmptyContacts(){assertTrue(ContactBook.group(Collections.singletonList(new String[]{"Empty","private","30","Cep"})).isEmpty());}
}
