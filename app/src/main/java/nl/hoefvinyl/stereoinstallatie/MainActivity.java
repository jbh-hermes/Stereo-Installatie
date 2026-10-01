package nl.hoefvinyl.stereoinstallatie;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Build;
import android.view.WindowInsets;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Space;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private static final String PREFS="stereo_installatie";
    private static final String KEY_IP="marantz_ip";
    private static final String DEFAULT_IP="192.168.68.73";

    private final ExecutorService io=Executors.newSingleThreadExecutor();
    private SharedPreferences prefs;
    private String marantzIp;
    private LinearLayout body, nav;
    private ProgressBar progress;
    private int page=0;
    private List<MarantzClient.Favorite> favorites=new ArrayList<>();
    private int favoriteIndex=0;

    private final int BG=Color.rgb(24,25,28);
    private final int CARD=Color.rgb(37,39,43);
    private final int CARD2=Color.rgb(49,50,55);
    private final int GOLD=Color.rgb(216,170,96);
    private final int PURPLE=Color.rgb(79,55,137);
    private final int ACTIVE=Color.rgb(82,73,101);
    private final int WHITE=Color.rgb(247,245,248);
    private final int MUTED=Color.rgb(190,187,195);

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        prefs=getSharedPreferences(PREFS,MODE_PRIVATE);
        marantzIp=prefs.getString(KEY_IP,DEFAULT_IP);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        setContentView(buildRoot());
        showHome();
        resolveMarantz(false);
    }

    private View buildRoot(){
        LinearLayout root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);

        progress=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);
        progress.setIndeterminate(true);
        progress.setVisibility(View.GONE);
        root.addView(progress,new LinearLayout.LayoutParams(-1,dp(3)));

        ScrollView sv=new ScrollView(this);
        body=new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(30),dp(26),dp(30),dp(28));
        sv.addView(body);
        root.addView(sv,new LinearLayout.LayoutParams(-1,0,1f));

        nav=new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setGravity(Gravity.CENTER);
        nav.setPadding(dp(5),dp(7),dp(5),dp(8));
        nav.setBackgroundColor(Color.rgb(36,37,41));
        root.addView(nav,new LinearLayout.LayoutParams(-1,dp(92)));
        rebuildNav();
        return root;
    }

    private void rebuildNav(){
        nav.removeAllViews();
        nav.addView(navItem(0,"⌂","Start"),weight());
        nav.addView(navItem(1,"▰","Radio"),weight());
        nav.addView(navItem(2,"♥","Favorieten"),weight());
        nav.addView(navItem(3,"▣","Versterker"),weight());
        nav.addView(navItem(4,"▦","Bronnen"),weight());
    }

    private View navItem(int p,String icon,String label){
        LinearLayout box=new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setPadding(dp(4),dp(3),dp(4),dp(2));
        TextView iv=txt(icon,29,p==page?WHITE:MUTED,Typeface.BOLD);
        iv.setGravity(Gravity.CENTER);
        if(p==page){
            iv.setBackground(round(ACTIVE,50,0,0));
            iv.setPadding(dp(16),dp(6),dp(16),dp(6));
        }
        TextView tv=txt(label,13,p==page?WHITE:MUTED,Typeface.BOLD);
        tv.setGravity(Gravity.CENTER);
        box.addView(iv);
        box.addView(tv);
        box.setOnClickListener(v->{page=p; rebuildNav(); if(p==0)showHome(); else if(p==1)showRadio(); else if(p==2)showFavorites(); else if(p==3)showAmplifier(); else showSources();});
        return box;
    }

    private void header(String title, boolean gear){
        LinearLayout r=new LinearLayout(this);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setGravity(Gravity.CENTER_VERTICAL);
        TextView h=txt(title,31,WHITE,Typeface.NORMAL);
        r.addView(h,new LinearLayout.LayoutParams(0,-2,1f));
        if(gear){
            Button g=plainButton("⚙",32,WHITE);
            g.setOnClickListener(v->showSettingsDialog());
            r.addView(g,new LinearLayout.LayoutParams(dp(62),dp(62)));
        }
        body.addView(r,lp(-1,-2,0,0,0,24));
    }

    private void showHome(){
        page=0; rebuildNav(); body.removeAllViews(); header("Stereo-Installatie",true);

        LinearLayout now=card();
        TextView wave=txt("▥",48,GOLD,Typeface.BOLD); wave.setGravity(Gravity.CENTER);
        now.addView(wave,lp(-1,-2,0,16,0,8));
        TextView cap=txt("Nu afspelen",18,MUTED,Typeface.NORMAL); cap.setGravity(Gravity.CENTER); now.addView(cap);
        TextView station=txt("HoefVinyl Radio Live",29,WHITE,Typeface.NORMAL); station.setGravity(Gravity.CENTER); now.addView(station,lp(-1,-2,0,6,0,0));
        TextView artist=txt("",18,GOLD,Typeface.NORMAL); artist.setGravity(Gravity.CENTER); now.addView(artist,lp(-1,-2,0,4,0,0));
        TextView title=txt("",18,WHITE,Typeface.BOLD); title.setGravity(Gravity.CENTER); now.addView(title,lp(-1,-2,0,4,0,14));
        Button fav=outlineButton("♡  Zender aan favorieten toevoegen",15,GOLD);
        fav.setOnClickListener(v->chooseFavoriteSlot());
        now.addView(fav,lp(-1,dp(58),18,0,18,8));
        body.addView(now,lp(-1,-2,0,0,0,26));

        body.addView(txt("Snel kiezen",28,WHITE,Typeface.NORMAL),lp(-1,-2,0,0,0,16));
        LinearLayout row1=row();
        row1.addView(tile("▰","Internet Radio",v->{page=1;rebuildNav();showRadio();}),weight());
        row1.addView(tile("▤","Muziekserver",v->selectSource("SISERVER","Muziekserver gekozen")),weight());
        body.addView(row1,lp(-1,dp(164),0,0,0,12));
        LinearLayout row2=row();
        row2.addView(tile("♆","USB",v->selectSource("SIUSB","USB gekozen")),weight());
        row2.addView(tile("★","Favorieten",v->{page=2;rebuildNav();showFavorites();}),weight());
        body.addView(row2,lp(-1,dp(164),0,0,0,4));

        refreshNowPlaying(station,artist,title);
    }

    private void showRadio(){
        page=1; rebuildNav(); body.removeAllViews(); header("Radio",false);

        LinearLayout p=card();
        LinearLayout pr=row();
        Button on=goldButton("⏻  NA8005\naan");
        Button off=goldButton("⌁  NA8005 uit");
        on.setOnClickListener(v->power(true));
        off.setOnClickListener(v->power(false));
        pr.addView(on,weight()); pr.addView(off,weight());
        p.addView(pr,new LinearLayout.LayoutParams(-1,dp(108)));
        body.addView(p,lp(-1,-2,0,0,0,24));

        LinearLayout now=card();
        TextView cap=txt("Nu op de radio",21,MUTED,Typeface.NORMAL); now.addView(cap);
        TextView station=txt("HoefVinyl Radio Live",31,WHITE,Typeface.NORMAL); now.addView(station,lp(-1,-2,0,8,0,0));
        TextView artist=txt("",23,GOLD,Typeface.NORMAL); now.addView(artist,lp(-1,-2,0,10,0,0));
        TextView title=txt("",20,WHITE,Typeface.BOLD); now.addView(title,lp(-1,-2,0,8,0,18));
        Button save=goldButton("♡  Zender bewaren"); save.setOnClickListener(v->chooseFavoriteSlot()); now.addView(save,lp(-1,dp(64),0,0,0,10));
        LinearLayout adj=row();
        Button prev=outlineButton("◀  Vorige",15,GOLD); Button next=outlineButton("Volgende  ▶",15,GOLD);
        prev.setOnClickListener(v->adjacentFavorite(-1)); next.setOnClickListener(v->adjacentFavorite(1));
        adj.addView(prev,weight()); adj.addView(next,weight()); now.addView(adj,new LinearLayout.LayoutParams(-1,dp(58)));
        body.addView(now,lp(-1,-2,0,0,0,24));

        LinearLayout search=card();
        search.addView(txt("Zoek radiostation",27,WHITE,Typeface.NORMAL),lp(-1,-2,0,0,0,18));
        EditText q=new EditText(this);
        q.setHint("Naam of zoekwoord");
        q.setHintTextColor(MUTED); q.setTextColor(WHITE); q.setTextSize(17);
        q.setSingleLine(true); q.setPadding(dp(16),0,dp(16),0);
        q.setBackground(round(CARD,8,Color.rgb(145,142,150),1));
        search.addView(q,new LinearLayout.LayoutParams(-1,dp(68)));
        Button find=plainButton("⌕  Zoeken bij vTuner",17,MUTED);
        find.setBackground(round(CARD2,50,0,0));
        find.setOnClickListener(v->{
            String term=q.getText().toString().trim();
            if(term.isEmpty())toast("Vul eerst een naam of zoekwoord in");
            else toast("vTuner zoeken wordt uit de oude APK verder teruggezet");
        });
        search.addView(find,lp(-1,dp(62),0,16,0,0));
        body.addView(search);

        refreshNowPlaying(station,artist,title);
    }

    private void showFavorites(){
        page=2; rebuildNav(); body.removeAllViews();
        LinearLayout h=new LinearLayout(this); h.setOrientation(LinearLayout.HORIZONTAL); h.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout ht=new LinearLayout(this); ht.setOrientation(LinearLayout.VERTICAL);
        ht.addView(txt("Marantz-favorieten",31,WHITE,Typeface.NORMAL));
        ht.addView(txt("Tik op een station om het direct af te\nspelen.",17,MUTED,Typeface.NORMAL),lp(-1,-2,0,5,0,0));
        h.addView(ht,new LinearLayout.LayoutParams(0,-2,1f));
        Button refresh=plainButton("⟳",35,WHITE); refresh.setOnClickListener(v->loadFavorites(true));
        h.addView(refresh,new LinearLayout.LayoutParams(dp(58),dp(58)));
        body.addView(h,lp(-1,-2,0,0,0,18));
        loadFavorites(false);
    }

    private void renderFavorites(){
        while(body.getChildCount()>1)body.removeViewAt(1);
        if(favorites.isEmpty()){
            TextView empty=txt("Er staan nog geen favorieten in de NA8005.",18,MUTED,Typeface.NORMAL);
            body.addView(empty,lp(-1,-2,0,18,0,0)); return;
        }
        for(int i=0;i<favorites.size();i++){
            final int idx=i; final MarantzClient.Favorite f=favorites.get(i);
            LinearLayout c=card(); c.setOrientation(LinearLayout.HORIZONTAL); c.setGravity(Gravity.CENTER_VERTICAL);
            TextView pos=txt(f.position,18,GOLD,Typeface.NORMAL); c.addView(pos,new LinearLayout.LayoutParams(dp(52),-2));
            TextView radio=txt("▰",29,GOLD,Typeface.BOLD); c.addView(radio,new LinearLayout.LayoutParams(dp(60),-2));
            LinearLayout mid=new LinearLayout(this); mid.setOrientation(LinearLayout.VERTICAL);
            mid.addView(txt(f.name,20,WHITE,Typeface.BOLD));
            mid.addView(txt("Tik om af te spelen",17,MUTED,Typeface.NORMAL),lp(-1,-2,0,4,0,0));
            c.addView(mid,new LinearLayout.LayoutParams(0,-2,1f));
            Button del=plainButton("▣",24,WHITE); del.setOnClickListener(v->deleteFavorite(f));
            c.addView(del,new LinearLayout.LayoutParams(dp(58),dp(58)));
            c.setOnClickListener(v->{favoriteIndex=idx;callFavorite(f);});
            body.addView(c,lp(-1,dp(132),0,0,0,14));
        }
    }

    private void showAmplifier(){
        page=3; rebuildNav(); body.removeAllViews(); header("PM8003 versterker",false);
        LinearLayout top=row();
        Button allOn=goldButton("⏻  Alles aan"); Button allOff=outlineButton("⌁  Alles uit",16,GOLD);
        allOn.setOnClickListener(v->{power(true);sendAmp("AMPON","PM8003 staat aan");});
        allOff.setOnClickListener(v->{sendAmp("AMPOFF","PM8003 staat stand-by");power(false);});
        top.addView(allOn,weight()); top.addView(allOff,weight()); body.addView(top,new LinearLayout.LayoutParams(-1,dp(112)));
        body.addView(txt("Alleen PM8003",27,WHITE,Typeface.NORMAL),lp(-1,-2,0,24,0,18));
        LinearLayout r=row();
        Button on=goldButton("⏻  Aan"); Button sb=goldButton("⌁  Stand-by");
        on.setOnClickListener(v->sendAmp("AMPON","PM8003 staat aan")); sb.setOnClickListener(v->sendAmp("AMPOFF","PM8003 staat stand-by"));
        r.addView(on,weight());r.addView(sb,weight());body.addView(r,new LinearLayout.LayoutParams(-1,dp(108)));
        LinearLayout vol=card(); vol.addView(txt("Volume",27,WHITE,Typeface.NORMAL),lp(-1,-2,0,0,0,18));
        LinearLayout vr1=row(); Button soft=goldButton("◀  Zachter");Button loud=goldButton("🔊  Harder");
        soft.setOnClickListener(v->sendAmp("AMPVOLDOWN","Zachter"));loud.setOnClickListener(v->sendAmp("AMPVOLUP","Harder"));vr1.addView(soft,weight());vr1.addView(loud,weight());vol.addView(vr1,new LinearLayout.LayoutParams(-1,dp(102)));
        LinearLayout vr2=row();Button mute=goldButton("⌁  Dempen");Button unmute=goldButton("🔊  Geluid aan");
        mute.setOnClickListener(v->sendAmp("AMPMUTEON","Geluid gedempt"));unmute.setOnClickListener(v->sendAmp("AMPMUTEOFF","Geluid weer aan"));vr2.addView(mute,weight());vr2.addView(unmute,weight());vol.addView(vr2,lp(-1,dp(102),0,12,0,0));
        body.addView(vol,lp(-1,-2,0,24,0,24));
        body.addView(txt("Ingang kiezen",27,WHITE,Typeface.NORMAL),lp(-1,-2,0,0,0,18));
        addAmpInputGrid();
    }

    private void addAmpInputGrid(){
        LinearLayout a=row(); a.addView(tile("●","CD",v->sendAmp("AMPINPUTCD","PM8003 ingang CD")),weight()); a.addView(tile("▰","Tuner",v->sendAmp("AMPINPUTTUNER","PM8003 ingang Tuner")),weight()); body.addView(a,new LinearLayout.LayoutParams(-1,dp(160)));
        LinearLayout b=row(); b.addView(tile("∿","AUX",v->sendAmp("AMPINPUTAUX","PM8003 ingang AUX")),weight()); b.addView(tile("●","Phono",v->sendAmp("AMPINPUTPHONO","PM8003 ingang Phono")),weight()); body.addView(b,lp(-1,dp(160),0,12,0,0));
    }

    private void showSources(){
        page=4; rebuildNav(); body.removeAllViews(); header("Bronnen",false);
        body.addView(txt("Kies wat je wilt beluisteren op "+marantzIp,18,MUTED,Typeface.NORMAL),lp(-1,-2,0,0,0,18));
        String[][] data={{"▰","Internet Radio","SIIRADIO"},{"▤","Muziekserver /\nNAS","SISERVER"},{"♆","USB","SIUSB"},{"★","Favorieten","FAVORITES"},{"♪","Spotify","SISPOTIFY"},{"∿","Digitaal coax","SICOAXIAL"},{"▥","Digitaal optisch","SIOPTICAL"},{"▱","USB-DAC","SIUSBDAC"}};
        for(int i=0;i<data.length;i+=2){
            LinearLayout r=row();
            final String[] a=data[i], b=data[i+1];
            r.addView(tile(a[0],a[1],v->{if(a[2].equals("FAVORITES")){page=2;rebuildNav();showFavorites();}else selectSource(a[2],a[1]+" gekozen");}),weight());
            r.addView(tile(b[0],b[1],v->{if(b[2].equals("FAVORITES")){page=2;rebuildNav();showFavorites();}else selectSource(b[2],b[1]+" gekozen");}),weight());
            body.addView(r,lp(-1,dp(162),0,0,0,12));
        }
    }

    private void resolveMarantz(boolean force){
        progress.setVisibility(View.VISIBLE); final String remembered=marantzIp;
        io.execute(()->{
            String found=null;
            if(!force&&MarantzClient.isReachable(remembered))found=remembered;
            if(found==null)found=MarantzDiscovery.findNa8005(getApplicationContext());
            if(found==null&&MarantzClient.isReachable(DEFAULT_IP))found=DEFAULT_IP;
            final String r=found;
            runOnUiThread(()->{
                progress.setVisibility(View.GONE);
                if(r!=null){marantzIp=r;prefs.edit().putString(KEY_IP,r).apply();}
                else toast("Marantz niet gevonden. Controleer wifi en Netwerkbediening.");
            });
        });
    }

    private void refreshNowPlaying(TextView station,TextView artist,TextView title){
        final String ip=marantzIp; if(ip==null)return;
        io.execute(()->{
            MarantzClient.NowPlaying n=MarantzClient.fetchNowPlaying(ip);
            runOnUiThread(()->{station.setText(n.station);artist.setText(n.artist);title.setText(n.title);});
        });
    }

    private void loadFavorites(boolean toast){
        progress.setVisibility(View.VISIBLE); final String ip=marantzIp;
        io.execute(()->{
            List<MarantzClient.Favorite> f=MarantzClient.fetchFavorites(ip);
            runOnUiThread(()->{
                progress.setVisibility(View.GONE); favorites=f; renderFavorites();
                if(toast)toast("Favorieten bijgewerkt");
            });
        });
    }

    private void callFavorite(MarantzClient.Favorite f){
        progress.setVisibility(View.VISIBLE); io.execute(()->{
            boolean ok=MarantzClient.callFavorite(marantzIp,f.position);
            runOnUiThread(()->{progress.setVisibility(View.GONE);toast(ok?f.name+" wordt afgespeeld":"NA8005 reageerde niet op deze favoriet");});
        });
    }

    private void deleteFavorite(MarantzClient.Favorite f){
        new AlertDialog.Builder(this).setTitle("Favoriet verwijderen").setMessage(f.name+" verwijderen uit de NA8005?")
                .setNegativeButton("Annuleren",null).setPositiveButton("Verwijderen",(d,w)->{
                    io.execute(()->{boolean ok=MarantzClient.deleteFavorite(marantzIp,f.position);runOnUiThread(()->{toast(ok?"Favoriet verwijderd":"Verwijderen mislukt");if(ok)loadFavorites(false);});});
                }).show();
    }

    private void adjacentFavorite(int dir){
        if(favorites.isEmpty()){io.execute(()->{List<MarantzClient.Favorite> fs=MarantzClient.fetchFavorites(marantzIp);runOnUiThread(()->{favorites=fs;if(!fs.isEmpty())adjacentFavorite(dir);});});return;}
        favoriteIndex=(favoriteIndex+dir+favorites.size())%favorites.size(); callFavorite(favorites.get(favoriteIndex));
    }

    private void chooseFavoriteSlot(){
        final String[] slots=new String[20]; for(int i=0;i<20;i++)slots[i]=String.format("%02d",i+1);
        new AlertDialog.Builder(this).setTitle("Kies een vrije Marantz-favorietenplaats").setItems(slots,(d,which)->{
            String slot=slots[which]; io.execute(()->{boolean ok=MarantzClient.addCurrentToFavorite(marantzIp,slot);runOnUiThread(()->toast(ok?"Opgeslagen op Marantz-positie "+slot:"Opslaan mislukt"));});
        }).show();
    }

    private void power(boolean on){
        progress.setVisibility(View.VISIBLE); io.execute(()->{boolean ok=on?MarantzClient.powerOn(marantzIp):MarantzClient.standby(marantzIp);runOnUiThread(()->{progress.setVisibility(View.GONE);if(!ok)resolveMarantz(true);else toast(on?"NA8005 staat aan":"NA8005 staat stand-by");});});
    }

    private void selectSource(String cmd,String message){
        io.execute(()->{boolean ok=MarantzClient.selectSource(marantzIp,cmd);runOnUiThread(()->toast(ok?message:"NA8005 reageerde niet op deze keuze"));});
    }

    private void sendAmp(String cmd,String msg){
        io.execute(()->{boolean ok=MarantzClient.selectSource(marantzIp,cmd);runOnUiThread(()->toast(ok?msg:"PM8003 reageerde niet; probeer de testknoppen"));});
    }

    private void showSettingsDialog(){
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(24),dp(6),dp(24),0);
        EditText ip=new EditText(this);ip.setText(marantzIp);ip.setInputType(InputType.TYPE_CLASS_PHONE);ip.setHint("Handmatig IP-adres");box.addView(ip);
        new AlertDialog.Builder(this).setTitle("Marantz-verbinding").setView(box)
                .setNeutralButton("Opnieuw zoeken",(d,w)->resolveMarantz(true))
                .setNegativeButton("Annuleren",null)
                .setPositiveButton("Opslaan en verbinden",(d,w)->{String s=ip.getText().toString().trim();if(!s.isEmpty()){marantzIp=s;prefs.edit().putString(KEY_IP,s).apply();resolveMarantz(false);}}).show();
    }

    private LinearLayout card(){
        LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(22),dp(20),dp(22),dp(20));c.setBackground(round(CARD,26,0,0));return c;
    }

    private View tile(String icon,String label,View.OnClickListener l){
        LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setGravity(Gravity.CENTER);c.setPadding(dp(8),dp(8),dp(8),dp(8));c.setBackground(round(CARD,26,0,0));
        TextView i=txt(icon,32,GOLD,Typeface.BOLD);i.setGravity(Gravity.CENTER);TextView t=txt(label,17,GOLD,Typeface.BOLD);t.setGravity(Gravity.CENTER);
        c.addView(i);c.addView(t,lp(-1,-2,0,8,0,0));c.setOnClickListener(l);return c;
    }

    private Button goldButton(String s){Button b=plainButton(s,16,PURPLE);b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);b.setBackground(round(GOLD,28,0,0));return b;}
    private Button outlineButton(String s,int size,int color){Button b=plainButton(s,size,color);b.setBackground(round(Color.TRANSPARENT,28,Color.rgb(150,147,154),1));return b;}
    private Button plainButton(String s,int size,int color){Button b=new Button(this);b.setText(s);b.setTextSize(size);b.setTextColor(color);b.setAllCaps(false);b.setGravity(Gravity.CENTER);b.setPadding(dp(8),0,dp(8),0);b.setBackgroundColor(Color.TRANSPARENT);return b;}
    private TextView txt(String s,int size,int color,int style){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setTypeface(Typeface.DEFAULT,style);return t;}
    private LinearLayout row(){LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.HORIZONTAL);r.setGravity(Gravity.CENTER_VERTICAL);r.setDividerPadding(dp(10));return r;}
    private LinearLayout.LayoutParams weight(){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-1,1f);p.setMargins(dp(5),0,dp(5),0);return p;}
    private GradientDrawable round(int color,int radius,int strokeColor,int strokeWidth){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(dp(radius));if(strokeWidth>0)g.setStroke(dp(strokeWidth),strokeColor);return g;}
    private LinearLayout.LayoutParams lp(int w,int h,int l,int t,int r,int b){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(w,h);p.setMargins(dp(l),dp(t),dp(r),dp(b));return p;}
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}

    @Override protected void onDestroy(){io.shutdownNow();super.onDestroy();}
}
