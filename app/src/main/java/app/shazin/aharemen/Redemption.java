package app.shazin.aharemen;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** The original app multiplies allowance units by the server's RB-per-unit rate. */
final class Redemption {
    static int units(String budget, double rate, double balance) {
        BigDecimal rb = new BigDecimal(budget);
        if (rb.signum() <= 0 || !Double.isFinite(rate) || rate <= 0
                || !Double.isFinite(balance) || balance < 0) {
            throw new IllegalArgumentException("Enter a positive RB amount and refresh your balances.");
        }
        BigDecimal unitCost = BigDecimal.valueOf(rate);
        int units = rb.divide(unitCost, 0, RoundingMode.DOWN).intValueExact();
        if (units <= 0 || unitCost.multiply(BigDecimal.valueOf(units))
                .compareTo(BigDecimal.valueOf(balance)) > 0) {
            throw new IllegalArgumentException("You don’t have enough RB for this amount.");
        }
        return units;
    }
}
