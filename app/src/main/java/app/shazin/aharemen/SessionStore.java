package app.shazin.aharemen;

import android.content.SharedPreferences;
import android.security.keystore.*;
import android.util.Base64;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;

final class SessionStore {
    private final SharedPreferences prefs;
    SessionStore(SharedPreferences prefs) { this.prefs = prefs; }
    private SecretKey key() throws Exception {
        KeyStore store = KeyStore.getInstance("AndroidKeyStore"); store.load(null);
        // Preserve the first build's private key alias across the rename.
        if (!store.containsAlias("mamen-session")) {
            KeyGenerator g = KeyGenerator.getInstance("AES", "AndroidKeyStore");
            g.init(new KeyGenParameterSpec.Builder("mamen-session", KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes("GCM").setEncryptionPaddings("NoPadding").build());
            g.generateKey();
        }
        return (SecretKey) store.getKey("mamen-session", null);
    }
    void save(String token) throws Exception {
        Cipher c = Cipher.getInstance("AES/GCM/NoPadding"); c.init(Cipher.ENCRYPT_MODE, key());
        if (!prefs.edit().putString("session", Base64.encodeToString(c.doFinal(token.getBytes(StandardCharsets.UTF_8)), Base64.NO_WRAP))
                .putString("iv", Base64.encodeToString(c.getIV(), Base64.NO_WRAP)).commit()) throw new Exception("Could not save the session.");
    }
    String load() throws Exception {
        String value = prefs.getString("session", ""); if (value.isEmpty()) return "";
        Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
        c.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(128, Base64.decode(prefs.getString("iv", ""), Base64.NO_WRAP)));
        return new String(c.doFinal(Base64.decode(value, Base64.NO_WRAP)), StandardCharsets.UTF_8);
    }



    void clear() { prefs.edit().remove("session").remove("iv").apply(); }
}
