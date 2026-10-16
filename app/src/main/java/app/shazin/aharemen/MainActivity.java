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
        ui.space(body,20);LinearLayout quick=ui.row();quickAction(quick,"send","Send RB",()->openTransfer("send"));quickAction(quick,"request","Request RB",()->openTransfer("request"));quickAction(quick,"wallet","Take back",()->openWallet(true));body.addView(quick);

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
    double[] prices(){return new double[]{rates.optDouble("mb",0),rates.optDouble("min",0),rates.optDouble("sms",0)};}
    void walletScreen(){
        ui.space(body,8);
        LinearLayout modes=ui.row();ui.pad(modes,4,4);modes.setBackground(ui.shape(ui.panel,15));
        for(boolean reverse:new boolean[]{false,true}){TextView m=ui.button(reverse?"Take back":"Build",false,()->{if(busy)return;take=reverse;Arrays.fill(chosen,0);render();});m.setTextColor(take==reverse?ui.ink:ui.muted);m.setBackground(ui.shape(take==reverse?ui.raised:ui.panel,12));modes.addView(m,new LinearLayout.LayoutParams(0,ui.d(42),1));}body.addView(modes);ui.space(body,18);
        Arrays.fill(sliders,null);Arrays.fill(fields,null);quote=null;after=null;review=null;
        for(int i=0;i<3;i++)sliderCard(i);
        LinearLayout dock=ui.col();ui.pad(dock,22,12);dock.setBackgroundColor(ui.bg);LinearLayout summary=ui.row();summary.addView(new Space(this),new LinearLayout.LayoutParams(0,1,1));quote=ui.medium("0 RB",25,ui.mint);summary.addView(quote);dock.addView(summary);after=ui.text("",12,ui.muted);dock.addView(after);ui.space(dock,12);
        review=ui.button(take?"Review take-back":"Review build",true,()->{if(!busy)reviewWallet();});dock.addView(review,ui.lp(-1,-2));root.addView(dock);updateQuote();
    }
    int maximum(int index){double[] p=prices();if(take){JSONObject plan=usagePlan(KEYS[index],true);double raw=plan==null?0:plan.optDouble("available",0);return (int)Math.min(1000000,Math.max(0,Math.floor(raw/(index==0?1024:index==1?60:1))));}double remaining=user.optDouble("rb_balance",0);for(int i=0;i<3;i++)if(i!=index)remaining-=chosen[i]*p[i];return WalletMath.maxUnits(Math.max(0,remaining),p[index]);}
    void sliderCard(int index){
        LinearLayout card=ui.col();ui.pad(card,16,14);card.setBackground(ui.shape(ui.panel,18));LinearLayout line=ui.row();int color=index==0?ui.blue:index==1?ui.peach:ui.mint;
        line.addView(ui.icon(KEYS[index],color),ui.lp(21,21));TextView name=ui.medium(NAMES[index],14,ui.ink);name.setPadding(ui.d(9),0,0,0);line.addView(name,new LinearLayout.LayoutParams(0,-2,1));
        EditText amount=new EditText(this);amount.setInputType(InputType.TYPE_CLASS_NUMBER);amount.setSingleLine(true);amount.setTextColor(ui.ink);amount.setTextSize(23);amount.setGravity(Gravity.END);amount.setPadding(0,ui.d(3),ui.d(5),ui.d(3));amount.setBackgroundColor(Color.TRANSPARENT);amount.setSelectAllOnFocus(true);amount.setText(String.valueOf(chosen[index]));amount.setContentDescription(NAMES[index]+" amount");line.addView(amount,ui.lp(80,44));line.addView(ui.text(UNITS[index],12,ui.muted));card.addView(line);
        SeekBar slider=new SeekBar(this);sliderLimits[index]=maximum(index);slider.setMax(sliderLimits[index]);slider.setProgress(chosen[index]);slider.setProgressTintList(ColorStateList.valueOf(color));slider.setThumbTintList(ColorStateList.valueOf(color));slider.setProgressBackgroundTintList(ColorStateList.valueOf(ui.raised));slider.setPadding(ui.d(14),0,ui.d(14),0);slider.setThumbOffset(ui.d(10));slider.setSplitTrack(false);slider.setContentDescription(NAMES[index]+" slider");card.addView(slider,ui.lp(-1,44));
        double rate=prices()[index];card.addView(ui.text(rate>0?"1 "+UNITS[index]+" = "+number(rate)+" RB":"Refresh to load rates",11,ui.muted));sliders[index]=slider;fields[index]=amount;
        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onStartTrackingTouch(SeekBar s){draggingSlider=index;hideKeyboard();}
            public void onStopTrackingTouch(SeekBar s){int n=s.getProgress();draggingSlider=-1;setAllowance(index,n);}
            public void onProgressChanged(SeekBar s,int n,boolean fromUser){if(!fromUser||busy)return;int anchor=index==1?10:100;int value=n;if(value==anchor&&chosen[index]!=anchor)s.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);chosen[index]=value;sliderUpdate=true;amount.setText(String.valueOf(value));sliderUpdate=false;updateQuote();}
        });
        amount.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int start,int count,int after){}public void onTextChanged(CharSequence s,int start,int before,int count){if(sliderUpdate||busy)return;try{chosen[index]=s.length()==0?0:Integer.parseInt(s.toString());}catch(Exception e){chosen[index]=Integer.MAX_VALUE;}sliderUpdate=true;slider.setProgress(chosen[index]);sliderUpdate=false;updateQuote();}public void afterTextChanged(Editable e){}});
        LinearLayout.LayoutParams params=ui.lp(-1,-2);params.setMargins(0,0,0,ui.d(12));body.addView(card,params);
    }
    void setAllowance(int index,int value){chosen[index]=value;sliderUpdate=true;fields[index].setText(String.valueOf(value));sliderUpdate=false;updateQuote();}
    void updateQuote(){if(quote==null||after==null)return;try{BigDecimal cost=WalletMath.cost(chosen,prices());quote.setText(decimal(cost)+" RB");double balance=user.optDouble("rb_balance",0);boolean hasSelection=chosen[0]>0||chosen[1]>0||chosen[2]>0;after.setVisibility(hasSelection?View.VISIBLE:View.GONE);if(hasSelection)after.setText(number(balance)+" → "+number(balance+(take?1:-1)*cost.doubleValue())+" RB");boolean valid=cost.signum()>0&&(take||cost.compareTo(BigDecimal.valueOf(balance))<=0)&&loaded;for(int i=0;i<3;i++){if(chosen[i]>maximum(i))valid=false;if(sliders[i]!=null){sliderUpdate=true;if(draggingSlider!=i){sliderLimits[i]=maximum(i);sliders[i].setMax(sliderLimits[i]);sliders[i].setProgress(chosen[i]);}sliders[i].setEnabled(sliderLimits[i]>0);sliderUpdate=false;}}if(review!=null)review.setAlpha(valid?1:.45f);}catch(Exception e){quote.setText("— RB");after.setVisibility(View.VISIBLE);after.setText("Refresh to load your conversion rates.");}}
    void reviewWallet(){try{if(!loaded)throw new Exception("Wait for your balances to load.");int[] selected=chosen.clone();boolean reverse=take;BigDecimal cost=WalletMath.cost(selected,prices());if(cost.signum()<=0)throw new Exception("Choose an allowance amount first.");if(!reverse&&cost.compareTo(BigDecimal.valueOf(user.optDouble("rb_balance",0)))>0)throw new Exception("You don’t have enough RB for this build.");for(int i=0;i<3;i++)if(selected[i]>maximum(i))throw new Exception("That amount exceeds your available balance.");
        LinearLayout c=sheet(reverse?"Take back allowances":"Build","",reverse?"wallet":"plus");for(int i=0;i<3;i++)if(selected[i]>0)pair(c,NAMES[i],number(selected[i])+" "+UNITS[i]);ui.space(c,10);ui.divider(c);pair(c,reverse?"RB returned":"RB spent",decimal(cost)+" RB");pair(c,"Balance after",number(user.optDouble("rb_balance",0)+(reverse?1:-1)*cost.doubleValue())+" RB");
        showSheet(c,reverse?"Confirm take-back":"Confirm build",()->{if(demo){simulateWallet(selected,reverse,cost);return;}request(reverse?"wallet/take":"wallet/build","POST",json("data",selected[0],"voice",selected[1],"sms",selected[2]).toString(),false,j->{Arrays.fill(chosen,0);refresh(()->success(reverse?"RB returned":"Allowance built",decimal(cost)+" RB "+(reverse?"returned to":"used from")+" your balance."));});});
    }catch(Exception e){notice(e.getMessage());}}
    void simulateWallet(int[] selected,boolean reverse,BigDecimal cost){try{double sign=reverse?-1:1;JSONObject plans=usage.getJSONObject("data");for(int i=0;i<3;i++){JSONObject p=plans.getJSONObject(KEYS[i]);p.put("available",p.optDouble("available",0)+sign*selected[i]*(i==0?1024:i==1?60:1));}user.put("rb_balance",user.optDouble("rb_balance",0)+(reverse?1:-1)*cost.doubleValue());Arrays.fill(chosen,0);render();success("Demo complete","Sample balances updated. Your real account was not changed.");}catch(Exception e){notice(e.getMessage());}}

    void openTransfer(String mode){transferMode=mode;recipientDraft="";rbDraft="10";contactSearch="";screen="Transfer";render();}
    void transfer(){
        title(transferMode.equals("send")?"Send RB":"Request RB","");
        recipient=input(body,"Mobile number",InputType.TYPE_CLASS_PHONE,recipientDraft);
        recipient.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_NEXT);
        ui.space(body,18);label("Amount in RB");transferAmount=input(body,"10",InputType.TYPE_CLASS_NUMBER,rbDraft);transferAmount.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_DONE);transferAmount.setOnEditorActionListener((v,a,event)->{if(a==android.view.inputmethod.EditorInfo.IME_ACTION_DONE){hideKeyboard();return true;}return false;});
        transferAmount.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int c,int f){}public void afterTextChanged(Editable e){}public void onTextChanged(CharSequence s,int a,int b,int c){rbDraft=s.toString();}});
        ui.space(body,10);body.addView(ui.text("Amounts in steps of 10 RB"+(transferMode.equals("request")?", up to 200 RB.":". Available: "+number(user.optDouble("rb_balance",0))+" RB."),12,ui.muted));ui.space(body,22);button("Review "+(transferMode.equals("send")?"transfer":"request"),true,()->reviewTransfer());
    }




    void reviewTransfer(){try{String phone;try{phone=WalletMath.mobile(recipient.getText().toString());}catch(Exception e){recipient.setError("Choose a contact or enter a seven-digit number.");recipient.requestFocus();return;}if(phone.equals(mobile))throw new Exception("Choose another person’s number.");int rb=Integer.parseInt(transferAmount.getText().toString());if(rb<10||rb%10!=0||transferMode.equals("request")&&rb>200)throw new Exception("Choose a multiple of 10 RB"+(transferMode.equals("request")?", from 10 to 200.":"."));if(transferMode.equals("send")&&rb>user.optDouble("rb_balance",0))throw new Exception("You don’t have enough RB.");String mode=transferMode;hideKeyboard();if(demo){transferReview(mode,phone,rb,json("id",123,"name","Demo recipient"));return;}request("friends","POST",json("numbers",new JSONArray().put(phone)).toString(),false,j->{JSONArray matches=j.optJSONArray("data");JSONObject match=null;if(matches!=null)for(int i=0;i<matches.length();i++){JSONObject candidate=matches.optJSONObject(i);if(candidate==null)continue;try{if(WalletMath.mobile(candidate.optString("mobile","")).equals(phone)){match=candidate;break;}}catch(Exception ignored){}}if(match==null||match.optInt("id",0)<=0)throw new Exception("That number was not found as a registered user.");if(match.optInt("id")==user.optInt("id"))throw new Exception("Choose another person’s account.");transferReview(mode,phone,rb,match);});}catch(NumberFormatException e){transferAmount.setError("Enter a valid RB amount.");}catch(Exception e){notice(e.getMessage());}}
    void transferReview(String mode,String phone,int rb,JSONObject person){LinearLayout c=sheet(mode.equals("send")?"Send RB":"Request RB",mode.equals("send")?"":"",mode.equals("send")?"send":"request");pair(c,"Name",person.optString("name","—"));pair(c,mode.equals("send")?"Recipient":"Request from",phone);pair(c,"Amount",rb+" RB");int id=person.optInt("id");showSheet(c,mode.equals("send")?"Send "+rb+" RB":"Send request",()->{if(demo){success("Demo only","No transfer or request was sent.");return;}request("transfer/"+mode,"POST",json("rb",rb,mode.equals("send")?"receiver":"from",id).toString(),false,j->{screen="Activity";historyLoaded=false;refresh(()->{loadHistory(false);success(mode.equals("send")?"RB sent":"Request sent",rb+" RB · "+phone);});});});}

    void activityScreen(){title("Activity","");LinearLayout actions=ui.row();quickAction(actions,"send","Send RB",()->openTransfer("send"));quickAction(actions,"request","Request RB",()->openTransfer("request"));body.addView(actions);ui.space(body,20);if(!historyLoaded){body.addView(ui.text("Loading your activity…",13,ui.muted));return;}if(history.length()==0){body.addView(ui.text("No activity",14,ui.muted));return;}for(int i=0;i<history.length();i++){JSONObject item=history.optJSONObject(i);if(item!=null)historyRow(item);}if(!nextHistory.isEmpty()){ui.space(body,16);button("Load earlier activity",false,()->loadHistory(true));}}
    void historyRow(JSONObject item){JSONObject t=item.optJSONObject("transaction");if(t==null)return;String type=t.optString("type","");boolean incoming=type.equals("received")||type.equals("user_request")||type.equals("user_response");String title=type.equals("received")?"RB received":type.equals("send")?"RB sent":type.equals("user_request")?"Request received":type.equals("my_request")?"Request sent":type.equals("user_response")?"Request response":"RB response";LinearLayout row=ui.row();ui.pad(row,0,16);row.addView(ui.icon(incoming?"request":"send",ui.mint),ui.lp(22,22));LinearLayout info=ui.col();info.setPadding(ui.d(13),0,ui.d(10),0);info.addView(ui.medium(title,14,ui.ink));ui.space(info,5);info.addView(ui.text(DateFormat.getDateInstance(DateFormat.MEDIUM).format(new Date(item.optLong("created_at",0)*1000)),11,ui.muted));row.addView(info,new LinearLayout.LayoutParams(0,-2,1));row.addView(amount(number(t.optDouble("amount",0)),"RB",20,ui.ink));body.addView(row);ui.divider(body);row.setOnClickListener(v->{if(!busy)historyDetails(item);});}
    void historyDetails(JSONObject item){JSONObject t=item.optJSONObject("transaction");if(t==null)return;String type=t.optString("type","");LinearLayout c=sheet("RB activity","","activity");pair(c,"Amount",number(t.optDouble("amount",0))+" RB");JSONObject users=item.optJSONObject("users"),other=users==null?null:users.optJSONObject("recipient");if(other!=null){pair(c,"Name",other.optString("name","—"));pair(c,"Number",other.optString("mobile","—"));}String message=t.optString("updated_message",t.optString("accepted_message",t.optString("ignored_message","")));if(!message.isEmpty()){ui.space(c,10);c.addView(ui.text(message,13,ui.muted));}boolean pending=type.equals("user_request")&&!t.has("accepted_message")&&!t.has("ignored_message")&&!t.optBoolean("accepted",false);if(pending){ui.space(c,14);TextView decline=ui.button("Decline request",false,()->{});c.addView(decline,ui.lp(-1,-2));Dialog d=showSheet(c,"Respond with RB",()->respond(item));decline.setOnClickListener(v->{d.dismiss();LinearLayout s=sheet("Decline request?","No RB will be sent.","close");showSheet(s,"Decline request",()->respondRequest(item,0,false));});}else showSheet(c,"Done",null);}
    void respond(JSONObject item){LinearLayout c=sheet("Respond with RB","","send");EditText input=input(c,"Amount in RB",InputType.TYPE_CLASS_NUMBER,"10");ui.space(c,10);c.addView(ui.text("Steps of 10 RB · Available "+number(user.optDouble("rb_balance",0))+" RB",12,ui.muted));Dialog dialog=showSheet(c,"Review amount",null);TextView action=(TextView)c.getChildAt(c.getChildCount()-1);action.setOnClickListener(v->{try{int rb=Integer.parseInt(input.getText().toString());if(rb<10||rb%10!=0||rb>user.optDouble("rb_balance",0))throw new Exception("Choose a multiple of 10 RB within your balance.");dialog.dismiss();LinearLayout review=sheet("Accept request","","send");pair(review,"Amount",rb+" RB");showSheet(review,"Send "+rb+" RB",()->respondRequest(item,rb,true));}catch(Exception e){input.setError(e.getMessage());}});}
    void respondRequest(JSONObject item,int rb,boolean accept){if(demo){success("Demo only","No response was sent.");return;}long id=item.optLong("id",-1);if(id<0){notice("This request has no valid identifier.");return;}request("transfer/accept/"+id,"POST",(accept?json("rb",rb,"accept",true):json("accept",false)).toString(),false,j->{historyLoaded=false;refresh(()->{loadHistory(false);success(accept?"RB sent":"Request declined","Your response was saved.");});});}
    void loadHistory(boolean append){if(demo){historyLoaded=true;render();return;}String path=append?nextHistory:"transfer/history?page=1";if(path.isEmpty())return;request(path,"GET",null,false,j->{JSONArray page=j.optJSONArray("data");if(page==null)throw new Exception("The server returned an unexpected activity list.");if(!append)history=new JSONArray();for(int i=0;i<page.length();i++)history.put(page.get(i));historyLoaded=true;nextHistory=safeHistory(j.optString("next_page_url",""));if(screen.equals("Activity"))render();});}
    String safeHistory(String value){try{java.net.URI base=new java.net.URI(ApiClient.BASE),u=base.resolve(value);if(value.isEmpty()||value.equals("null")||!"https".equals(u.getScheme())||!base.getHost().equals(u.getHost())||u.getPort()!=-1||!u.getPath().equals("/api/transfer/history"))return "";return "transfer/history"+(u.getRawQuery()==null?"":"?"+u.getRawQuery());}catch(Exception e){return "";}}

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


















