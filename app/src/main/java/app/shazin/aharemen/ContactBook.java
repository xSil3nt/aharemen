package app.shazin.aharemen;

import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract;
import java.util.*;

final class ContactBook {
    static LinkedHashMap<String,String> read(Context context) {
        LinkedHashMap<String,String> names=new LinkedHashMap<>();
        String[] columns={ContactsContract.CommonDataKinds.Phone.NUMBER, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME};
        try(Cursor cursor=context.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,columns,null,null,null)) {
            if(cursor==null)throw new IllegalStateException("Contacts could not be read.");
            while(cursor.moveToNext()) {
                String raw=cursor.getString(0),name=cursor.getString(1);
                if(raw==null)continue;
                try {
                    String phone=WalletMath.mobile(raw);
                    if(!names.containsKey(phone))names.put(phone,name==null||name.trim().isEmpty()?phone:name.trim());
                }catch(IllegalArgumentException ignored){}
            }
        }
        return names;
    }
}
