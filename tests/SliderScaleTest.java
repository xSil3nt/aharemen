package app.shazin.aharemen;

public final class SliderScaleTest {
    public static void main(String[] args){
        int checks=0;
        for(int max:new int[]{0,3,10,73,100,500,5000,1000000})for(int anchor:new int[]{10,100}){
            if(SliderScale.units(0,max,anchor)!=0||SliderScale.units(10000,max,anchor)!=max)throw new AssertionError("Endpoints");
            int previous=-1;
            for(int p=0;p<=10000;p++){int v=SliderScale.units(p,max,anchor);if(v<previous||v>max)throw new AssertionError("Non-monotonic scale");previous=v;}
            if(max>=anchor&&SliderScale.snap(SliderScale.position(anchor,max,anchor),max,anchor)!=anchor)throw new AssertionError("Missing snap");
            checks++;
        }
        if(SliderScale.units(2000,5000,100)>30)throw new AssertionError("Low end too coarse");
        if(SliderScale.units(SliderScale.position(25,5000,100),5000,100)!=25)throw new AssertionError("25 MB precision");
        if(SliderScale.snap(0,100,100)!=0)throw new AssertionError("Zero snapped incorrectly");
        System.out.println("19 slider range, monotonicity, precision and snapping checks passed.");
    }
}
