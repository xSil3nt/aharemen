package app.shazin.aharemen;

import org.json.*;
import java.util.*;
import java.util.concurrent.ExecutorService;

final class AccountSnapshot {
    static JSONObject fetch(String token,ExecutorService reads)throws Exception {
        List<JSONObject> values=ParallelCalls.run(reads,Arrays.asList(
            ()->ApiClient.call(token,"user","GET",null,false),
            ()->ApiClient.call(token,"usages","GET",null,false),
            ()->ApiClient.call(token,"rates","GET",null,false),
            ()->ApiClient.call(token,"wallet","GET",null,false)));
        JSONObject user=values.get(0),usage=values.get(1),rates=values.get(2),wallet=values.get(3);
        if(wallet.optJSONObject("user")!=null)user=wallet.getJSONObject("user");
        JSONObject base=wallet.optJSONObject("usage"),all=usage.optJSONObject("data");
        if(all==null)throw new Exception("The server returned no allowances.");
        if(base!=null)for(String key:new String[]{"data","voice","sms"})if(base.optJSONObject(key)!=null)all.put(key,base.getJSONObject(key));
        return new JSONObject().put("user",user).put("usage",usage).put("rates",rates).put("wallet",wallet).put("at",System.currentTimeMillis());
    }
    static double[] rates(JSONObject snapshot){JSONObject r=snapshot.optJSONObject("rates");return r==null?new double[]{0,0,0}:new double[]{r.optDouble("mb",0),r.optDouble("min",0),r.optDouble("sms",0)};}
    static double allowance(JSONObject snapshot,int kind){
        JSONObject usage=snapshot.optJSONObject("usage"),plans=usage==null?null:usage.optJSONObject("data");if(plans==null)return 0;
        String key=new String[]{"data","voice","sms"}[kind];JSONObject plan=plans.optJSONObject(key);double raw=plan==null?0:plan.optDouble("available",0);
        if(kind==0){JSONObject bonus=plans.optJSONObject("bonus_data");if(bonus!=null)raw+=bonus.optDouble("available",0);JSONArray addons=plans.optJSONArray("addons");if(addons!=null)for(int i=0;i<addons.length();i++){JSONObject addon=addons.optJSONObject(i);if(addon!=null)raw+=addon.optDouble("available",0);}}
        return Math.max(0,raw)/(kind==0?1024:kind==1?60:1);
    }
}
