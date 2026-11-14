package app.shazin.aharemen;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** A quick-add spends no more than the displayed 50 MB quote. */
final class QuickAdd {
    static BigDecimal budget(double mbRate){
        if(!Double.isFinite(mbRate)||mbRate<=0)throw new IllegalArgumentException("Refresh rates in aharemen.");
        return BigDecimal.valueOf(mbRate).multiply(BigDecimal.valueOf(50));
    }
    static int units(int kind,double[] rates){
        if(kind<0||kind>2||rates.length!=3||!Double.isFinite(rates[kind])||rates[kind]<=0)throw new IllegalArgumentException("Refresh rates in aharemen.");
        return budget(rates[0]).divide(BigDecimal.valueOf(rates[kind]),0,RoundingMode.DOWN).min(BigDecimal.valueOf(1000000)).intValue();
    }
    static BigDecimal validate(int kind,int amount,BigDecimal quotedCost,double[] rates,double balance){
        if(kind<0||kind>2||amount<=0||amount>1000000||quotedCost==null||quotedCost.signum()<=0)throw new IllegalArgumentException("Open aharemen to update this shortcut.");
        if(!Double.isFinite(balance)||balance<0)throw new IllegalArgumentException("Refresh balances in aharemen.");
        int[] selected={0,0,0};selected[kind]=amount;
        BigDecimal cost=WalletMath.cost(selected,rates);
        if(cost.compareTo(quotedCost)>0||cost.compareTo(budget(rates[0]))>0)throw new IllegalArgumentException("Rates changed. Refresh before adding.");
        if(cost.compareTo(BigDecimal.valueOf(balance))>0)throw new IllegalArgumentException("Not enough RB. Top up in aharemen.");
        return cost;
    }
    static boolean blocks(String state){return state.equals("queued")||state.equals("checking")||state.equals("sending")||state.equals("uncertain");}
    static boolean mayClaim(String state,long age){return state.equals("checking")&&age>=0&&age<=120000;}
}
