package app.shazin.aharemen;

import org.json.JSONObject;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

final class ApiClient {
    static final String BASE = "https://ybaas.dhiraagu.com.mv/api/";
    static JSONObject call(String token, String path, String method, String data, boolean form) throws Exception {
        URI uri = new URI(BASE).resolve(path);
        if (!"https".equals(uri.getScheme()) || !"ybaas.dhiraagu.com.mv".equals(uri.getHost())
                || uri.getPort() != -1 || !uri.getPath().startsWith("/api/")) {
            throw new IOException("Unexpected server address.");
        }
        HttpURLConnection c = (HttpURLConnection) uri.toURL().openConnection();
        try {
            c.setConnectTimeout(20000);
            c.setReadTimeout(25000);
            c.setInstanceFollowRedirects(false);
            c.setRequestMethod(method);
            c.setRequestProperty("Accept", "application/json");
            if (!token.isEmpty()) c.setRequestProperty("Authorization", "Bearer " + token);
            if (data != null) {
                c.setDoOutput(true);
                c.setRequestProperty("Content-Type", form ? "application/x-www-form-urlencoded" : "application/json; charset=utf-8");
                try (OutputStream out = c.getOutputStream()) { out.write(data.getBytes(StandardCharsets.UTF_8)); }
            }
            int status = c.getResponseCode();
            InputStream stream = status >= 400 ? c.getErrorStream() : c.getInputStream();
            if (stream == null) throw new IOException("Server returned HTTP " + status);
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (InputStream in = stream) {
                byte[] buffer = new byte[4096]; int count;
                while ((count = in.read(buffer)) != -1) {
                    if (bytes.size() + count > 2000000) throw new IOException("Server response was too large.");
                    bytes.write(buffer, 0, count);
                }
            }
            JSONObject result;
            try { result = new JSONObject(bytes.toString("UTF-8")); }
            catch (Exception e) { throw new IOException("Unexpected server response (HTTP " + status + ")."); }
            if (status < 200 || status >= 300 || !result.optBoolean("success", true)) {
                Object error = result.opt("error");
                String message = error instanceof String ? (String) error : result.optString("message", "Request failed (HTTP " + status + ").");
                throw new IOException(message);
            }
            return result;
        } finally { c.disconnect(); }
    }
    static String form(String... pairs) {
        StringBuilder b = new StringBuilder();
        try {
            for (int i = 0; i < pairs.length; i += 2) {
                if (i > 0) b.append('&');
                b.append(URLEncoder.encode(pairs[i], "UTF-8")).append('=').append(URLEncoder.encode(pairs[i+1], "UTF-8"));
            }
        } catch (Exception e) { throw new IllegalArgumentException(e); }
        return b.toString();
    }
}
