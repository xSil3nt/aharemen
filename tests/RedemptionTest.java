package app.shazin.aharemen;

public final class RedemptionTest {
    static void eq(int expected, int actual) {
        if (expected != actual) throw new AssertionError(expected + " != " + actual);
    }
    static void rejects(String budget, double rate, double balance) {
        try { Redemption.units(budget, rate, balance); }
        catch (IllegalArgumentException | ArithmeticException expected) { return; }
        throw new AssertionError("Invalid redemption was accepted");
    }
    public static void main(String[] args) {
        eq(100, Redemption.units("1", 0.01, 1));
        eq(10, Redemption.units("1", 0.1, 1));
        eq(30, Redemption.units("0.3", 0.01, 0.3));
        eq(33, Redemption.units("1", 0.03, 0.99));
        eq(1, Redemption.units("1.9", 1, 1));
        rejects("1", 0.01, 0.99);
        rejects("0.001", 0.01, 10);
        rejects("-1", 0.01, 10);
        rejects("1", 0, 10);
        rejects("1", Double.NaN, 10);
        rejects("1", 0.01, -1);
        rejects("999999999999999", 0.01, 1e20);
        System.out.println("12 redemption conversion and boundary checks passed.");
    }
}
