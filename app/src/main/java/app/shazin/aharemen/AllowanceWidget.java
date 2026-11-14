package app.shazin.aharemen;

import android.app.*;
import android.appwidget.*;
import android.content.*;
import android.os.Bundle;
import android.view.View;
import android.widget.RemoteViews;
import org.json.*;
import java.math.BigDecimal;
import java.text.*;
import java.util.*;
import java.util.concurrent.*;

public final class AllowanceWidget extends AppWidgetProvider {
    static final String REFRESH="app.shazin.aharemen.WIDGET_REFRESH",ADD="app.shazin.aharemen.WIDGET_ADD";
    static final ExecutorService display=Executors.newSingleThreadExecutor();
    static volatile boolean refreshing;
    static final int[] VALUES={R.id.widget_data,R.id.widget_voice,R.id.widget_sms},BUTTONS={R.id.widget_add_data,R.id.widget_add_voice,R.id.widget_add_sms},BALANCES={R.id.widget_data_balance,R.id.widget_voice_balance,R.id.widget_sms_balance};
    static final int[] UNIT_LABELS={R.id.widget_unit_data,R.id.widget_unit_voice,R.id.widget_unit_sms};
    static final String[] UNITS={"MB","min","SMS"};
    @Override public void onUpdate(Context c,AppWidgetManager manager,int[] ids){updateAll(c);WidgetJob.schedule(c,false);}
    @Override public void onAppWidgetOptionsChanged(Context c,AppWidgetManager manager,int id,Bundle options){updateAll(c);}
    @Override public void onDisabled(Context c){WidgetJob.cancelRefresh(c);}
    @Override public void onReceive(Context c,Intent intent){
        super.onReceive(c,intent);String action=intent.getAction();if(!REFRESH.equals(action)&&!ADD.equals(action))return;
        PendingResult pending=goAsync();display.execute(()->{try{
            WidgetActions.recover(c);
            if(ADD.equals(action))add(c,intent);else WidgetJob.schedule(c,false);
            updateNow(c);
        }finally{pending.finish();}});
    }
    static PendingIntent open(Context c){return PendingIntent.getActivity(c,800,new Intent(c,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP).putExtra("widget",true),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);}
    static PendingIntent broadcast(Context c,int code,Intent intent){return PendingIntent.getBroadcast(c,code,intent,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);}
    static void add(Context c,Intent intent){try{
        SessionStore sessions=new SessionStore(WidgetActions.prefs(c));String token=sessions.load();if(token.isEmpty())return;
        if(!WidgetActions.fingerprint(token).equals(intent.getStringExtra("session")))return;
        int kind=intent.getIntExtra("kind",-1),amount=intent.getIntExtra("amount",0);BigDecimal quote=new BigDecimal(intent.getStringExtra("quote"));
        JSONObject snapshot=new JSONObject(sessions.loadSnapshot());
        QuickAdd.validate(kind,amount,quote,AccountSnapshot.rates(snapshot),snapshot.getJSONObject("user").optDouble("rb_balance",0));
        JSONObject action=WidgetActions.enqueue(c,token,kind,amount,quote);
        if(action!=null&&!WidgetJob.schedule(c,true))WidgetActions.finish(c,action.optString("id"),"failed","Couldn’t start. Tap to try again.");
    }catch(Exception e){WidgetActions.prefs(c).edit().putString("widget_refresh_error",e.getMessage()==null?"Open aharemen to refresh.":e.getMessage()).apply();}}
    static String savedTime(long at){
        Calendar saved=Calendar.getInstance(),now=Calendar.getInstance();saved.setTimeInMillis(at);
        boolean today=saved.get(Calendar.ERA)==now.get(Calendar.ERA)&&saved.get(Calendar.YEAR)==now.get(Calendar.YEAR)&&saved.get(Calendar.DAY_OF_YEAR)==now.get(Calendar.DAY_OF_YEAR);
        return today?DateFormat.getTimeInstance(DateFormat.SHORT).format(new Date(at)):DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT).format(new Date(at));
    }
    static void updateAll(Context context){Context c=context.getApplicationContext();display.execute(()->updateNow(c));}
    static void updateNow(Context c){
        AppWidgetManager manager=AppWidgetManager.getInstance(c);int[] ids=manager.getAppWidgetIds(new ComponentName(c,AllowanceWidget.class));if(ids.length==0)return;
        WidgetActions.recover(c);JSONObject action=WidgetActions.current(c);String state=action.optString("state");boolean blocked=WidgetActions.inFlight(c);
        JSONObject snapshot=null;String token="";
        try{SessionStore sessions=new SessionStore(WidgetActions.prefs(c));token=sessions.load();if(!token.isEmpty())snapshot=new JSONObject(sessions.loadSnapshot());}catch(Exception ignored){}
        for(int id:ids){
            RemoteViews views=new RemoteViews(c.getPackageName(),R.layout.allowance_widget);PendingIntent open=open(c);
            views.setOnClickPendingIntent(R.id.widget_title,open);views.setOnClickPendingIntent(R.id.widget_status,open);
            views.setOnClickPendingIntent(R.id.widget_refresh,broadcast(c,801,new Intent(c,AllowanceWidget.class).setAction(REFRESH)));
            double[] rates=snapshot==null?new double[]{0,0,0}:AccountSnapshot.rates(snapshot);
            for(int kind=0;kind<3;kind++){
                views.setOnClickPendingIntent(BALANCES[kind],open);
                String text="—",unit=UNITS[kind];if(snapshot!=null){double value=AccountSnapshot.allowance(snapshot,kind);if(kind==0&&value>=1024){value/=1024;unit="GB";}text=new DecimalFormat("#,##0.##").format(value);}
                views.setTextViewText(UNIT_LABELS[kind],unit);
                views.setTextViewText(VALUES[kind],text);int amount=0;BigDecimal cost=BigDecimal.ZERO;
                try{amount=QuickAdd.units(kind,rates);int[] selected={0,0,0};selected[kind]=amount;cost=WalletMath.cost(selected,rates);}catch(Exception ignored){}
                boolean available=snapshot!=null&&amount>0&&!blocked;
                views.setTextViewText(BUTTONS[kind],amount>0?"+"+amount+" "+UNITS[kind]:"Open app");
                views.setBoolean(BUTTONS[kind],"setEnabled",!blocked||state.equals("uncertain"));
                views.setTextColor(BUTTONS[kind],blocked?0xff64756b:kind==0?0xffa8ccea:kind==1?0xffedc3a2:0xffb4e7c7);
                if(available){Intent add=new Intent(c,AllowanceWidget.class).setAction(ADD).putExtra("kind",kind).putExtra("amount",amount).putExtra("quote",cost.toPlainString()).putExtra("session",WidgetActions.fingerprint(token));views.setOnClickPendingIntent(BUTTONS[kind],broadcast(c,1000+id*3+kind,add));views.setContentDescription(BUTTONS[kind],"Add "+amount+" "+UNITS[kind]+" for "+cost.stripTrailingZeros().toPlainString()+" RB");}
                else views.setOnClickPendingIntent(BUTTONS[kind],open);
            }
            String note=token.isEmpty()?"Open aharemen to sign in":snapshot==null?"Open aharemen to load allowances":savedTime(snapshot.optLong("at",0));
            String error=WidgetActions.prefs(c).getString("widget_refresh_error","");if(!error.isEmpty()&&!token.isEmpty())note=error;
            if(refreshing&&!token.isEmpty())note="Refreshing allowances…";
            if(!action.optString("note").isEmpty()&&(blocked||System.currentTimeMillis()-action.optLong("ended",0)<30000))note=action.optString("note");
            views.setTextViewText(R.id.widget_status,note);
            views.setViewVisibility(R.id.widget_progress,refreshing||state.equals("checking")||state.equals("sending")||(blocked&&state.equals("confirmed"))?View.VISIBLE:View.GONE);
            manager.updateAppWidget(id,views);
        }
    }
}
