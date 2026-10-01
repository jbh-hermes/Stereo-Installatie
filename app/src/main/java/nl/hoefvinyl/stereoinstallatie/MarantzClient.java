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
        String xml = postAppCommand(ip, "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<tx>\n <cmd id=\"1\">GetSystemFavoriteList</cmd>\n</tx>");
        List<Favorite> out = new ArrayList<>();
        if (xml == null) return out;

        Pattern fav = Pattern.compile("<favorite(?:\\s[^>]*)?>(.*?)</favorite>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher m = fav.matcher(xml);
        while (m.find()) {
            String b = m.group(1);
            String pos = firstNonBlank(tag(b,"position"), tag(b,"Position"), tag(b,"id"), tag(b,"value"));
            String name = firstNonBlank(tag(b,"name"), tag(b,"Name"), tag(b,"title"), tag(b,"Title"));
            if (!name.isEmpty()) out.add(new Favorite(normalizePosition(pos, out.size()+1), clean(name)));
        }

        if (out.isEmpty()) {
            Pattern item = Pattern.compile("<Item[^>]*>(.*?)</Item>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
            Matcher im = item.matcher(xml);
            int i=1;
            while (im.find()) {
                String b=im.group(1);
                String pos=firstNonBlank(tag(b,"position"),tag(b,"value"),String.format("%02d",i));
                String name=firstNonBlank(tag(b,"name"),tag(b,"title"),tag(b,"szLine"));
                if(!name.isEmpty()) { out.add(new Favorite(normalizePosition(pos,i),clean(name))); i++; }
            }
        }
        return out;
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
