package app.shazin.aharemen;

public final class AllowanceTest {
    static void eq(long expected, long actual) {
        if (expected != actual) throw new AssertionError(expected + " != " + actual);
    }
    public static void main(String[] args) {
        eq(73, Allowance.data(75027));
        eq(8, Allowance.voice(520));
        eq(0, Allowance.voice(59));
        eq(1, Allowance.voice(60));
        eq(0, Allowance.data(511));
        eq(1, Allowance.data(512));
        eq(1024, Allowance.data(1048576));
        eq(3, Allowance.totalData(512, 512, 512));
        eq(104, Allowance.totalData(75027, 10240, 21504));
        System.out.println("9 original-app allowance conversion and aggregation checks passed.");
    }
}
