package app.shazin.aharemen;

import java.math.BigDecimal;
import java.math.RoundingMode;

final class WalletMath {
    static BigDecimal cost(int[] units, double[] rates) {
        BigDecimal result = BigDecimal.ZERO;
        for (int i = 0; i < 3; i++) {
            if (units[i] < 0 || !Double.isFinite(rates[i]) || rates[i] <= 0)
                throw new IllegalArgumentException("Refresh your conversion rates and choose valid amounts.");
            result = result.add(BigDecimal.valueOf(rates[i]).multiply(BigDecimal.valueOf(units[i])));
        }
        return result;
    }
    static int maxUnits(double rb, double rate) {
        if (!Double.isFinite(rb) || rb <= 0 || !Double.isFinite(rate) || rate <= 0) return 0;
        return BigDecimal.valueOf(rb).divide(BigDecimal.valueOf(rate), 0, RoundingMode.DOWN)
                .min(BigDecimal.valueOf(1000000)).intValue();
    }
    static String mobile(String value) {
        String n = value.replaceAll("[\\s()\\-]", "");
        if (n.startsWith("+960")) n = n.substring(4);
        else if (n.startsWith("00960")) n = n.substring(5);
        else if (n.length() == 10 && n.startsWith("960")) n = n.substring(3);
        if (!n.matches("[79][0-9]{6}")) throw new IllegalArgumentException("Enter a valid seven-digit mobile number.");
        return n;
    }
}
