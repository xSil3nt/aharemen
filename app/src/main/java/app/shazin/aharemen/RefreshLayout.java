package app.shazin.aharemen;

import android.content.Context;
import android.view.*;
import android.widget.*;

final class RefreshLayout extends FrameLayout {
    interface Action {boolean allowed();void refresh();}
    private final ScrollView scroll;
    private final OrbitLoader loader;
    private final Action action;
    private final float density;
    private float startX,startY,distance;
    private boolean dragging,blocked,refreshing;
    RefreshLayout(Context c,ScrollView content,Ui ui,Action action){super(c);this.action=action;scroll=content;density=getResources().getDisplayMetrics().density;addView(scroll,new LayoutParams(-1,-1));loader=new OrbitLoader(c,ui);loader.setVisibility(INVISIBLE);LayoutParams p=new LayoutParams(ui.d(48),ui.d(48),Gravity.TOP|Gravity.CENTER_HORIZONTAL);p.topMargin=ui.d(8);addView(loader,p);setClipChildren(false);}
    void refreshing(boolean value){refreshing=value;dragging=false;if(value){loader.setVisibility(VISIBLE);loader.setAlpha(1);loader.running(true);scroll.animate().translationY(64*density).setDuration(160).start();}else{loader.running(false);loader.setVisibility(INVISIBLE);scroll.animate().translationY(0).setDuration(180).start();distance=0;}}
    private boolean protectedAt(View view,float x,float y){if(view instanceof SeekBar||view instanceof EditText)return true;if(view.canScrollVertically(-1))return true;if(view instanceof ViewGroup){ViewGroup g=(ViewGroup)view;for(int i=g.getChildCount()-1;i>=0;i--){View child=g.getChildAt(i);float cx=x+g.getScrollX()-child.getLeft(),cy=y+g.getScrollY()-child.getTop();if(child.getVisibility()==VISIBLE&&cx>=0&&cy>=0&&cx<child.getWidth()&&cy<child.getHeight()&&protectedAt(child,cx,cy))return true;}}return false;}
    @Override public boolean onInterceptTouchEvent(android.view.MotionEvent e){if(refreshing||!action.allowed())return false;if(e.getActionMasked()==MotionEvent.ACTION_DOWN){startX=e.getX();startY=e.getY();dragging=false;blocked=protectedAt(scroll,e.getX(),e.getY());}else if(e.getActionMasked()==MotionEvent.ACTION_MOVE&&!blocked&&!scroll.canScrollVertically(-1)){float dy=e.getY()-startY;if(dy>ViewConfiguration.get(getContext()).getScaledTouchSlop()*2&&dy>Math.abs(e.getX()-startX)*1.3f){dragging=true;scroll.animate().cancel();getParent().requestDisallowInterceptTouchEvent(true);return true;}}return false;}
    @Override public boolean onTouchEvent(MotionEvent e){if(!dragging)return refreshing;switch(e.getActionMasked()){case MotionEvent.ACTION_MOVE:distance=Math.min(96*density,Math.max(0,(e.getY()-startY)*.5f));scroll.setTranslationY(distance);loader.setVisibility(VISIBLE);loader.setAlpha(Math.min(1,distance/(48*density)));loader.progress(distance/(64*density));return true;case MotionEvent.ACTION_UP:boolean trigger=distance>=64*density;dragging=false;getParent().requestDisallowInterceptTouchEvent(false);if(trigger&&action.allowed()){performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);refreshing(true);action.refresh();}else refreshing(false);return true;case MotionEvent.ACTION_CANCEL:dragging=false;refreshing(false);return true;}return true;}
}

