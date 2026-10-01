package nl.hoefvinyl.stereoinstallatie;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MarantzClient {
    private MarantzClient() {}

    public static final class NowPlaying {
        public final String station, artist, title;
        public NowPlaying(String station, String artist, String title) {
            this.station = station; this.artist = artist; this.title = title;
        }
    }

    public static final class Favorite {
        public final String position, name;
        public Favorite(String position, String name) {
            this.position = position; this.name = name;
        }
    }

    public static boolean isReachable(String ip) {
        if (ip == null || ip.trim().isEmpty()) return false;
        HttpURLConnection c = null;
        try {
            c = open("http://" + ip + "/", "GET", 900, 900);
            int code = c.getResponseCode();
            return code >= 200 && code < 500;
        } catch (Exception e) {
            return false;
        } finally { if (c != null) c.disconnect(); }
    }

    public static boolean powerOn(String ip) {
        return getOk(ip, "/goform/formiPhoneAppPower.xml?1+PowerOn");
    }

    public static boolean standby(String ip) {
        return getOk(ip, "/goform/formiPhoneAppPower.xml?1+PowerStandby");
    }

    public static boolean mute(String ip, boolean on) {
        return getOk(ip, "/goform/formiPhoneAppMute.xml?1+" + (on ? "MuteOn" : "MuteOff"));
    }

    public static boolean volumeUp(String ip) {
        return getOk(ip, "/goform/formiPhoneAppVolume.xml?1+>");
    }

    public static boolean volumeDown(String ip) {
        return getOk(ip, "/goform/formiPhoneAppVolume.xml?1+<");
    }

    public static boolean selectSource(String ip, String command) {
        return getOk(ip, "/goform/formiPhoneAppDirect.xml?" + command);
    }

    public static NowPlaying fetchNowPlaying(String ip) {
        String xml = getText(ip, "/goform/formNetAudio_StatusXml.xml");
        if (xml == null || xml.isEmpty()) xml = getText(ip, "/goform/formiPhoneAppNetAudio_StatusXml.xml");
        if (xml == null) return new NowPlaying("Nog geen radiostation", "", "");
        String station = firstNonBlank(
                tag(xml, "szLine", 0), tag(xml, "station"), tag(xml, "StationName"),
                tag(xml, "funcname"), tag(xml, "FunctionName"));
        String artist = firstNonBlank(tag(xml, "artist"), tag(xml, "Artist"), tag(xml, "szLine", 1));
        String title = firstNonBlank(tag(xml, "title"), tag(xml, "Title"), tag(xml, "szLine", 2));
        if (station.isEmpty()) station = "Nog geen radiostation";
        return new NowPlaying(clean(station), clean(artist), clean(title));
    }

    public static List<Favorite> fetchFavorites(String ip) {
        List<Favorite> out = new ArrayList<>();

        String xml = postAppCommand(ip,
                "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<tx>\n" +
                " <cmd id=\"1\">GetSystemFavoriteList</cmd>\n" +
                "</tx>");

        parseFavoritesPayload(xml, out);

        // Older NA-series units can also expose the list through the legacy FV query.
        if (out.isEmpty()) {
            String legacy = getText(ip, "/goform/formiPhoneAppDirect.xml?FV%20?");
            parseFavoritesPayload(legacy, out);
        }

        return out;
    }

    private static void parseFavoritesPayload(String payload, List<Favorite> out) {
        if (payload == null || payload.trim().isEmpty()) return;

        // Exact pattern used by the original app: each <favorite> contains repeated <value> fields.
        Pattern favoriteBlock = Pattern.compile(
                "<favorite(?:\\s[^>]*)?>(.*?)</favorite>",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher fm = favoriteBlock.matcher(payload);
        int favoriteFallback = 1;
        Pattern valuePattern = Pattern.compile(
                "<value[^>]*>(.*?)</value>",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        while (fm.find()) {
            String b = fm.group(1);
            Matcher vm = valuePattern.matcher(b);
            List<String> values = new ArrayList<>();
            while (vm.find()) values.add(clean(vm.group(1)));

            String pos = "";
            String name = "";
            for (String v : values) {
                if (v == null || v.trim().isEmpty()) continue;
                String trimmed = v.trim();
                if (pos.isEmpty() && trimmed.matches("\\d{1,3}")) {
                    pos = trimmed;
                } else if (name.isEmpty() && !trimmed.matches("\\d{1,3}")) {
                    name = trimmed;
                }
            }

            // On some NA8005 firmware the first value is the position and the second is the station name.
            if (pos.isEmpty() && values.size() >= 1) pos = values.get(0);
            if (name.isEmpty() && values.size() >= 2) name = values.get(1);

            if (!name.isEmpty()) {
                out.add(new Favorite(normalizePosition(pos, favoriteFallback++), clean(name)));
            }
        }
        if (!out.isEmpty()) return;

        // Additional formats seen on related Marantz firmware.
        Pattern block = Pattern.compile(
                "<(?:item|list)(?:\\s[^>]*)?>(.*?)</(?:item|list)>",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher bm = block.matcher(payload);
        int fallback = 1;
        while (bm.find()) {
            String b = bm.group(1);
            String pos = firstNonBlank(
                    tag(b, "position"), tag(b, "Position"),
                    tag(b, "id"), tag(b, "value"),
                    tag(b, "Value"), tag(b, "index"), tag(b, "Index"));
            String name = firstNonBlank(
                    tag(b, "name"), tag(b, "Name"),
                    tag(b, "title"), tag(b, "Title"),
                    tag(b, "szLine"), tag(b, "text"), tag(b, "Text"));
            if (!name.isEmpty()) {
                out.add(new Favorite(normalizePosition(pos, fallback++), clean(name)));
            }
        }

        if (!out.isEmpty()) return;

        // Some firmware returns repeated <value>01</value><name>Station</name>.
        Matcher pair = Pattern.compile(
                "<value>(.*?)</value>\\s*<(?:name|title|text)>(.*?)</(?:name|title|text)>",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(payload);
        while (pair.find()) {
            String pos = clean(pair.group(1));
            String name = clean(pair.group(2));
            if (!name.isEmpty()) out.add(new Favorite(normalizePosition(pos, fallback++), name));
        }

        if (!out.isEmpty()) return;

        // Legacy FV status can be plain text. Accept lines like "FV 01 Station Name".
        String[] lines = payload.split("\\r?\\n");
        Pattern legacy = Pattern.compile("^\\s*(?:FV\\s*)?(\\d{1,2})[\\s:=,-]+(.+?)\\s*$", Pattern.CASE_INSENSITIVE);
        for (String line : lines) {
            Matcher lm = legacy.matcher(line);
            if (lm.find()) {
                String name = clean(lm.group(2));
                if (!name.isEmpty()) out.add(new Favorite(normalizePosition(lm.group(1), fallback++), name));
            }
        }
    }

    public static boolean callFavorite(String ip, String position) {
        String p = position == null ? "" : position.trim();
        return getOk(ip, "/goform/formiPhoneAppFavorite_Call.xml?" + url(p));
    }

    public static boolean deleteFavorite(String ip, String position) {
        String xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<tx>\n <cmd id=\"1\">SetDeleteSystemFavorite</cmd>\n <value>" +
                escapeXml(position) + "</value>\n</tx>";
        String r = postAppCommand(ip, xml);
        return r != null && (r.contains("<cmd>OK</cmd>") || r.toLowerCase().contains("ok"));
    }

    public static boolean addCurrentToFavorite(String ip, String position) {
        String xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<tx>\n <cmd id=\"1\">SetAddToSystemFavorite</cmd>\n <value>" +
                escapeXml(position) + "</value>\n</tx>";
        String r = postAppCommand(ip, xml);
        return r != null && (r.contains("<cmd>OK</cmd>") || r.toLowerCase().contains("ok"));
    }

    private static String postAppCommand(String ip, String body) {
        HttpURLConnection c = null;
        try {
            c = open("http://" + ip + "/goform/AppCommand.xml", "POST", 1600, 2200);
            c.setRequestProperty("Content-Type", "text/xml; charset=utf-8");
            c.setDoOutput(true);
            byte[] b=body.getBytes(StandardCharsets.UTF_8);
            c.setFixedLengthStreamingMode(b.length);
            try(OutputStream os=c.getOutputStream()) { os.write(b); }
            int code=c.getResponseCode();
            if(code<200 || code>=500) return null;
            return readAll(code>=400?c.getErrorStream():c.getInputStream());
        } catch(Exception e) { return null; }
        finally { if(c!=null)c.disconnect(); }
    }

    private static boolean getOk(String ip, String path) {
        HttpURLConnection c=null;
        try {
            c=open("http://"+ip+path,"GET",1400,1600);
            int code=c.getResponseCode();
            if(code>=200&&code<400) { InputStream is=c.getInputStream(); if(is!=null)is.close(); return true; }
            return false;
        } catch(Exception e) { return false; }
        finally { if(c!=null)c.disconnect(); }
    }

    private static String getText(String ip, String path) {
        HttpURLConnection c=null;
        try {
            c=open("http://"+ip+path,"GET",1200,1700);
            if(c.getResponseCode()<200||c.getResponseCode()>=500)return null;
            return readAll(c.getInputStream());
        } catch(Exception e){return null;}
        finally {if(c!=null)c.disconnect();}
    }

    private static HttpURLConnection open(String url, String method, int connect, int read) throws Exception {
        HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();
        c.setConnectTimeout(connect); c.setReadTimeout(read); c.setRequestMethod(method);
        c.setUseCaches(false); c.setRequestProperty("Connection","close");
        return c;
    }

    private static String readAll(InputStream in) throws Exception {
        if(in==null)return "";
        try(BufferedReader r=new BufferedReader(new InputStreamReader(in,StandardCharsets.UTF_8))) {
            StringBuilder b=new StringBuilder(); String line;
            while((line=r.readLine())!=null)b.append(line).append('\n');
            return b.toString();
        }
    }

    private static String tag(String xml,String name){ return tag(xml,name,0); }
    private static String tag(String xml,String name,int index){
        if(xml==null)return "";
        Matcher m=Pattern.compile("<"+Pattern.quote(name)+"(?:\\s[^>]*)?>(.*?)</"+Pattern.quote(name)+">",Pattern.CASE_INSENSITIVE|Pattern.DOTALL).matcher(xml);
        int i=0; while(m.find()){ if(i++==index)return strip(m.group(1)); } return "";
    }
    private static String strip(String s){ return s==null?"":s.replaceAll("<[^>]+>","").replace("&amp;","&").replace("&lt;","<").replace("&gt;",">").replace("&quot;","\"").trim(); }
    private static String clean(String s){ return strip(s).replace("\u0000","").trim(); }
    private static String firstNonBlank(String... xs){ for(String s:xs)if(s!=null&&!s.trim().isEmpty())return s.trim(); return ""; }
    private static String normalizePosition(String p,int fallback){
        String digits=p==null?"":p.replaceAll("[^0-9]","");
        if(digits.isEmpty())return String.format("%02d",fallback);
        try{return String.format("%02d",Integer.parseInt(digits));}catch(Exception e){return digits;}
    }
    private static String url(String s){ try{return URLEncoder.encode(s, "UTF-8");}catch(Exception e){return s;} }
    private static String escapeXml(String s){return s==null?"":s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;");}
}
