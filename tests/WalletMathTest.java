package app.shazin.aharemen;

public final class WalletMathTest {
    static void eq(String expected, String value) { if(!expected.equals(value))throw new AssertionError(value); }
    public static void main(String[] args) {
        eq("0.25", WalletMath.cost(new int[]{25,0,0},new double[]{.01,.1,.01}).toPlainString());
        eq("0.8", WalletMath.cost(new int[]{25,3,25},new double[]{.01,.1,.01}).stripTrailingZeros().toPlainString());
        if(WalletMath.maxUnits(.25,.01)!=25)throw new AssertionError("Fractional RB maximum");
        if(WalletMath.maxUnits(.3,.1)!=3)throw new AssertionError("Decimal boundary");
        eq("7716176",WalletMath.mobile("+960 771-6176"));
        eq("7716176",WalletMath.mobile("00960 (771) 6176"));
        eq("7716176",WalletMath.mobile("9607716176"));
        try {WalletMath.mobile("771617699");throw new AssertionError("Invalid number accepted");}catch(IllegalArgumentException expected){}
        if(Math.abs(Allowance.preciseData(75027)-73.2685546875)>1e-9)throw new AssertionError("Data precision");
        if(Math.abs(Allowance.preciseVoice(520)-8.666666666666666)>1e-9)throw new AssertionError("Voice precision");
        System.out.println("10 fractional wallet, precision and phone normalization checks passed.");
    }
}
