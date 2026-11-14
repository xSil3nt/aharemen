package app.shazin.aharemen;

import java.math.BigDecimal;

public final class QuickAddTest {
    static int checks;
    static void check(boolean result,String label){checks++;if(!result)throw new AssertionError(label);}
    static void rejects(Runnable action,String label){checks++;try{action.run();}catch(IllegalArgumentException expected){return;}throw new AssertionError(label);}
    public static void main(String[] args){
        double[] rates={.01,.1,.01};
        check(QuickAdd.units(0,rates)==50,"50 MB data");
        check(QuickAdd.units(1,rates)==5,"Equivalent minutes");
        check(QuickAdd.units(2,rates)==50,"Equivalent texts");
        check(QuickAdd.units(1,new double[]{.01,.13,.01})==3,"Never round above budget");
        check(QuickAdd.units(2,new double[]{.01,.1,1})==0,"Hide unaffordable whole-unit equivalent");
        check(QuickAdd.validate(1,5,new BigDecimal("0.5"),rates,.5).compareTo(new BigDecimal("0.5"))==0,"Exact RB balance accepted");
        rejects(()->QuickAdd.validate(0,50,new BigDecimal("0.5"),rates,.49),"Insufficient RB");
        rejects(()->QuickAdd.validate(0,50,new BigDecimal("0.5"),new double[]{.02,.1,.01},10),"Reject increased cost");
        rejects(()->QuickAdd.validate(1,5,new BigDecimal("0.5"),new double[]{.005,.1,.01},10),"Respect current 50 MB budget");
        rejects(()->QuickAdd.units(0,new double[]{0,.1,.01}),"Missing rates");
        rejects(()->QuickAdd.units(1,new double[]{.01,Double.NaN,.01}),"NaN rate");
        rejects(()->QuickAdd.units(4,rates),"Invalid allowance");
        rejects(()->QuickAdd.validate(0,-1,BigDecimal.ONE,rates,1),"Negative allowance");
        rejects(()->QuickAdd.validate(0,50,BigDecimal.ONE,rates,Double.NaN),"Unknown balance");
        check(QuickAdd.blocks("queued")&&QuickAdd.blocks("checking")&&QuickAdd.blocks("sending")&&QuickAdd.blocks("uncertain"),"Block duplicate and uncertain operations");
        check(!QuickAdd.blocks("confirmed")&&!QuickAdd.blocks("failed")&&!QuickAdd.blocks(""),"Allow intentional next operation");
        check(QuickAdd.mayClaim("checking",1000),"Preflight may claim once");
        check(!QuickAdd.mayClaim("sending",1000)&&!QuickAdd.mayClaim("uncertain",1000)&&!QuickAdd.mayClaim("queued",1000),"No submission replay");
        check(!QuickAdd.mayClaim("checking",120001)&&!QuickAdd.mayClaim("checking",-1),"Expire delayed jobs and clock rollback");
        for(int i=1;i<=200;i++){double[] sampled={.01,i/100.0,.007};int n=QuickAdd.units(1,sampled);check(BigDecimal.valueOf(sampled[1]).multiply(BigDecimal.valueOf(n)).compareTo(QuickAdd.budget(sampled[0]))<=0,"Never overspend across rates "+i);}
        System.out.println(checks+" widget equivalents, budget, expiry and no-replay checks passed.");
    }
}
