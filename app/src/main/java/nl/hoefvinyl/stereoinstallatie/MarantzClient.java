package nl.hoefvinyl.stereoinstallatie;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public final class MarantzClient {
    private MarantzClient() {}

    public static boolean isReachable(String ip) {
        if (ip == null || ip.trim().isEmpty()) return false;
        HttpURLConnection c = null;
        try {
            c = (HttpURLConnection) new URL("http://" + ip + "/").openConnection();
            c.setConnectTimeout(900);
            c.setReadTimeout(900);
            c.setInstanceFollowRedirects(true);
            c.setRequestMethod("GET");
            int code = c.getResponseCode();
            if (code < 200 || code >= 500) return false;
            String text = readSome(c);
            return text.isEmpty() || text.toLowerCase().contains("marantz") || text.toLowerCase().contains("na8005") || code == 200;
        } catch (Exception e) {
            return false;
        } finally {
            if (c != null) c.disconnect();
        }
    }

    public static boolean powerOn(String ip) {
        return command(ip, "/goform/formiPhoneAppPower.xml?1+PowerOn");
    }

    public static boolean standby(String ip) {
        return command(ip, "/goform/formiPhoneAppPower.xml?1+PowerStandby");
    }

    private static boolean command(String ip, String path) {
        HttpURLConnection c = null;
        try {
            c = (HttpURLConnection) new URL("http://" + ip + path).openConnection();
            c.setConnectTimeout(1500);
            c.setReadTimeout(1500);
            c.setRequestMethod("GET");
            int code = c.getResponseCode();
            return code >= 200 && code < 400;
        } catch (Exception e) {
            return false;
        } finally {
            if (c != null) c.disconnect();
        }
    }

    private static String readSome(HttpURLConnection c) {
        try (BufferedReader r = new BufferedReader(new InputStreamReader(c.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder b = new StringBuilder();
            String line;
            int n = 0;
            while ((line = r.readLine()) != null && n++ < 40) b.append(line).append('\n');
            return b.toString();
        } catch (Exception e) {
            return "";
        }
    }
}
