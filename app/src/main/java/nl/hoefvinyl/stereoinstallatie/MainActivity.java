package nl.hoefvinyl.stereoinstallatie;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
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
    private TextView status;
    private TextView ipText;
    private ProgressBar progress;
    private Button onButton;
    private Button offButton;
    private Button webButton;
    private Button searchButton;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        marantzIp = prefs.getString(KEY_IP, DEFAULT_IP);
        setContentView(buildUi());
        resolveMarantz(false);
    }

    private View buildUi() {
        int pad = dp(20);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad, pad, pad);
        root.setBackgroundColor(Color.rgb(255, 240, 189));

        TextView title = new TextView(this);
        title.setText("Stereo-installatie");
        title.setTextSize(28);
        title.setTextColor(Color.rgb(74, 29, 9));
        title.setGravity(Gravity.CENTER);
        root.addView(title, lp(-1, -2, 0, 0, 0, dp(14)));

        TextView sub = new TextView(this);
        sub.setText("Marantz NA8005");
        sub.setTextSize(18);
        sub.setGravity(Gravity.CENTER);
        sub.setTextColor(Color.DKGRAY);
        root.addView(sub, lp(-1, -2, 0, 0, 0, dp(18)));

        status = new TextView(this);
        status.setText("Controleren…");
        status.setTextSize(17);
        status.setGravity(Gravity.CENTER);
        root.addView(status, lp(-1, -2, 0, 0, 0, dp(6)));

        ipText = new TextView(this);
        ipText.setText("IP: " + marantzIp);
        ipText.setGravity(Gravity.CENTER);
        ipText.setTextSize(15);
        root.addView(ipText, lp(-1, -2, 0, 0, 0, dp(14)));

        progress = new ProgressBar(this);
        progress.setIndeterminate(true);
        root.addView(progress, lp(-1, dp(44), 0, 0, 0, dp(12)));

        onButton = makeButton("Marantz aan");
        onButton.setOnClickListener(v -> sendPower(true));
        root.addView(onButton, lp(-1, dp(58), 0, 0, 0, dp(10)));

        offButton = makeButton("Stand-by");
        offButton.setOnClickListener(v -> sendPower(false));
        root.addView(offButton, lp(-1, dp(58), 0, 0, 0, dp(10)));

        webButton = makeButton("Webbediening openen");
        webButton.setOnClickListener(v -> {
            if (marantzIp != null) startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://" + marantzIp + "/")));
        });
        root.addView(webButton, lp(-1, dp(58), 0, 0, 0, dp(10)));

        searchButton = makeButton("Marantz opnieuw zoeken");
        searchButton.setOnClickListener(v -> resolveMarantz(true));
        root.addView(searchButton, lp(-1, dp(58), 0, 0, 0, dp(10)));

        TextView help = new TextView(this);
        help.setText("De app onthoudt het laatst werkende IP-adres. Reageert dat adres niet meer, dan zoekt de app de NA8005 automatisch opnieuw op het lokale netwerk en slaat het nieuwe IP op.");
        help.setTextSize(14);
        help.setTextColor(Color.DKGRAY);
        help.setPadding(0, dp(12), 0, 0);
        root.addView(help, lp(-1, -2, 0, 0, 0, 0));

        ScrollView scroll = new ScrollView(this);
        scroll.addView(root);
        return scroll;
    }

    private Button makeButton(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextSize(17);
        b.setAllCaps(false);
        return b;
    }

    private void resolveMarantz(boolean forceSearch) {
        busy(true, forceSearch ? "Marantz zoeken…" : "Marantz controleren…");
        final String remembered = marantzIp;
        io.execute(() -> {
            String found = null;

            if (!forceSearch && remembered != null && MarantzClient.isReachable(remembered)) {
                found = remembered;
            }

            if (found == null) {
                found = MarantzDiscovery.findNa8005(getApplicationContext());
            }

            if (found == null && MarantzClient.isReachable(DEFAULT_IP)) {
                found = DEFAULT_IP;
            }

            final String result = found;
            runOnUiThread(() -> {
                if (result != null) {
                    marantzIp = result;
                    prefs.edit().putString(KEY_IP, result).apply();
                    ipText.setText("IP: " + result);
                    busy(false, "Marantz gevonden en klaar voor bediening");
                } else {
                    busy(false, "Marantz niet gevonden op dit netwerk");
                    Toast.makeText(this, "Controleer of telefoon en Marantz op hetzelfde netwerk zitten.", Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    private void sendPower(boolean on) {
        if (marantzIp == null) {
            resolveMarantz(false);
            return;
        }
        busy(true, on ? "Marantz inschakelen…" : "Marantz naar stand-by…");
        final String ip = marantzIp;
        io.execute(() -> {
            boolean ok = on ? MarantzClient.powerOn(ip) : MarantzClient.standby(ip);
            runOnUiThread(() -> {
                if (ok) {
                    busy(false, on ? "Marantz ingeschakeld" : "Marantz staat in stand-by");
                } else {
                    status.setText("Geen antwoord; IP opnieuw zoeken…");
                    resolveMarantz(true);
                }
            });
        });
    }

    private void busy(boolean value, String text) {
        status.setText(text);
        progress.setVisibility(value ? View.VISIBLE : View.GONE);
        onButton.setEnabled(!value);
        offButton.setEnabled(!value);
        webButton.setEnabled(!value && marantzIp != null);
        searchButton.setEnabled(!value);
    }

    private int dp(int n) {
        return Math.round(n * getResources().getDisplayMetrics().density);
    }

    private LinearLayout.LayoutParams lp(int w, int h, int l, int t, int r, int b) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(w, h);
        p.setMargins(l, t, r, b);
        return p;
    }

    @Override
    protected void onDestroy() {
        io.shutdownNow();
        super.onDestroy();
    }
}
