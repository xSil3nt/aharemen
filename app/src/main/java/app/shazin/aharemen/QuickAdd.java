package app.shazin.aharemen;

import java.math.BigDecimal;

/** Fixed widget amounts; live rates must not exceed the displayed RB quote. */
final class QuickAdd {
    static int units(int kind,double[] rates){
        if(kind<0||kind>2||rates==null||rates.length!=3||!Double.isFinite(rates[kind])||rates[kind]<=0)throw new IllegalArgumentException("Refresh rates in aharemen.");
        return new int[]{25,3,10}[kind];
    }
    static BigDecimal validate(int kind,int amount,BigDecimal quotedCost,double[] rates,double balance){
        if(kind<0||kind>2||quotedCost==null||quotedCost.signum()<=0||amount!=units(kind,rates))throw new IllegalArgumentException("Open aharemen to update this shortcut.");
        if(!Double.isFinite(balance)||balance<0)throw new IllegalArgumentException("Refresh balances in aharemen.");
        int[] selected={0,0,0};selected[kind]=amount;
        BigDecimal cost=WalletMath.cost(selected,rates);
        if(cost.compareTo(quotedCost)>0)throw new IllegalArgumentException("Rates changed. Refresh before adding.");
        if(cost.compareTo(BigDecimal.valueOf(balance))>0)throw new IllegalArgumentException("Not enough RB. Top up in aharemen.");
        return cost;
    }
    static boolean blocks(String state){return state.equals("queued")||state.equals("checking")||state.equals("sending")||state.equals("uncertain");}
    static boolean mayClaim(String state,long age){return state.equals("checking")&&age>=0&&age<=120000;}
}
