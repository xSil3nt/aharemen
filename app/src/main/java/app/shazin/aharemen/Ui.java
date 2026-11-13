package app.shazin.aharemen;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.*;
import android.content.res.ColorStateList;
import android.view.*;
import android.widget.*;

/** Small native component set, shared by screens and transaction sheets. */
final class Ui {
    final Context context;
    final java.util.ArrayList<ActionButton> buttons=new java.util.ArrayList<>();
    ActionButton lastAction;
    final int bg, panel, raised, line, ink, muted, mint, blue, peach;
    Ui(Context c) {
        context=c;
        bg=0xff101716;
        panel=0xff1a2321;
        raised=0xff25312d;
        line=0xff30403a;
        ink=0xffedf5ed;
        muted=0xff9eafa5;
        mint=0xffb4e7c7;
        blue=0xffa8ccea;
        peach=0xffedc3a2;
    }
    int d(float n) { return Math.round(n*context.getResources().getDisplayMetrics().density); }
    LinearLayout col() { LinearLayout l=new LinearLayout(context);l.setOrientation(1);return l; }
    LinearLayout row() { LinearLayout l=new LinearLayout(context);l.setGravity(Gravity.CENTER_VERTICAL);return l; }
    LinearLayout.LayoutParams lp(int w,int h) { return new LinearLayout.LayoutParams(w<0?w:d(w),h<0?h:d(h)); }
    TextView text(String s,int size,int color) {
        TextView t=new TextView(context);t.setText(s);t.setTextSize(size);t.setTextColor(color);
        t.setFontFeatureSettings("tnum");t.setIncludeFontPadding(false);t.setLineSpacing(d(2),1);
        return t;
    }
    TextView medium(String s,int size,int color) {TextView t=text(s,size,color);t.setTypeface(Typeface.create("sans-serif-medium",0));return t;}
    GradientDrawable shape(int color,int radius) {GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(d(radius));return g;}
    GradientDrawable outline(int color,int radius) {GradientDrawable g=shape(color,radius);g.setStroke(d(1),line);return g;}
    RippleDrawable touchSurface(int color,int radius){return new RippleDrawable(ColorStateList.valueOf(0x25ffffff),shape(color,radius),null);}
    void space(LinearLayout parent,int h) {parent.addView(new Space(context),lp(1,h));}
    void divider(LinearLayout parent) {View v=new View(context);v.setBackgroundColor(line);parent.addView(v,lp(-1,1));}
    void pad(View v,int h,int vertical) {v.setPadding(d(h),d(vertical),d(h),d(vertical));}
    TextView button(String title,boolean primary,Runnable action) {
        ActionButton b=new ActionButton(title,primary);buttons.add(b);b.setGravity(Gravity.CENTER);b.setMinHeight(d(50));pad(b,16,12);
        b.setBackground(new RippleDrawable(ColorStateList.valueOf(0x25ffffff),shape(primary?mint:raised,16),null));
        b.setFocusable(true);b.setOnClickListener(v->{lastAction=b;action.run();});return b;
    }
    void working(boolean value,String label,TextView fallback){
        ActionButton active=lastAction!=null&&lastAction.isAttachedToWindow()?lastAction:fallback instanceof ActionButton?(ActionButton)fallback:null;
        for(ActionButton b:buttons){b.loading=value&&b==active;b.setEnabled(!value);b.setText(b.loading?label:b.title);b.setAlpha(value&&!b.loading?.5f:1);b.setPadding(d(b.loading?44:16),d(12),d(16),d(12));b.invalidate();}
    }
    final class ActionButton extends TextView {
        final String title;final boolean primary;boolean loading;final Paint spinner=new Paint(3);
        ActionButton(String text,boolean filled){super(context);title=text;primary=filled;setText(text);setTextSize(15);setTextColor(filled?bg:ink);setTypeface(Typeface.create("sans-serif-medium",0));setIncludeFontPadding(false);setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);}
        @Override protected void onDraw(Canvas canvas){super.onDraw(canvas);if(!loading)return;spinner.setColor(primary?bg:mint);spinner.setStyle(Paint.Style.STROKE);spinner.setStrokeWidth(d(2));spinner.setStrokeCap(Paint.Cap.ROUND);float x=d(25),y=getHeight()/2f,r=d(8);float angle=android.animation.ValueAnimator.areAnimatorsEnabled()?(android.os.SystemClock.uptimeMillis()%1000)*.36f:-90;canvas.drawArc(x-r,y-r,x+r,y+r,angle,250,false,spinner);if(android.animation.ValueAnimator.areAnimatorsEnabled())postInvalidateDelayed(16);}
    }
    LinearLayout loadingRow(String label){LinearLayout row=row();pad(row,0,10);OrbitLoader loader=new OrbitLoader(context,this);loader.running(true);loader.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);row.addView(loader,lp(28,28));TextView text=text(label,13,muted);text.setPadding(d(10),0,0,0);row.addView(text,new LinearLayout.LayoutParams(0,-2,1));return row;}
    void placeholder(LinearLayout parent,int width,int height){View block=new View(context);block.setBackground(shape(raised,6));block.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);parent.addView(block,lp(width,height));}
    void skeletonRows(LinearLayout parent,int count){for(int i=0;i<count;i++){LinearLayout row=row();pad(row,0,18);View icon=new View(context);icon.setBackground(shape(raised,14));row.addView(icon,lp(40,40));LinearLayout lines=col();lines.setPadding(d(14),0,0,0);placeholder(lines,120,12);space(lines,10);placeholder(lines,80,9);row.addView(lines,new LinearLayout.LayoutParams(0,-2,1));placeholder(row,54,16);parent.addView(row);divider(parent);}}
    Icon icon(String name,int color) {return new Icon(context,name,color);}
    final class Icon extends View {
        final String name; final Paint p=new Paint(3); final int color;
        Icon(Context c,String n,int color) {super(c);name=n;this.color=color;setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
        void l(Canvas c,float a,float b,float x,float y){c.drawLine(a,b,x,y,p);}
        @Override protected void onDraw(Canvas original) {
            super.onDraw(original);Canvas c=original;c.save();c.scale(getWidth()/24f,getHeight()/24f);
            p.setColor(color);p.setStrokeWidth(1.65f);p.setStyle(Paint.Style.STROKE);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);
            Path path=new Path();
            switch(name) {
                case "home":path.moveTo(4,10);path.lineTo(12,3);path.lineTo(20,10);path.lineTo(20,20);path.lineTo(15,20);path.lineTo(15,14);path.lineTo(9,14);path.lineTo(9,20);path.lineTo(4,20);path.close();c.drawPath(path,p);break;
                case "wallet":c.drawRoundRect(3,6,21,20,3,3,p);l(c,5,6,18,3);c.drawRoundRect(14,11,21,16,2,2,p);c.drawCircle(17,13.5f,.5f,p);break;
                case "activity":l(c,5,7,19,7);l(c,16,4,19,7);l(c,16,10,19,7);l(c,19,17,5,17);l(c,8,14,5,17);l(c,8,20,5,17);break;
                case "account":c.drawCircle(12,8,4,p);c.drawArc(4,14,20,26,195,150,false,p);break;
                case "data":c.drawArc(3,5,21,21,220,100,false,p);c.drawArc(7,9,17,19,220,100,false,p);c.drawCircle(12,17,1,p);break;
                case "voice":path.moveTo(6,3);path.lineTo(3,5);path.cubicTo(3,13,11,21,19,21);path.lineTo(21,17);path.lineTo(16,14);path.lineTo(14,17);path.cubicTo(10,15,8,12,7,9);path.lineTo(10,7);path.lineTo(8,3);path.close();c.drawPath(path,p);break;
                case "sms":c.drawRoundRect(3,4,21,18,4,4,p);l(c,7,18,5,22);l(c,5,22,12,18);l(c,8,9,16,9);l(c,8,13,13,13);break;
                case "plus":l(c,5,12,19,12);l(c,12,5,12,19);break;
                case "back":l(c,19,12,5,12);l(c,5,12,11,6);l(c,5,12,11,18);break;
                case "refresh":c.drawArc(4,4,20,20,40,290,false,p);l(c,20,4,20,10);l(c,20,10,14,10);break;
                case "send":l(c,6,18,18,6);l(c,6,6,18,6);l(c,18,6,18,18);break;
                case "request":l(c,18,6,6,18);l(c,6,6,6,18);l(c,6,18,18,18);break;
                case "check":path.moveTo(5,12);path.lineTo(10,17);path.lineTo(20,6);c.drawPath(path,p);break;
                case "close":l(c,6,6,18,18);l(c,18,6,6,18);break;
                default:c.drawCircle(12,12,8,p);l(c,12,7,12,13);c.drawCircle(12,17,.5f,p);
            }
            c.restore();
        }
    }
}

