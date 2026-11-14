package app.shazin.aharemen;

import java.math.BigDecimal;

public final class QuickAddTest {
    static int checks;
    static void check(boolean result,String label){checks++;if(!result)throw new AssertionError(label);}
    static void rejects(Runnable action,String label){checks++;try{action.run();}catch(IllegalArgumentException expected){return;}throw new AssertionError(label);}
    public static void main(String[] args){
        double[] rates={.01,.1,.01};
        check(QuickAdd.units(0,rates)==25,"25 MB data");
        check(QuickAdd.units(1,rates)==3,"3 minutes");
        check(QuickAdd.units(2,rates)==10,"10 texts");
        check(QuickAdd.units(1,new double[]{.01,.13,.01})==3,"Fixed amount when rates change");
        check(QuickAdd.validate(0,25,new BigDecimal(".25"),rates,.25).compareTo(new BigDecimal(".25"))==0,"Exact RB data balance accepted");
        check(QuickAdd.validate(1,3,new BigDecimal(".3"),rates,.3).compareTo(new BigDecimal(".3"))==0,"Exact RB voice balance accepted");
        check(QuickAdd.validate(2,10,new BigDecimal(".1"),rates,.1).compareTo(new BigDecimal(".1"))==0,"Exact RB SMS balance accepted");
        rejects(()->QuickAdd.validate(0,25,new BigDecimal(".25"),rates,.24),"Insufficient RB");
        rejects(()->QuickAdd.validate(0,25,new BigDecimal(".25"),new double[]{.02,.1,.01},10),"Reject increased cost");
        rejects(()->QuickAdd.validate(0,50,new BigDecimal(".5"),rates,10),"Reject stale 50 MB shortcut");
        rejects(()->QuickAdd.validate(1,5,new BigDecimal(".5"),rates,10),"Reject stale 5 minute shortcut");
        rejects(()->QuickAdd.validate(2,50,new BigDecimal(".5"),rates,10),"Reject stale 50 SMS shortcut");
        rejects(()->QuickAdd.units(0,new double[]{0,.1,.01}),"Missing rates");
        rejects(()->QuickAdd.units(1,new double[]{.01,Double.NaN,.01}),"NaN rate");
        rejects(()->QuickAdd.units(4,rates),"Invalid allowance");
        rejects(()->QuickAdd.validate(0,-1,BigDecimal.ONE,rates,1),"Negative allowance");
        rejects(()->QuickAdd.validate(0,25,BigDecimal.ONE,rates,Double.NaN),"Unknown balance");
        check(QuickAdd.blocks("queued")&&QuickAdd.blocks("checking")&&QuickAdd.blocks("sending")&&QuickAdd.blocks("uncertain"),"Block duplicate and uncertain operations");
        check(!QuickAdd.blocks("confirmed")&&!QuickAdd.blocks("failed")&&!QuickAdd.blocks(""),"Allow intentional next operation");
        check(QuickAdd.mayClaim("checking",1000),"Preflight may claim once");
        check(!QuickAdd.mayClaim("sending",1000)&&!QuickAdd.mayClaim("uncertain",1000)&&!QuickAdd.mayClaim("queued",1000),"No submission replay");
        check(!QuickAdd.mayClaim("checking",120001)&&!QuickAdd.mayClaim("checking",-1),"Expire delayed jobs and clock rollback");
        System.out.println(checks+" widget fixed-amount, price, expiry and no-replay checks passed.");
    }
}
