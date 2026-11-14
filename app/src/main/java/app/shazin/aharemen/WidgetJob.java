package app.shazin.aharemen;

import android.app.job.*;
import android.content.*;
import android.os.*;
import org.json.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;

public final class WidgetJob extends JobService {
    static final int REFRESH_JOB=5101,ADD_JOB=5102;
    final ExecutorService workers=Executors.newFixedThreadPool(2),reads=Executors.newFixedThreadPool(4);
    final Handler main=new Handler(Looper.getMainLooper());
    final Map<Integer,Future<?>> tasks=new HashMap<>();
    static boolean schedule(Context c,boolean add){
        JobScheduler scheduler=c.getSystemService(JobScheduler.class);int id=add?ADD_JOB:REFRESH_JOB;
        if(scheduler.getPendingJob(id)!=null)return true;
        JobInfo.Builder builder=new JobInfo.Builder(id,new ComponentName(c,WidgetJob.class)).setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY);
        if(Build.VERSION.SDK_INT>=31&&add)builder.setExpedited(true);
        try{if(scheduler.schedule(builder.build())==JobScheduler.RESULT_SUCCESS)return true;
            if(Build.VERSION.SDK_INT>=31&&add)return scheduler.schedule(builder.setExpedited(false).build())==JobScheduler.RESULT_SUCCESS;
        }catch(Exception ignored){}
        return false;
    }
    static void cancelRefresh(Context c){c.getSystemService(JobScheduler.class).cancel(REFRESH_JOB);}
    @Override public boolean onStartJob(JobParameters parameters){
        final boolean add=parameters.getJobId()==ADD_JOB;final JSONObject action=add?WidgetActions.start(this):null;
        if(add&&action==null){AllowanceWidget.updateAll(this);return false;}
        if(!add)AllowanceWidget.refreshing=true;AllowanceWidget.updateAll(this);
        tasks.put(parameters.getJobId(),workers.submit(()->{
            if(add)performAdd(action);else refresh();
            main.post(()->{tasks.remove(parameters.getJobId());if(!add)AllowanceWidget.refreshing=false;AllowanceWidget.updateAll(this);jobFinished(parameters,false);});
        }));return true;
    }
    private void refresh(){try{
        SessionStore sessions=new SessionStore(WidgetActions.prefs(this));String token=sessions.load();if(token.isEmpty())return;
        JSONObject snapshot=AccountSnapshot.fetch(token,reads);if(sessions.saveSnapshotIfCurrent(token,snapshot.toString()))WidgetActions.prefs(this).edit().remove("widget_refresh_error").apply();
    }catch(Exception e){WidgetActions.prefs(this).edit().putString("widget_refresh_error","Couldn’t refresh · tap ↻ to retry").apply();}}
    private void performAdd(JSONObject action){
        String id=action.optString("id");boolean submitted=false,confirmed=false;
        try{
            SessionStore sessions=new SessionStore(WidgetActions.prefs(this));String token=sessions.load();
            if(token.isEmpty()||!WidgetActions.fingerprint(token).equals(action.optString("session")))throw new Exception("Sign in to aharemen again.");
            JSONObject snapshot=AccountSnapshot.fetch(token,reads);
            if(!sessions.saveSnapshotIfCurrent(token,snapshot.toString()))throw new Exception("Your account changed. Open aharemen.");
            WidgetActions.prefs(this).edit().remove("widget_refresh_error").apply();
            int kind=action.getInt("kind"),amount=action.getInt("amount");
            QuickAdd.validate(kind,amount,new BigDecimal(action.getString("quote")),AccountSnapshot.rates(snapshot),snapshot.getJSONObject("user").optDouble("rb_balance",0));
            synchronized(SessionStore.LOCK){
                if(Thread.currentThread().isInterrupted()||!token.equals(sessions.load())||!WidgetActions.claim(this,id))throw new Exception("Quick-add expired. Tap to try again.");
            }
            AllowanceWidget.updateAll(this);
            int[] units={0,0,0};units[kind]=amount;String data=new JSONObject().put("data",units[0]).put("voice",units[1]).put("sms",units[2]).toString();
            submitted=true;JSONObject result=ApiClient.call(token,"wallet/build","POST",data,false);
            if(!result.optBoolean("success",false))throw new Exception("Dhiraagu did not confirm the add.");
            confirmed=true;WidgetActions.finish(this,id,"confirmed","Added +"+amount+" "+AllowanceWidget.UNITS[kind]+" · updating…");AllowanceWidget.updateAll(this);
            JSONObject latest=AccountSnapshot.fetch(token,reads);sessions.saveSnapshotIfCurrent(token,latest.toString());
            WidgetActions.finish(this,id,"confirmed","Added +"+amount+" "+AllowanceWidget.UNITS[kind]);
        }catch(Exception e){
            String state=confirmed?"confirmed":submitted?"uncertain":"failed";
            String note=confirmed?"Added · tap ↻ to update balances":submitted?"Check last add in aharemen":e.getMessage()==null?"Couldn’t add. Open aharemen.":e.getMessage();
            WidgetActions.finish(this,id,state,note);
        }finally{WidgetActions.completePost(id);}
    }
    @Override public boolean onStopJob(JobParameters parameters){
        if(parameters.getJobId()==ADD_JOB){JSONObject action=WidgetActions.current(this);String state=action.optString("state");if(state.equals("sending"))WidgetActions.finish(this,action.optString("id"),"uncertain","Check last add in aharemen");else if(state.equals("checking")||state.equals("queued"))WidgetActions.finish(this,action.optString("id"),"failed","Quick-add stopped. Tap to try again.");}
        else AllowanceWidget.refreshing=false;
        Future<?> task=tasks.remove(parameters.getJobId());if(task!=null)task.cancel(true);AllowanceWidget.updateAll(this);return false;
    }
    @Override public void onDestroy(){workers.shutdownNow();reads.shutdownNow();main.removeCallbacksAndMessages(null);super.onDestroy();}
}
