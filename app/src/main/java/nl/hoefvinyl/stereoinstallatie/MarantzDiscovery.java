package nl.hoefvinyl.stereoinstallatie;

import android.content.Context;
import android.net.wifi.WifiManager;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

public final class MarantzDiscovery {
    private MarantzDiscovery() {}

    public static String findNa8005(Context context) {
        WifiManager.MulticastLock lock = null;
        DatagramSocket socket = null;
        try {
            WifiManager wm = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wm != null) {
                lock = wm.createMulticastLock("StereoInstallatie-SSDP");
                lock.setReferenceCounted(false);
                lock.acquire();
            }

            socket = new DatagramSocket();
            socket.setReuseAddress(true);
            socket.setSoTimeout(700);

            String request = "M-SEARCH * HTTP/1.1\r\n" +
                    "HOST: 239.255.255.250:1900\r\n" +
                    "MAN: \"ssdp:discover\"\r\n" +
                    "MX: 2\r\n" +
                    "ST: ssdp:all\r\n\r\n";
            byte[] data = request.getBytes(StandardCharsets.UTF_8);
            DatagramPacket out = new DatagramPacket(data, data.length,
                    InetAddress.getByName("239.255.255.250"), 1900);

            socket.send(out);
            Thread.sleep(150);
            socket.send(out);

            long end = System.currentTimeMillis() + 4200;
            Set<String> locations = new LinkedHashSet<>();
            byte[] buf = new byte[8192];

            while (System.currentTimeMillis() < end) {
                try {
                    DatagramPacket in = new DatagramPacket(buf, buf.length);
                    socket.receive(in);
                    String response = new String(in.getData(), 0, in.getLength(), StandardCharsets.UTF_8);
                    String lower = response.toLowerCase(Locale.ROOT);
                    String location = headerValue(response, "location");

                    if ((lower.contains("marantz") || lower.contains("na8005")) && location != null) {
                        String ip = ipFromLocation(location);
                        if (MarantzClient.isReachable(ip)) return ip;
                    }
                    if (location != null) locations.add(location);
                } catch (java.net.SocketTimeoutException ignored) {
                }
            }

            for (String location : locations) {
                if (isNa8005Description(location)) {
                    String ip = ipFromLocation(location);
                    if (MarantzClient.isReachable(ip)) return ip;
                }
            }
        } catch (Exception ignored) {
        } finally {
            if (socket != null) socket.close();
            if (lock != null && lock.isHeld()) lock.release();
        }
        return null;
    }

    private static boolean isNa8005Description(String location) {
        HttpURLConnection c = null;
        try {
            c = (HttpURLConnection) new URL(location).openConnection();
            c.setConnectTimeout(900);
            c.setReadTimeout(900);
            int code = c.getResponseCode();
            if (code < 200 || code >= 400) return false;
            try (BufferedReader r = new BufferedReader(new InputStreamReader(c.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder b = new StringBuilder();
                String line;
                int n = 0;
                while ((line = r.readLine()) != null && n++ < 120) b.append(line).append('\n');
                String s = b.toString().toLowerCase(Locale.ROOT);
                return s.contains("na8005") || (s.contains("marantz") && s.contains("friendlyname"));
            }
        } catch (Exception e) {
            return false;
        } finally {
            if (c != null) c.disconnect();
        }
    }

    private static String headerValue(String response, String wanted) {
        String[] lines = response.split("\\r?\\n");
        for (String line : lines) {
            int p = line.indexOf(':');
            if (p > 0 && line.substring(0, p).trim().equalsIgnoreCase(wanted)) {
                return line.substring(p + 1).trim();
            }
        }
        return null;
    }

    private static String ipFromLocation(String location) {
        try {
            return new URI(location).getHost();
        } catch (Exception e) {
            return null;
        }
    }
}
