package app.shazin.aharemen;
import java.util.*;

public final class ContactMatchTest {
    public static void main(String[] args){
        Map<String,String> names=new HashMap<>();names.put("7710000","One");names.put("7716176","Me");
        if(!ContactMatch.eligible(names,"+960 771-0000",21,1,"7716176"))throw new AssertionError("Registered match missing");
        if(ContactMatch.eligible(names,"7710000",0,1,"7716176"))throw new AssertionError("Invalid account shown");
        if(ContactMatch.eligible(names,"7710000",1,1,"7716176"))throw new AssertionError("Own ID shown");
        if(ContactMatch.eligible(names,"7716176",2,1,"7716176"))throw new AssertionError("Own number shown");
        if(ContactMatch.eligible(names,"7720000",21,1,"7716176"))throw new AssertionError("Unknown number shown");
        if(ContactMatch.eligible(names,"invalid",21,1,"7716176"))throw new AssertionError("Invalid number shown");
        System.out.println("6 compatible-contact filtering checks passed.");
    }
}
