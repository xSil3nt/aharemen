package app.shazin.aharemen;

import android.content.*;
import org.json.*;
import java.math.BigDecimal;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/** The durable claim is written before POST. A lost response never triggers a replay. */
final class WidgetActions {
    static boolean appMutation;
    static String active="",posting="";
    static SharedPreferences prefs(Context c){return c.getSharedPreferences("fresh",0);}
    static JSONObject current(Context c){synchronized(SessionStore.LOCK){try{return new JSONObject(prefs(c).getString("widget_action","{}"));}catch(Exception e){return new JSONObject();}}}
    static String fingerprint(String token){try{byte[] hash=MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));StringBuilder b=new StringBuilder();for(byte value:hash)b.append(String.format(java.util.Locale.ROOT,"%02x",value&255));return b.toString();}catch(Exception e){throw new IllegalStateException(e);}}
    static boolean save(Context c,JSONObject action){return prefs(c).edit().putString("widget_action",action.toString()).commit();}
    static JSONObject enqueue(Context c,String token,int kind,int amount,BigDecimal quote)throws Exception {
        synchronized(SessionStore.LOCK){
            JSONObject old=current(c);String state=old.optString("state");
            if(appMutation||!posting.isEmpty()||QuickAdd.blocks(state)||System.currentTimeMillis()-old.optLong("ended",0)<1500)return null;
            JSONObject action=new JSONObject().put("id",UUID.randomUUID().toString()).put("session",fingerprint(token)).put("kind",kind).put("amount",amount).put("quote",quote.toPlainString()).put("at",System.currentTimeMillis()).put("state","queued").put("note","Waiting to add…");
            if(!save(c,action))throw new Exception("Couldn’t save the quick-add. Open aharemen.");
            return action;
        }
    }
    static JSONObject start(Context c){synchronized(SessionStore.LOCK){
        JSONObject action=current(c);if(!action.optString("state").equals("queued"))return null;
        if(System.currentTimeMillis()-action.optLong("at",0)>120000){finish(c,action.optString("id"),"failed","Quick-add expired. Tap to try again.");return null;}
        try{action.put("state","checking").put("note","Checking balance…");if(!save(c,action))return null;active=action.optString("id");return action;}catch(Exception e){return null;}
    }}
    static boolean claim(Context c,String id){synchronized(SessionStore.LOCK){
        JSONObject action=current(c);if(!id.equals(action.optString("id"))||!QuickAdd.mayClaim(action.optString("state"),System.currentTimeMillis()-action.optLong("at",0)))return false;
        try{action.put("state","sending").put("note","Adding allowance…");if(!save(c,action))return false;posting=id;return true;}catch(Exception e){return false;}
    }}
    static void finish(Context c,String id,String state,String note){synchronized(SessionStore.LOCK){JSONObject action=current(c);if(!id.equals(action.optString("id")))return;try{action.put("state",state).put("note",note).put("ended",System.currentTimeMillis());save(c,action);}catch(Exception ignored){}if(id.equals(active))active="";}}
    static boolean inFlight(Context c){synchronized(SessionStore.LOCK){return appMutation||!posting.isEmpty()||QuickAdd.blocks(current(c).optString("state"));}}
    static boolean beginApp(Context c){synchronized(SessionStore.LOCK){if(appMutation||!posting.isEmpty()||QuickAdd.blocks(current(c).optString("state")))return false;appMutation=true;return true;}}
    static void endApp(){synchronized(SessionStore.LOCK){appMutation=false;}}
    static boolean clear(Context c){synchronized(SessionStore.LOCK){if(!posting.isEmpty())return false;prefs(c).edit().remove("widget_action").remove("widget_refresh_error").apply();active="";return true;}}
    static void completePost(String id){synchronized(SessionStore.LOCK){if(id.equals(posting))posting="";}}
    static void recover(Context c){synchronized(SessionStore.LOCK){JSONObject action=current(c);String state=action.optString("state"),id=action.optString("id");if(state.equals("sending")&&!id.equals(active))finish(c,id,"uncertain","Check last add in aharemen");else if((state.equals("queued")||state.equals("checking"))&&System.currentTimeMillis()-action.optLong("at",0)>120000&&!id.equals(active))finish(c,id,"failed","Quick-add expired. Tap to try again.");}}
}
