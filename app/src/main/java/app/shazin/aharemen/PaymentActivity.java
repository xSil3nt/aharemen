package app.shazin.aharemen;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.net.http.SslError;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import org.json.JSONObject;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Hosts Dhiraagu's BML checkout and reports its receipt back to the account API. */
public final class PaymentActivity extends Activity {
    private static final String CHECKOUT="https://www.dhiraagu.com.mv/ocs/mobile_pay.aspx";
    private static final String RECEIPT_URL="https://www.dhiraagu.com.mv/ocs/service_reload_payment_response.aspx";
    private static final String CHECKSUM_SUFFIX=ClientConfig.PAYMENT_CHECKSUM_SUFFIX;
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final Handler main=new Handler(Looper.getMainLooper());
    private Ui ui;
    private WebView web;
    private FrameLayout content;
    private OrbitLoader loader;
    private LinearLayout errorPanel;
    private String token,amount,mobile;
    private JSONObject receipt;
    private boolean receiptCaptured,reporting;
    private String reportError="";

    @Override protected void onCreate(Bundle state){
        super.onCreate(state);
        token=getIntent().getStringExtra("token");amount=getIntent().getStringExtra("amount");mobile=getIntent().getStringExtra("mobile");
        if(token==null||amount==null||mobile==null){setResult(RESULT_CANCELED);finish();return;}
        ui=new Ui(this);getWindow().setStatusBarColor(ui.bg);getWindow().setNavigationBarColor(ui.bg);getWindow().getDecorView().setSystemUiVisibility(0);
        if(android.os.Build.VERSION.SDK_INT>=30&&getWindow().getInsetsController()!=null){int flags=android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS|android.view.WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;getWindow().getInsetsController().setSystemBarsAppearance(0,flags);}
        LinearLayout root=ui.col();root.setBackgroundColor(ui.bg);root.setOnApplyWindowInsetsListener((v,i)->{v.setPadding(i.getSystemWindowInsetLeft(),i.getSystemWindowInsetTop(),i.getSystemWindowInsetRight(),i.getSystemWindowInsetBottom());return i.consumeSystemWindowInsets();});setContentView(root);root.requestApplyInsets();
        LinearLayout bar=ui.row();ui.pad(bar,18,12);View back=ui.icon("back",ui.ink);back.setContentDescription("Back");bar.addView(back,ui.lp(42,46));back.setOnClickListener(v->leavePrompt());TextView title=ui.medium("BML payment",18,ui.ink);title.setPadding(ui.d(10),0,0,0);bar.addView(title);root.addView(bar);
        content=new FrameLayout(this);root.addView(content,new LinearLayout.LayoutParams(-1,0,1));
        web=new WebView(this);web.setBackgroundColor(Color.WHITE);content.addView(web,new FrameLayout.LayoutParams(-1,-1));
        WebSettings settings=web.getSettings();settings.setJavaScriptEnabled(true);settings.setJavaScriptCanOpenWindowsAutomatically(true);settings.setDomStorageEnabled(true);settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);settings.setSupportMultipleWindows(false);
        CookieManager.getInstance().setAcceptCookie(true);CookieManager.getInstance().setAcceptThirdPartyCookies(web,true);web.setWebChromeClient(new WebChromeClient());web.setWebViewClient(new CheckoutClient());
        loader=new OrbitLoader(this,ui);loader.running(true);FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(ui.d(76),ui.d(76),Gravity.CENTER);content.addView(loader,lp);
        errorPanel=makeErrorPanel();content.addView(errorPanel,new FrameLayout.LayoutParams(-1,-1));errorPanel.setVisibility(View.GONE);
        if(state!=null){receiptCaptured=state.getBoolean("receiptCaptured");if(state.containsKey("receipt"))try{receipt=new JSONObject(state.getString("receipt"));}catch(Exception ignored){}if(receipt!=null)showReceipt();else if(web.restoreState(state)==null)startCheckout();}
        else startCheckout();
    }

    private void startCheckout(){
        try{
            String checksum=md5(mobile+amount+CHECKSUM_SUFFIX);
            String body="type=3&serviceno="+enc(mobile)+"&amount="+enc(amount)+"&mid="+enc(checksum)+"&userid=";
            web.postUrl(CHECKOUT,body.getBytes(StandardCharsets.UTF_8));
        }catch(Exception e){showLoadError("Could not open the payment page.");}
    }
    private static String enc(String value)throws Exception{return URLEncoder.encode(value,"UTF-8");}
    private static String md5(String value)throws Exception{byte[] bytes=MessageDigest.getInstance("MD5").digest(value.getBytes(StandardCharsets.UTF_8));StringBuilder out=new StringBuilder(bytes.length*2);for(byte b:bytes)out.append(String.format(java.util.Locale.US,"%02x",b&255));return out.toString();}

    private LinearLayout makeErrorPanel(){
        LinearLayout panel=ui.col();panel.setGravity(Gravity.CENTER);panel.setPadding(ui.d(24),ui.d(24),ui.d(24),ui.d(24));panel.setBackgroundColor(ui.bg);
        TextView message=ui.medium("Couldn’t load the payment page",18,ui.ink);message.setGravity(Gravity.CENTER);panel.addView(message);ui.space(panel,18);panel.addView(ui.button("Try again",true,()->{errorPanel.setVisibility(View.GONE);loader.setVisibility(View.VISIBLE);loader.running(true);startCheckout();}));return panel;
    }
    private void showLoadError(String message){loader.running(false);loader.setVisibility(View.GONE);errorPanel.setVisibility(View.VISIBLE);}

    private boolean isReceiptUrl(String url){try{Uri u=Uri.parse(url);return "https".equalsIgnoreCase(u.getScheme())&&"www.dhiraagu.com.mv".equalsIgnoreCase(u.getHost())&&"/ocs/service_reload_payment_response.aspx".equalsIgnoreCase(u.getPath());}catch(Exception e){return false;}}
    private final class CheckoutClient extends WebViewClient{
        @Override public boolean shouldOverrideUrlLoading(WebView view,WebResourceRequest request){return allowUrl(request.getUrl());}
        @Override public boolean shouldOverrideUrlLoading(WebView view,String url){return allowUrl(Uri.parse(url));}
        private boolean allowUrl(Uri uri){String scheme=uri.getScheme();if("https".equalsIgnoreCase(scheme))return false;if("intent".equalsIgnoreCase(scheme)){try{startActivity(android.content.Intent.parseUri(uri.toString(),android.content.Intent.URI_INTENT_SCHEME));}catch(Exception ignored){}return true;}return true;}
        @Override public void onPageStarted(WebView view,String url,android.graphics.Bitmap favicon){super.onPageStarted(view,url,favicon);loader.setVisibility(View.VISIBLE);loader.running(true);if(!isReceiptUrl(url))view.removeJavascriptInterface("AharemenReceipt");}
        @Override public void onPageFinished(WebView view,String url){super.onPageFinished(view,url);if(isReceiptUrl(url)){if(!receiptCaptured){receiptCaptured=true;captureReceipt(view);}return;}loader.running(false);loader.setVisibility(View.GONE);}
        @Override public void onReceivedError(WebView view,WebResourceRequest request,android.webkit.WebResourceError error){super.onReceivedError(view,request,error);if(request.isForMainFrame())showLoadError("Couldn’t load the payment page.");}
        @Override public void onReceivedSslError(WebView view,SslErrorHandler handler,SslError error){handler.cancel();showLoadError("The payment site’s secure connection failed.");}
    }

    private void captureReceipt(WebView view){
        view.addJavascriptInterface(new ReceiptBridge(),"AharemenReceipt");
        String script="(function(){var g=function(id){var e=document.getElementById(id);return e?(e.innerText||e.textContent||'').trim():''};var r={invoice:g('ctl00_ContentPlaceHolder1_lbl_InvoiceNo'),order:g('ctl00_ContentPlaceHolder1_lbl_OrderNo'),date:g('ctl00_ContentPlaceHolder1_lbl_date'),phone:g('ctl00_ContentPlaceHolder1_lbl_ServiceNo'),amount:g('ctl00_ContentPlaceHolder1_lbl_amount'),status:g('ctl00_ContentPlaceHolder1_lbl_status')};window.AharemenReceipt.receive(JSON.stringify(r));})()";
        view.evaluateJavascript(script,null);
    }
    public final class ReceiptBridge{
        @JavascriptInterface public void receive(String json){main.post(()->{web.removeJavascriptInterface("AharemenReceipt");try{receipt=new JSONObject(json);}catch(Exception e){receipt=new JSONObject();}reportReceipt();});}
    }

    private void reportReceipt(){
        if(reporting||receipt==null)return;reporting=true;reportError="";if(loader.getParent()==null){content.addView(loader,new FrameLayout.LayoutParams(ui.d(76),ui.d(76),Gravity.CENTER));}loader.setVisibility(View.VISIBLE);loader.running(true);String status=receipt.optString("status","");JSONObject payload=new JSONObject();
        try{payload.put("invoice_no",receipt.optString("invoice",""));payload.put("status",status.equalsIgnoreCase("successful"));payload.put("amount",receipt.optString("amount",amount));payload.put("transaction_no",receipt.optString("order",""));}catch(Exception ignored){}
        String data=payload.toString();worker.execute(()->{try{ApiClient.call(token,"bmlTopup","POST",data,false);main.post(()->{reporting=false;loader.running(false);loader.setVisibility(View.GONE);showReceipt();});}catch(Exception e){main.post(()->{reporting=false;loader.running(false);loader.setVisibility(View.GONE);reportError=e.getMessage()==null?"Could not update the account.":e.getMessage();showReceipt();});}});
    }

    private void showReceipt(){
        LinearLayout page=ui.col();page.setPadding(ui.d(24),ui.d(24),ui.d(24),ui.d(24));page.setBackgroundColor(ui.bg);page.setGravity(Gravity.CENTER_VERTICAL);
        String stateText=receipt==null?"":receipt.optString("status","");boolean paid=stateText.equalsIgnoreCase("successful");
        page.addView(ui.icon(paid?"check":"close",paid?ui.mint:ui.peach),ui.lp(42,42));ui.space(page,18);page.addView(ui.medium(paid?"Payment successful":"Payment not completed",23,ui.ink));ui.space(page,20);
        addReceiptRow(page,"Amount",receipt==null?amount+" MVR":receipt.optString("amount",amount)+" MVR");if(receipt!=null&&!receipt.optString("date","").isEmpty())addReceiptRow(page,"Date",receipt.optString("date"));if(receipt!=null&&!receipt.optString("order","").isEmpty())addReceiptRow(page,"Reference",receipt.optString("order"));if(receipt!=null&&!receipt.optString("invoice","").isEmpty())addReceiptRow(page,"Invoice",receipt.optString("invoice"));
        if(!reportError.isEmpty()){ui.space(page,18);page.addView(ui.text(reportError,13,ui.peach));ui.space(page,14);page.addView(ui.button("Retry update",false,()->{reportReceipt();}));}
        ui.space(page,24);page.addView(ui.button("Done",true,()->{setResult(RESULT_OK);finish();}));content.removeAllViews();content.addView(page,new FrameLayout.LayoutParams(-1,-1));
    }
    private void addReceiptRow(LinearLayout page,String label,String value){LinearLayout row=ui.row();ui.pad(row,0,10);row.addView(ui.text(label,13,ui.muted),new LinearLayout.LayoutParams(0,-2,1));TextView v=ui.medium(value,15,ui.ink);v.setGravity(Gravity.END);row.addView(v,new LinearLayout.LayoutParams(0,-2,1));page.addView(row);ui.divider(page);}

    private void leavePrompt(){
        if(receipt!=null){setResult(RESULT_OK);finish();return;}
        LinearLayout panel=ui.col();panel.setPadding(ui.d(24),ui.d(20),ui.d(24),ui.d(22));panel.setBackground(ui.shape(ui.panel,24));TextView title=ui.medium("Leave payment?",20,ui.ink);panel.addView(title);ui.space(panel,10);panel.addView(ui.text("Your payment may still be processing.",14,ui.muted));ui.space(panel,20);
        Dialog dialog=new Dialog(this);dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);ScrollView scroll=new ScrollView(this);scroll.addView(panel);dialog.setContentView(scroll);Window window=dialog.getWindow();if(window!=null){window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);WindowManager.LayoutParams params=window.getAttributes();params.dimAmount=.65f;window.setAttributes(params);window.setGravity(Gravity.BOTTOM);}
        panel.addView(ui.button("Stay",false,dialog::dismiss));ui.space(panel,8);panel.addView(ui.button("Leave",true,()->{dialog.dismiss();setResult(RESULT_CANCELED);finish();}));dialog.show();if(window!=null)window.setLayout(-1,-2);
    }
    @Override public void onBackPressed(){if(web!=null&&web.getVisibility()==View.VISIBLE&&web.canGoBack()){web.goBack();return;}leavePrompt();}
    @Override protected void onSaveInstanceState(Bundle state){super.onSaveInstanceState(state);state.putBoolean("receiptCaptured",receiptCaptured);if(receipt!=null)state.putString("receipt",receipt.toString());if(web!=null)web.saveState(state);}
    @Override protected void onDestroy(){worker.shutdownNow();if(web!=null){web.stopLoading();web.destroy();}super.onDestroy();}
}
