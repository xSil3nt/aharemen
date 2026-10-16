package app.shazin.aharemen;

import android.app.*;
import android.os.*;
import android.content.*;
import android.content.res.ColorStateList;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;

import android.text.*;
import android.text.style.RelativeSizeSpan;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import org.json.*;
import java.math.*;
import java.text.*;
import java.util.*;
import java.util.concurrent.*;

public class MainActivity extends Activity {
    static final String[] KEYS={"data","voice","sms"}, NAMES={"Data","Calls","Texts"}, UNITS={"MB","min","SMS"};
    final ExecutorService worker=Executors.newSingleThreadExecutor();
    final Handler handler=new Handler(Looper.getMainLooper());
    SharedPreferences prefs; SessionStore sessions; Ui ui;
    LinearLayout root,body; TextView status,quote,after,review;
    String token="",mobile="",screen="Home",transferMode="send";
    boolean demo=false,busy=false,loaded=false,take=false,sliderUpdate=false;
    JSONObject user=new JSONObject(),usage=new JSONObject(),rates=new JSONObject(),wallet=new JSONObject();
    JSONArray history=new JSONArray(); String nextHistory=""; boolean historyLoaded=false;
    final int[] chosen={0,0,0}; final SeekBar[] sliders=new SeekBar[3]; final EditText[] fields=new EditText[3]; final int[] sliderLimits=new int[3]; int draggingSlider=-1;
    EditText recipient,transferAmount,bmlAmount; String recipientDraft="",rbDraft="10",bmlAmountDraft="";
    boolean contactsLoading=false,contactsLoaded=false;
    String contactsError="",contactSearch="";
    final ArrayList<JSONObject> compatibleContacts=new ArrayList<>();
    LinearLayout contactResults;
    ScrollView contactScroll;  String startupFailure="";
    interface Result {void done(JSONObject result) throws Exception;}

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);prefs=getSharedPreferences("fresh",0);prefs.edit().remove("light").apply();sessions=new SessionStore(prefs);
        try {token=sessions.load();}catch(Exception ignored){}
        mobile=prefs.getString("mobile","");
        if(state!=null){screen=state.getString("screen","Home");take=state.getBoolean("take");transferMode=state.getString("transferMode","send");contactSearch=state.getString("contactSearch","");recipientDraft=state.getString("recipientDraft","");rbDraft=state.getString("rbDraft","10");bmlAmountDraft=state.getString("bmlAmountDraft","");int[] saved=state.getIntArray("chosen");if(saved!=null&&saved.length==3)System.arraycopy(saved,0,chosen,0,3);if(state.getBoolean("demo")){demo=true;seed();}}
        render();if(!token.isEmpty())refresh(null);
    }
    void render() {
        ui=new Ui(this);
        getWindow().setStatusBarColor(ui.bg);getWindow().setNavigationBarColor(ui.bg);
        getWindow().getDecorView().setSystemUiVisibility(0);
        if(Build.VERSION.SDK_INT>=30&&getWindow().getInsetsController()!=null){int flags=WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS|WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;getWindow().getInsetsController().setSystemBarsAppearance(0,flags);}
        root=ui.col();root.setBackgroundColor(ui.bg);
        root.setOnApplyWindowInsetsListener((v,i)->{v.setPadding(i.getSystemWindowInsetLeft(),i.getSystemWindowInsetTop(),i.getSystemWindowInsetRight(),i.getSystemWindowInsetBottom());return i.consumeSystemWindowInsets();});
        setContentView(root);root.requestApplyInsets();
        LinearLayout bar=ui.row();ui.pad(bar,22,10);
        TextView brand=ui.medium("aharemen",24,ui.ink);brand.setLetterSpacing(-.04f);bar.addView(brand,new LinearLayout.LayoutParams(0,-2,1));
        if(demo)bar.addView(ui.text("DEMO",11,ui.mint));else if(loaded)bar.addView(ui.button("Refresh",false,()->refresh(null)));
        root.addView(bar);
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        body=ui.col();body.setPadding(ui.d(22),ui.d(8),ui.d(22),ui.d(24));scroll.addView(body);
        status=ui.text(busy?"Updating balances…":"",13,ui.peach);status.setVisibility(busy?View.VISIBLE:View.GONE);body.addView(status);
        if(token.isEmpty()&&!demo){login();return;}
        if(!loaded&&!demo){startupLoading();return;}
        switch(screen){case "Build":walletScreen();break;case "Activity":activityScreen();break;case "Settings":settings();break;case "Top up":topup();break;case "Transfer":transfer();break;default:home();}
        navigation();
    }
    void startupLoading(){
        body.removeAllViews();body.setGravity(Gravity.CENTER);
        if(startupFailure.isEmpty()){
            ProgressBar loader=new ProgressBar(this);LinearLayout.LayoutParams p=ui.lp(48,48);p.gravity=Gravity.CENTER_HORIZONTAL;body.addView(loader,p);
        }else{
            TextView title=ui.medium("Couldn’t load your account",20,ui.ink);title.setGravity(Gravity.CENTER);body.addView(title);ui.space(body,12);TextView detail=ui.text(startupFailure,13,ui.muted);detail.setGravity(Gravity.CENTER);body.addView(detail);ui.space(body,24);button("Try again",true,()->{startupFailure="";render();refresh(null);});
        }
    }
    void navigation() {
        LinearLayout nav=ui.row();nav.setPadding(ui.d(12),ui.d(6),ui.d(12),ui.d(8));
        String[] labels={"Home","Build","Activity","Settings"},icons={"home","wallet","activity","account"};
        for(int i=0;i<labels.length;i++){
            String name=labels[i];boolean active=screen.equals(name)||(name.equals("Activity")&&screen.equals("Transfer"));
            LinearLayout item=ui.col();item.setGravity(Gravity.CENTER);ui.pad(item,4,7);item.setMinimumHeight(ui.d(54));
            item.addView(ui.icon(icons[i],active?ui.mint:ui.muted),ui.lp(22,22));ui.space(item,5);TextView caption=ui.medium(name,10,active?ui.mint:ui.muted);caption.setGravity(Gravity.CENTER);item.addView(caption);
            item.setBackground(ui.shape(active?ui.panel:ui.bg,16));item.setContentDescription(name);item.setFocusable(true);
            item.setOnClickListener(v->{if(busy)return;hideKeyboard();screen=name;render();if(name.equals("Activity")&&!historyLoaded)loadHistory(false);});nav.addView(item,new LinearLayout.LayoutParams(0,-2,1));
        }root.addView(nav);
    }
    void title(String name,String subtitle){ui.space(body,8);body.addView(ui.medium(name,27,ui.ink));if(!subtitle.isEmpty()){ui.space(body,8);body.addView(ui.text(subtitle,13,ui.muted));}ui.space(body,22);}
    void section(String name){ui.space(body,24);body.addView(ui.medium(name,17,ui.ink));ui.space(body,12);}
    void button(String name,boolean primary,Runnable action){body.addView(ui.button(name,primary,()->{if(!busy)action.run();}),ui.lp(-1,-2));ui.space(body,10);}
    TextView amount(String value,String unit,int size,int color){SpannableString s=new SpannableString(value+" "+unit);s.setSpan(new RelativeSizeSpan(.43f),value.length(),s.length(),0);TextView t=ui.medium("",size,color);t.setText(s);return t;}
    EditText input(LinearLayout parent,String hint,int type,String value){EditText e=new EditText(this);e.setSingleLine(true);e.setTextSize(16);e.setTextColor(ui.ink);e.setHintTextColor(ui.muted);e.setHint(hint);e.setInputType(type);e.setText(value);e.setPadding(ui.d(16),ui.d(12),ui.d(16),ui.d(12));e.setBackground(ui.outline(ui.panel,14));parent.addView(e,ui.lp(-1,54));return e;}
    void label(String value){body.addView(ui.text(value,13,ui.muted));ui.space(body,9);}
    void hideKeyboard(){View v=getCurrentFocus();if(v!=null){((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(v.getWindowToken(),0);v.clearFocus();}}
    void notice(String message){if(isDestroyed())return;status.setText(message==null?"Something went wrong. Try again.":message);status.setVisibility(View.VISIBLE);}


    String number(double n){return new DecimalFormat("#,##0.##").format(n);}
    String decimal(BigDecimal n){return number(n.doubleValue());}
    JSONObject json(Object... pairs){JSONObject j=new JSONObject();try{for(int i=0;i<pairs.length;i+=2)j.put(pairs[i].toString(),pairs[i+1]);}catch(Exception e){throw new IllegalArgumentException(e);}return j;}

    void home(){
        ui.space(body,7);LinearLayout intro=ui.row();intro.addView(ui.text(demo?"Your preview":user.optString("name","Your account"),14,ui.muted),new LinearLayout.LayoutParams(0,-2,1));intro.addView(ui.text(demo?"Sample balances":mobile,12,ui.muted));body.addView(intro);ui.space(body,18);
        LinearLayout balance=ui.col();ui.pad(balance,22,20);balance.setBackground(ui.shape(ui.mint,24));
        balance.addView(amount(loaded?number(user.optDouble("rb_balance",0)):"—","RB",46,ui.bg));ui.space(balance,18);
        LinearLayout actions=ui.row();TextView build=ui.button("Build",false,()->{if(!busy)openWallet(false);});build.setTextColor(ui.bg);build.setBackground(ui.shape(0x19000000,12));actions.addView(build,new LinearLayout.LayoutParams(0,ui.d(44),1));
        TextView top=ui.medium("Top up  +",13,ui.bg);top.setGravity(Gravity.CENTER);top.setMinimumWidth(ui.d(100));top.setMinHeight(ui.d(44));top.setOnClickListener(v->{if(!busy){screen="Top up";render();}});actions.addView(top);balance.addView(actions);body.addView(balance);
        ui.space(body,18);allowanceRow("data","Mobile data",ui.blue);allowanceRow("voice","Calls",ui.peach);allowanceRow("sms","Texts",ui.mint);
        ui.space(body,20);LinearLayout quick=ui.row();quickAction(quick,"wallet","Take back",()->openWallet(true));body.addView(quick);

    }
    JSONObject usagePlan(String key,boolean refundable){JSONObject data=refundable?wallet.optJSONObject("usage"):usage.optJSONObject("data");return data==null?null:data.optJSONObject(key);}
    double rawData(JSONObject data,String field){if(data==null)return 0;JSONObject base=data.optJSONObject("data"),bonus=data.optJSONObject("bonus_data");double sum=base==null?0:base.optDouble(field,0);if(bonus!=null)sum+=bonus.optDouble(field,0);JSONArray addons=data.optJSONArray("addons");if(addons!=null)for(int i=0;i<addons.length();i++){JSONObject a=addons.optJSONObject(i);if(a!=null)sum+=a.optDouble(field,0);}return sum;}
    void allowanceRow(String key,String name,int color){
        JSONObject plan=usagePlan(key,false);double raw=plan==null?0:plan.optDouble("available",0),total=plan==null?0:plan.optDouble("total",0);
        if(key.equals("data")){raw=rawData(usage.optJSONObject("data"),"available");total=rawData(usage.optJSONObject("data"),"total");}
        double value=key.equals("data")?Allowance.data((long)raw):key.equals("voice")?Allowance.voice((long)raw):raw;
        String unit=key.equals("data")?(value>=1024?"GB":"MB"):key.equals("voice")?"min":"SMS";if(key.equals("data")&&value>=1024)value/=1024;
        LinearLayout row=ui.row();row.setPadding(0,ui.d(14),0,ui.d(14));LinearLayout badge=ui.col();badge.setGravity(Gravity.CENTER);badge.setBackground(ui.shape(ui.panel,13));badge.addView(ui.icon(key,color),ui.lp(21,21));row.addView(badge,ui.lp(42,42));
        LinearLayout center=ui.col();center.setPadding(ui.d(12),0,ui.d(8),0);center.addView(ui.medium(name,14,ui.ink));ui.space(center,6);
        center.addView(ui.text(plan==null?"Loading…":key.equals("voice")?((long)raw/60)+"m "+((long)raw%60)+"s remaining":expiryText(plan),10,ui.muted));row.addView(center,new LinearLayout.LayoutParams(0,-2,1));row.addView(amount(plan==null?"—":number(value),unit,23,color));body.addView(row);
        ProgressBar progress=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);progress.setMax(1000);progress.setProgress(total>0?(int)Math.min(1000,Math.max(0,raw/total*1000)):0);progress.setProgressTintList(ColorStateList.valueOf(color));progress.setProgressBackgroundTintList(ColorStateList.valueOf(ui.raised));body.addView(progress,ui.lp(-1,3));row.setOnClickListener(v->{if(!busy)allowanceDetails(key,name);});
    }
    void allowanceDetails(String key,String name){JSONObject p=usagePlan(key,false);if(p==null)return;LinearLayout c=sheet(name,"",key);pair(c,"Build allowance",precisePlan(p,key));pair(c,"Expiry",expiryText(p));if(key.equals("data")){JSONObject data=usage.optJSONObject("data"),bonus=data==null?null:data.optJSONObject("bonus_data");if(bonus!=null)pair(c,"Bonus data",precisePlan(bonus,"data"));JSONArray addons=data==null?null:data.optJSONArray("addons");if(addons!=null)for(int i=0;i<addons.length();i++){JSONObject a=addons.optJSONObject(i);if(a!=null)pair(c,a.optString("planName","Add-on"),precisePlan(a,"data"));}}showSheet(c,"Done",null);}
    String precisePlan(JSONObject p,String key){double n=p.optDouble("available",0);return number(key.equals("data")?n/1024:key.equals("voice")?n/60:n)+" "+(key.equals("data")?"MB":key.equals("voice")?"min":"SMS");}
    String expiryText(JSONObject p){String s=p.optString("expiryDateTime","");if(s.isEmpty()||s.equals("null"))return "No expiry provided";try{return "Until "+DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date.from(java.time.OffsetDateTime.parse(s).toInstant()));}catch(Exception e){return "Expiry unavailable";}}
    void quickAction(LinearLayout row,String icon,String name,Runnable action){LinearLayout item=ui.col();item.setGravity(Gravity.CENTER);ui.pad(item,4,12);item.addView(ui.icon(icon,ui.ink),ui.lp(22,22));ui.space(item,9);TextView caption=ui.medium(name,12,ui.ink);caption.setGravity(Gravity.CENTER);item.addView(caption);item.setMinimumHeight(ui.d(72));item.setBackground(ui.shape(ui.panel,16));item.setFocusable(true);item.setOnClickListener(v->{if(!busy)action.run();});LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,1);p.setMargins(ui.d(3),0,ui.d(3),0);row.addView(item,p);}

    void openWallet(boolean reverse){take=reverse;Arrays.fill(chosen,0);screen="Build";hideKeyboard();render();}

    void walletScreen(){title("Build","");body.addView(ui.text("Allowance builds are coming soon.",14,ui.muted));}







    void openTransfer(String mode){transferMode=mode;recipientDraft="";rbDraft="10";contactSearch="";screen="Transfer";render();}
    void transfer(){screen="Home";render();}







    void activityScreen(){title("Activity","");body.addView(ui.text("No activity",14,ui.muted));}




    void loadHistory(boolean append){}


    void topup(){title("Top up","");body.addView(ui.text("Top-up support is coming soon.",14,ui.muted));}

    void settings(){title(user.optString("name","Account"),"");setting("Mobile number",mobile);setting("Email",user.optString("email","—"));button(demo?"Leave demo":"Sign out",false,()->{if(demo){signout();return;}LinearLayout c=sheet("Sign out?","","account");showSheet(c,"Sign out",()->signout());});}
    void setting(String name,String value){body.addView(ui.text(name,12,ui.muted));ui.space(body,8);body.addView(ui.medium(value,16,ui.ink));ui.space(body,18);ui.divider(body);ui.space(body,18);}
    void signout(){compatibleContacts.clear();contactsLoaded=false;contactsLoading=false;contactsError="";sessions.clear();token="";demo=false;loaded=false;user=new JSONObject();usage=new JSONObject();rates=new JSONObject();wallet=new JSONObject();history=new JSONArray();historyLoaded=false;mobile=prefs.getString("mobile","");screen="Home";render();}

    LinearLayout sheet(String title,String subtitle,String icon){LinearLayout c=ui.col();c.setPadding(ui.d(24),ui.d(12),ui.d(24),ui.d(24));c.setBackground(ui.shape(ui.panel,28));View handle=new View(this);handle.setBackground(ui.shape(ui.line,4));LinearLayout.LayoutParams h=ui.lp(34,4);h.gravity=Gravity.CENTER_HORIZONTAL;c.addView(handle,h);ui.space(c,20);LinearLayout badge=ui.col();badge.setGravity(Gravity.CENTER);badge.setBackground(ui.shape(ui.raised,16));badge.addView(ui.icon(icon,ui.mint),ui.lp(25,25));c.addView(badge,ui.lp(50,50));ui.space(c,16);c.addView(ui.medium(title,24,ui.ink));if(!subtitle.isEmpty()){ui.space(c,8);c.addView(ui.text(subtitle,13,ui.muted));}ui.space(c,18);return c;}
    void pair(LinearLayout c,String name,String value){LinearLayout row=ui.row();ui.pad(row,0,9);row.addView(ui.text(name,13,ui.muted),new LinearLayout.LayoutParams(0,-2,1));TextView v=ui.medium(value,15,ui.ink);v.setGravity(Gravity.END);row.addView(v,new LinearLayout.LayoutParams(0,-2,1));c.addView(row);}
    Dialog showSheet(LinearLayout c,String action,Runnable confirmed){hideKeyboard();Dialog dialog=new Dialog(this);dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);ScrollView scroll=new ScrollView(this);scroll.addView(c);dialog.setContentView(scroll);Window window=dialog.getWindow();if(window!=null){window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);WindowManager.LayoutParams p=window.getAttributes();p.dimAmount=.65f;window.setAttributes(p);window.setGravity(Gravity.BOTTOM);window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);}ui.space(c,18);if(confirmed!=null){c.addView(ui.button("Cancel",false,()->dialog.dismiss()),ui.lp(-1,-2));ui.space(c,8);}c.addView(ui.button(action,true,()->{if(confirmed!=null&&busy)return;dialog.dismiss();if(confirmed!=null)confirmed.run();}),ui.lp(-1,-2));dialog.show();if(window!=null)window.setLayout(-1,-2);return dialog;}
    void success(String title,String message){showSheet(sheet(title,message,"check"),"Done",null);}

    void login(){ui.space(body,24);body.addView(ui.icon("wallet",ui.mint),ui.lp(44,44));ui.space(body,24);title("Sign in","");label("Dhiraagu mobile number");EditText phone=input(body,"",InputType.TYPE_CLASS_PHONE,mobile);ui.space(body,20);button("Send SMS code",true,()->{try{mobile=WalletMath.mobile(phone.getText().toString());}catch(Exception e){phone.setError(e.getMessage());return;}hideKeyboard();request("user/login","POST",ApiClient.form("mobile",mobile),true,j->{if(!j.optBoolean("success",false))throw new Exception("Could not send a code.");otp();});});}
    void otp(){body.removeAllViews();status.setVisibility(View.GONE);title("Check your messages","Enter the SMS code sent to "+mobile+".");EditText code=input(body,"SMS code",InputType.TYPE_CLASS_NUMBER,"");code.setAutofillHints("smsOTPCode");ui.space(body,20);button("Sign in",true,()->{String c=code.getText().toString().trim();if(!c.matches("[0-9]{4,8}")){code.setError("Enter the code from your SMS.");return;}hideKeyboard();request("user/verify","POST",ApiClient.form("mobile",mobile,"code",c,"client_id",ClientConfig.ID,"client_secret",ClientConfig.SECRET),true,j->{String t=j.getString("access_token");if(t.isEmpty())throw new Exception("No session was returned.");sessions.save(t);token=t;prefs.edit().putString("mobile",mobile).apply();screen="Home";render();refresh(null);});});button("Change number",false,()->render());}

    void request(String path,String method,String data,boolean form,Result done){if(busy)return;busy=true;notice("Please wait…");String session=token;worker.execute(()->{try{JSONObject result=ApiClient.call(session,path,method,data,form);if(method.equals("POST")&&!path.equals("user/verify")&&!path.equals("friends")&&!result.optBoolean("success",false))throw new Exception(result.optString("error","The request was not confirmed by the server."));handler.post(()->{if(isDestroyed())return;busy=false;status.setVisibility(View.GONE);try{done.done(result);}catch(Exception e){notice(e.getMessage());}});}catch(Exception e){handler.post(()->{if(isDestroyed())return;busy=false;notice(e.getMessage());});}});}

    void refresh(Runnable afterRefresh){if(busy)return;if(demo){render();if(afterRefresh!=null)afterRefresh.run();return;}busy=true;startupFailure="";status.setVisibility(View.GONE);String session=token;worker.execute(()->{try{JSONObject u=ApiClient.call(session,"user","GET",null,false),us=ApiClient.call(session,"usages","GET",null,false),r=ApiClient.call(session,"rates","GET",null,false),w=ApiClient.call(session,"wallet","GET",null,false);handler.post(()->{if(isDestroyed())return;busy=false;user=w.optJSONObject("user")==null?u:w.optJSONObject("user");JSONObject newest=w.optJSONObject("usage"),all=us.optJSONObject("data");if(newest!=null&&all!=null){for(String k:KEYS){JSONObject plan=newest.optJSONObject(k);if(plan!=null)try{all.put(k,plan);}catch(JSONException ignored){}}}usage=us;rates=r;wallet=w;loaded=true;render();if(afterRefresh!=null)afterRefresh.run();else if(screen.equals("Activity"))loadHistory(false);});}catch(Exception e){handler.post(()->{if(isDestroyed())return;busy=false;if(!loaded){startupFailure=e.getMessage()==null?"Could not load your account.":e.getMessage();render();}else notice(e.getMessage());});}});}
    void seed(){mobile="7•• ••••";user=json("id",1,"name","Your account","rb_balance",24.5,"email","you@example.com");JSONObject data=json("data",json("available",75027,"total",106496),"voice",json("available",520,"total",1200),"sms",json("available",67,"total",100));usage=json("data",data);wallet=json("usage",data,"user",user);rates=json("mb",.01,"min",.1,"sms",.01);history=new JSONArray();historyLoaded=true;loaded=true;}
    @Override public void onBackPressed(){if(busy)return;if(screen.equals("Home")||token.isEmpty()&&!demo){super.onBackPressed();return;}screen=screen.equals("Transfer")?"Activity":"Home";hideKeyboard();render();}
    @Override protected void onSaveInstanceState(Bundle state){super.onSaveInstanceState(state);state.putString("screen",screen);state.putString("transferMode",transferMode);state.putString("contactSearch",contactSearch);state.putBoolean("take",take);state.putIntArray("chosen",chosen);state.putBoolean("demo",demo);state.putString("recipientDraft",screen.equals("Transfer")&&recipient!=null?recipient.getText().toString():recipientDraft);state.putString("rbDraft",screen.equals("Transfer")&&transferAmount!=null?transferAmount.getText().toString():rbDraft);state.putString("bmlAmountDraft",screen.equals("Top up")&&bmlAmount!=null?bmlAmount.getText().toString():bmlAmountDraft);}
    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){super.onActivityResult(requestCode,resultCode,data);if(requestCode==54&&resultCode==RESULT_OK){screen="Top up";refresh(null);}}
    @Override protected void onDestroy(){worker.shutdownNow();handler.removeCallbacksAndMessages(null);super.onDestroy();}
}


















