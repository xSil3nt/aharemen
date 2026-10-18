package app.shazin.aharemen;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.*;
import android.view.View;
import android.view.animation.LinearInterpolator;

final class OrbitLoader extends View {
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final int accent,secondary,track;
    private float phase,pull=1;
    private boolean running;
    private ValueAnimator animator;
    OrbitLoader(Context c,Ui ui){super(c);accent=ui.mint;secondary=ui.blue;track=ui.line;setContentDescription("Loading");setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);}
    void progress(float value){pull=Math.max(0,Math.min(1,value));invalidate();}
    void running(boolean value){running=value;if(value&&isAttachedToWindow())start();else if(!value&&animator!=null){animator.cancel();animator=null;}invalidate();}
    private void start(){if(animator!=null||!ValueAnimator.areAnimatorsEnabled())return;animator=ValueAnimator.ofFloat(0,1);animator.setDuration(2400);animator.setRepeatCount(ValueAnimator.INFINITE);animator.setInterpolator(new LinearInterpolator());animator.addUpdateListener(a->{phase=(float)a.getAnimatedValue();invalidate();});animator.start();}
    @Override protected void onAttachedToWindow(){super.onAttachedToWindow();if(running)start();}
    @Override protected void onDetachedFromWindow(){if(animator!=null){animator.cancel();animator=null;}super.onDetachedFromWindow();}
    @Override protected void onDraw(Canvas c){
        super.onDraw(c);float density=getResources().getDisplayMetrics().density;
        float cx=getWidth()/2f,cy=getHeight()/2f,r=Math.min(getWidth(),getHeight())*.35f;
        float wave=(float)Math.sin(phase*Math.PI*2),breath=running?1+.035f*wave:1;
        c.save();c.scale(breath,breath,cx,cy);
        paint.setStyle(Paint.Style.STROKE);paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(1.2f*density);paint.setColor(track);paint.setAlpha(100);c.drawCircle(cx,cy,r,paint);paint.setAlpha(255);
        float sweep=running?55+190*(float)(.5-.5*Math.cos(phase*Math.PI*2)):Math.max(8,280*pull);
        float rotation=running?phase*720-90:-90+90*pull;
        paint.setStrokeWidth(2.8f*density);paint.setColor(accent);
        c.drawArc(cx-r,cy-r,cx+r,cy+r,rotation,sweep,false,paint);
        float inner=r*.66f;paint.setStrokeWidth(1.8f*density);paint.setColor(secondary);paint.setAlpha(210);
        c.drawArc(cx-inner,cy-inner,cx+inner,cy+inner,running?90-phase*720:-90,85+35*wave,false,paint);
        c.drawArc(cx-inner,cy-inner,cx+inner,cy+inner,running?270-phase*720:90,25+20*wave,false,paint);
        paint.setStyle(Paint.Style.FILL);paint.setColor(secondary);
        for(int i=2;i>=0;i--){double a=Math.toRadians(rotation+sweep+18+i*12);paint.setAlpha(255-i*75);float size=(2.7f-i*.6f)*density;c.drawCircle(cx+r*(float)Math.cos(a),cy+r*(float)Math.sin(a),size,paint);}
        paint.setAlpha(170);paint.setColor(accent);c.drawCircle(cx,cy,(1.5f+.4f*wave)*density,paint);
        paint.setAlpha(255);c.restore();
    }
}
