package app.shazin.aharemen;

/** A soft logarithmic scale: precise near zero, with the full range still reachable. */
final class SliderScale {
    static final int STEPS=10000;
    static int units(int position,int maximum,int anchor) {
        if(maximum<=0)return 0;
        double p=Math.max(0,Math.min(STEPS,position))/(double)STEPS;
        double soft=Math.max(1,anchor/10.0);
        return (int)Math.min(maximum,Math.round(soft*Math.expm1(p*Math.log1p(maximum/soft))));
    }
    static int position(int units,int maximum,int anchor) {
        if(maximum<=0)return 0;
        double soft=Math.max(1,anchor/10.0);
        return (int)Math.round(STEPS*Math.log1p(Math.max(0,Math.min(maximum,units))/soft)/Math.log1p(maximum/soft));
    }
    static int snap(int position,int maximum,int anchor) {
        if(maximum>=anchor&&Math.abs(position-position(anchor,maximum,anchor))<=180)return anchor;
        return units(position,maximum,anchor);
    }
}
