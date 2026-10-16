package app.shazin.aharemen;

import java.util.Map;

/** Accept only a valid server account for a number we actually looked up. */
final class ContactMatch {
    static boolean eligible(Map<String,String> localNames, String serverPhone, int id, int ownId, String ownPhone) {
        if (id <= 0 || id == ownId) return false;
        try {
            String normalized = WalletMath.mobile(serverPhone);
            return !normalized.equals(ownPhone) && localNames.containsKey(normalized);
        } catch (IllegalArgumentException e) { return false; }
    }
}
