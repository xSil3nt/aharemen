package app.shazin.aharemen;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.widget.SeekBar;

final class AllowanceSlider extends SeekBar {
    private int limit,anchor;
    private final Paint tick=new Paint(Paint.ANTI_ALIAS_FLAG);
    AllowanceSlider(Context context,int color){super(context);tick.setColor(color);tick.setAlpha(150);tick.setStrokeWidth(context.getResources().getDisplayMetrics().density*2);}
    void range(int maximum,int snap){limit=maximum;anchor=snap;invalidate();}
    @Override protected void onDraw(Canvas canvas){
        super.onDraw(canvas);
        if(limit<anchor)return;
        float density=getResources().getDisplayMetrics().density;
        float fraction=SliderScale.position(anchor,limit,anchor)/(float)SliderScale.STEPS;
        if(getLayoutDirection()==LAYOUT_DIRECTION_RTL)fraction=1-fraction;
        float x=getPaddingLeft()+fraction*(getWidth()-getPaddingLeft()-getPaddingRight());
        canvas.drawLine(x,getHeight()/2f+8*density,x,getHeight()/2f+12*density,tick);
    }
}
