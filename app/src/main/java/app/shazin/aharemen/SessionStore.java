package app.shazin.aharemen;

import android.content.SharedPreferences;
import android.security.keystore.*;
import android.util.Base64;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;

final class SessionStore {
    static final Object LOCK=new Object();
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
    synchronized void saveSnapshot(String value) throws Exception {
        Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.ENCRYPT_MODE,key());
        prefs.edit().putString("account_cache",Base64.encodeToString(c.doFinal(value.getBytes(StandardCharsets.UTF_8)),Base64.NO_WRAP))
            .putString("account_cache_iv",Base64.encodeToString(c.getIV(),Base64.NO_WRAP)).apply();
    }
    synchronized String loadSnapshot() throws Exception {
        String value=prefs.getString("account_cache","");if(value.isEmpty())return "";
        Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.DECRYPT_MODE,key(),new GCMParameterSpec(128,Base64.decode(prefs.getString("account_cache_iv",""),Base64.NO_WRAP)));
        return new String(c.doFinal(Base64.decode(value,Base64.NO_WRAP)),StandardCharsets.UTF_8);
    }
    boolean saveSnapshotIfCurrent(String token,String snapshot)throws Exception {
        synchronized(LOCK){if(token.isEmpty()||!token.equals(load()))return false;saveSnapshot(snapshot);return true;}
    }
    synchronized void clearSnapshot(){prefs.edit().remove("account_cache").remove("account_cache_iv").apply();}
    void clear() { prefs.edit().remove("session").remove("iv").apply(); }
}
