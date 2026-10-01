package nl.hoefvinyl.stereoinstallatie;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private static final String PREFS = "stereo_installatie";
    private static final String KEY_IP = "marantz_ip";
    private static final String DEFAULT_IP = "192.168.68.73";

    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private SharedPreferences prefs;
    private String marantzIp;
    private LinearLayout body;
    private TextView status;
    private TextView ipText;
    private ProgressBar progress;

    private final int bg = Color.rgb(18, 18, 18);
    private final int panel = Color.rgb(38, 38, 38);
    private final int accent = Color.rgb(205, 154, 54);
    private final int text = Color.rgb(242, 242, 242);
    private final int muted = Color.rgb(180, 180, 180);

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        marantzIp = prefs.getString(KEY_IP, DEFAULT_IP);
        setContentView(buildApp());
        showStart();
        resolveMarantz(false);
    }

    private View buildApp() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(bg);

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.VERTICAL);
        top.setPadding(dp(18), dp(18), dp(18), dp(10));
        top.setBackgroundColor(Color.rgb(27,27,27));

        TextView title = new TextView(this);
        title.setText("Stereo-installatie");
        title.setTextSize(26);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(text);
        top.addView(title);

        TextView sub = new TextView(this);
        sub.setText("Marantz Hi-Fi Remote");
        sub.setTextSize(14);
        sub.setTextColor(accent);
        top.addView(sub);
        root.addView(top, new LinearLayout.LayoutParams(-1,-2));

        status = new TextView(this);
        status.setTextColor(muted);
        status.setTextSize(14);
        status.setPadding(dp(18), dp(8), dp(18), 0);
        root.addView(status);

        ipText = new TextView(this);
        ipText.setTextColor(muted);
        ipText.setTextSize(13);
        ipText.setPadding(dp(18), 0, dp(18), dp(6));
        root.addView(ipText);

        progress = new ProgressBar(this);
        root.addView(progress, new LinearLayout.LayoutParams(-1, dp(4)));

        ScrollView scroll = new ScrollView(this);
        body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(16), dp(12), dp(16), dp(16));
        scroll.addView(body);
        root.addView(scroll, new LinearLayout.LayoutParams(-1,0,1f));

        root.addView(buildNav(), new LinearLayout.LayoutParams(-1,-2));
        return root;
    }

    private View buildNav() {
        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setPadding(dp(6), dp(6), dp(6), dp(8));
        nav.setBackgroundColor(Color.rgb(25,25,25));
        nav.addView(navBtn("Start", v -> showStart()), weight());
        nav.addView(navBtn("Versterker", v -> showAmplifier()), weight());
        nav.addView(navBtn("Bronnen", v -> showSources()), weight());
        nav.addView(navBtn("Radio", v -> showRadio()), weight());
        nav.addView(navBtn("Favorieten", v -> showFavorites()), weight());
        nav.addView(navBtn("Instellingen", v -> showSettings()), weight());
        return nav;
    }

    private Button navBtn(String s, View.OnClickListener l) {
        Button b = new Button(this);
        b.setText(s);
        b.setTextSize(11);
        b.setAllCaps(false);
        b.setTextColor(text);
        b.setBackgroundColor(Color.TRANSPARENT);
        b.setPadding(2,0,2,0);
        b.setOnClickListener(l);
        return b;
    }

    private void pageTitle(String t, String s) {
        body.removeAllViews();
        TextView h = new TextView(this);
        h.setText(t);
        h.setTextSize(23);
        h.setTypeface(Typeface.DEFAULT_BOLD);
        h.setTextColor(text);
        body.addView(h, lp(-1,-2,0,0,0,4));
        if (s != null && !s.isEmpty()) {
            TextView d = new TextView(this);
            d.setText(s);
            d.setTextSize(14);
            d.setTextColor(muted);
            body.addView(d, lp(-1,-2,0,0,0,14));
        }
    }

    private void showStart() {
        pageTitle("Start", "Bronnen en volledige bediening");
        cardButton("Alles aan", "Alles inschakelen", v -> sendPower(true));
        cardButton("NA8005 aan", "Marantz netwerkspeler inschakelen", v -> sendPower(true));
        cardButton("NA8005 uit", "NA8005 naar stand-by", v -> sendPower(false));
        cardButton("Versterker", "PM8003 versterker", v -> showAmplifier());
        cardButton("Bronnen", "Kies wat je wilt beluisteren", v -> showSources());
        cardButton("Radio", "Internetradio en radiostations zoeken", v -> showRadio());
        cardButton("Favorieten", "Marantz-favorieten", v -> showFavorites());
    }

    private void showAmplifier() {
        pageTitle("Versterker", "PM8003 versterker");
        cardButton("PM8003 aan", "Versterker inschakelen", v -> toast("Deze knop wordt in de volgende stap aan de oude PM8003-opdracht gekoppeld."));
        cardButton("PM8003 stand-by", "Versterker uitschakelen", v -> toast("Deze knop wordt in de volgende stap aan de oude PM8003-opdracht gekoppeld."));
        section("Ingang kiezen");
        smallRow(new String[]{"Phono","CD","Tuner","AUX"});
        section("Volume");
        smallRow(new String[]{"Volume zachter","Geluid aan","Volume harder"});
    }

    private void showSources() {
        pageTitle("Bronnen", "Kies wat je wilt beluisteren");
        cardButton("Internet Radio", "Internetradio", v -> showRadio());
        cardButton("Muziekserver / NAS", "Muziekserver", v -> toast("Muziekserver gekozen"));
        cardButton("USB", "USB", v -> toast("USB gekozen"));
        cardButton("Digitaal optisch", "Optische ingang", v -> toast("Bron wordt gekozen"));
        cardButton("Digitaal coax", "Coaxiale ingang", v -> toast("Bron wordt gekozen"));
    }

    private void showRadio() {
        pageTitle("Radio", "Internet Radio");
        cardButton("Nu op de radio", "Now Playing", v -> openWeb());
        cardButton("Mijn toegevoegde radiostations", "vTuner stations", v -> openWeb());
        cardButton("Aanbevolen Radiostations", "Kies een radiostation", v -> openWeb());
        cardButton("Radiostations zoeken", "Zoeken bij vTuner", v -> openWeb());
        TextView note = label("De radiobediening uit de oude APK is herkend. In deze eerste herbouw openen deze onderdelen de werkende NA8005-webbediening; daarna zetten we de directe radiocommando's terug.");
        body.addView(note, lp(-1,-2,0,12,0,0));
    }

    private void showFavorites() {
        pageTitle("Favorieten", "Marantz-favorieten");
        cardButton("Favorietenplaatsen ophalen", "Lees favorieten uit de NA8005", v -> openWeb());
        cardButton("Marantz-favorieten opnieuw ophalen", "Vernieuw de lijst", v -> openWeb());
        cardButton("Zender bewaren", "Opslaan op Marantz-positie", v -> openWeb());
    }

    private void showSettings() {
        pageTitle("Instellingen", "Marantz NA8005 zoeken op het netwerk");
        cardButton("Marantz opnieuw zoeken", "Automatisch zoeken via UPnP/SSDP", v -> resolveMarantz(true));
        cardButton("Webbediening openen", "Volledige NA8005-bediening", v -> openWeb());
        TextView ip = label("Handmatig IP-adres\nHuidig: " + (marantzIp == null ? "onbekend" : marantzIp));
        body.addView(ip, lp(-1,-2,0,10,0,10));
        TextView help = label("De app onthoudt het laatst werkende IP-adres. Reageert dat adres niet meer, dan zoekt hij automatisch opnieuw naar de NA8005 en slaat het nieuwe adres op.");
        body.addView(help);
    }

    private void cardButton(String title, String subtitle, View.OnClickListener l) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(12), dp(16), dp(12));
        card.setBackgroundColor(panel);
        card.setOnClickListener(l);

        TextView a = new TextView(this);
        a.setText(title);
        a.setTextColor(text);
        a.setTextSize(18);
        a.setTypeface(Typeface.DEFAULT_BOLD);
        card.addView(a);

        TextView b = new TextView(this);
        b.setText(subtitle);
        b.setTextColor(muted);
        b.setTextSize(13);
        card.addView(b);
        body.addView(card, lp(-1,-2,0,0,0,10));
    }

    private void section(String s) {
        TextView v = new TextView(this);
        v.setText(s);
        v.setTextColor(accent);
        v.setTextSize(16);
        v.setTypeface(Typeface.DEFAULT_BOLD);
        body.addView(v, lp(-1,-2,0,12,0,8));
    }

    private void smallRow(String[] names) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        for (String n : names) {
            Button b = new Button(this);
            b.setText(n);
            b.setTextSize(12);
            b.setAllCaps(false);
            b.setTextColor(text);
            b.setBackgroundColor(panel);
            b.setOnClickListener(v -> toast(n));
            row.addView(b, weight());
        }
        body.addView(row, lp(-1,-2,0,0,0,8));
    }

    private TextView label(String s) {
        TextView v = new TextView(this);
        v.setText(s);
        v.setTextColor(muted);
        v.setTextSize(14);
        return v;
    }

    private void resolveMarantz(boolean forceSearch) {
        busy(true, forceSearch ? "Marantz zoeken…" : "Marantz controleren…");
        final String remembered = marantzIp;
        io.execute(() -> {
            String found = null;
            if (!forceSearch && remembered != null && MarantzClient.isReachable(remembered)) found = remembered;
            if (found == null) found = MarantzDiscovery.findNa8005(getApplicationContext());
            if (found == null && MarantzClient.isReachable(DEFAULT_IP)) found = DEFAULT_IP;
            final String result = found;
            runOnUiThread(() -> {
                if (result != null) {
                    marantzIp = result;
                    prefs.edit().putString(KEY_IP, result).apply();
                    busy(false, "Marantz gevonden op " + result);
                } else {
                    busy(false, "Marantz niet gevonden. Controleer wifi en Netwerkbediening.");
                }
            });
        });
    }

    private void sendPower(boolean on) {
        if (marantzIp == null) { resolveMarantz(false); return; }
        busy(true, "Opdracht wordt verzonden");
        final String ip = marantzIp;
        io.execute(() -> {
            boolean ok = on ? MarantzClient.powerOn(ip) : MarantzClient.standby(ip);
            runOnUiThread(() -> {
                if (ok) busy(false, on ? "NA8005 staat aan" : "NA8005 staat stand-by");
                else resolveMarantz(true);
            });
        });
    }

    private void openWeb() {
        if (marantzIp == null) return;
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://" + marantzIp + "/")));
    }

    private void busy(boolean b, String s) {
        status.setText(s);
        ipText.setText("IP: " + (marantzIp == null ? "zoeken…" : marantzIp));
        progress.setVisibility(b ? View.VISIBLE : View.GONE);
    }

    private void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_SHORT).show(); }

    private LinearLayout.LayoutParams weight() { return new LinearLayout.LayoutParams(0, dp(52), 1f); }
    private int dp(int n) { return Math.round(n * getResources().getDisplayMetrics().density); }
    private LinearLayout.LayoutParams lp(int w, int h, int l, int t, int r, int b) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(w,h);
        p.setMargins(dp(l),dp(t),dp(r),dp(b));
        return p;
    }

    @Override protected void onDestroy() {
        io.shutdownNow();
        super.onDestroy();
    }
}
