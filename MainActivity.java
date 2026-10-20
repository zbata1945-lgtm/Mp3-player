package rs.musicflow.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Insets;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.view.inputmethod.EditorInfo;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * MusicFlow v1: mini-pregledac fokusiran na YouTube i YouTube Music.
 * Ne tvrdi da u potpunosti uklanja reklame ili obezbedjuje playback sa ugasenim ekranom.
 */
public class MainActivity extends Activity {
    private static final String URL_MUSIC = "https://music.youtube.com/";
    private static final String URL_YOUTUBE = "https://m.youtube.com/";
    private static final String PREF_FILE = "musicflow_settings";
    private static final String KEY_BOOKMARKS = "bookmarks";
    private static final String KEY_SHIELD = "shield_on";
    private static final String KEY_MUSIC = "music_mode";
    private static final String KEY_LAST_URL = "last_url";

    private static final int BG = Color.rgb(11, 15, 22);
    private static final int SURFACE = Color.rgb(20, 26, 38);
    private static final int BUTTON = Color.rgb(31, 40, 56);
    private static final int TEXT = Color.rgb(246, 247, 251);
    private static final int MUTED = Color.rgb(166, 177, 196);
    private static final int RED = Color.rgb(228, 73, 73);
    private static final int GREEN = Color.rgb(69, 187, 132);

    private final String[] blockedHosts = {
            "doubleclick.net", "googleadservices.com", "googlesyndication.com",
            "adservice.google.com", "adnxs.com", "adform.net", "criteo.com",
            "criteo.net", "taboola.com", "outbrain.com"
    };

    private WebView webView;
    private ProgressBar progress;
    private EditText searchInput;
    private TextView shieldButton;
    private TextView statusText;
    private TextView musicButton;
    private TextView videoButton;
    private SharedPreferences prefs;
    private boolean musicMode;
    private volatile boolean shieldEnabled;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);

        prefs = getSharedPreferences(PREF_FILE, MODE_PRIVATE);
        musicMode = prefs.getBoolean(KEY_MUSIC, true);
        shieldEnabled = prefs.getBoolean(KEY_SHIELD, true);

        buildInterface();
        setupWebView();
        updateModeButtons();
        updateShieldButton();

        if (savedInstanceState == null || webView.restoreState(savedInstanceState) == null) {
            String last = prefs.getString(KEY_LAST_URL, musicMode ? URL_MUSIC : URL_YOUTUBE);
            if (isSafeWebUrl(last)) {
                webView.loadUrl(last);
            } else {
                webView.loadUrl(musicMode ? URL_MUSIC : URL_YOUTUBE);
            }
        }
    }

    private void buildInterface() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);
        setContentView(root);

        // Android 15+ crta aplikacije do samih ivica ekrana.
        if (Build.VERSION.SDK_INT >= 35) {
            root.setOnApplyWindowInsetsListener((v, insets) -> {
                Insets bars = insets.getInsets(WindowInsets.Type.systemBars());
                v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
                return insets;
            });
            root.requestApplyInsets();
        }

        LinearLayout header = new LinearLayout(this);
        header.setPadding(dp(14), dp(9), dp(14), dp(10));
        header.setOrientation(LinearLayout.VERTICAL);
        header.setBackgroundColor(SURFACE);
        root.addView(header, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout brand = new LinearLayout(this);
        brand.setGravity(Gravity.CENTER_VERTICAL);
        header.addView(brand, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(46)));

        TextView logo = new TextView(this);
        logo.setText("♫  MusicFlow");
        logo.setTextSize(21);
        logo.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        logo.setTextColor(TEXT);
        brand.addView(logo, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        shieldButton = makeButton("Štit", 12, BUTTON);
        shieldButton.setOnClickListener(v -> toggleShield());
        LinearLayout.LayoutParams shieldParams = new LinearLayout.LayoutParams(dp(101), dp(42));
        brand.addView(shieldButton, shieldParams);

        TextView settingsButton = makeButton("⚙", 23, BUTTON);
        settingsButton.setContentDescription("Podešavanja");
        LinearLayout.LayoutParams settingsParams = new LinearLayout.LayoutParams(dp(44), dp(42));
        settingsParams.leftMargin = dp(7);
        brand.addView(settingsButton, settingsParams);
        settingsButton.setOnClickListener(v -> showSettings());

        LinearLayout searchRow = new LinearLayout(this);
        searchRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams searchRowParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(48));
        searchRowParams.topMargin = dp(7);
        header.addView(searchRow, searchRowParams);

        searchInput = new EditText(this);
        searchInput.setSingleLine(true);
        searchInput.setTextSize(16);
        searchInput.setHint("Pretraži pesme ili izvođače...");
        searchInput.setHintTextColor(MUTED);
        searchInput.setTextColor(TEXT);
        searchInput.setPadding(dp(12), 0, dp(9), 0);
        searchInput.setBackground(roundRect(BUTTON, 11));
        searchInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
        searchInput.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        searchInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER &&
                            event.getAction() == KeyEvent.ACTION_DOWN)) {
                search();
                return true;
            }
            return false;
        });
        searchRow.addView(searchInput, new LinearLayout.LayoutParams(0, dp(48), 1f));

        TextView searchBtn = makeButton("Traži", 14, RED);
        searchBtn.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        LinearLayout.LayoutParams searchBtnParams = new LinearLayout.LayoutParams(dp(68), dp(48));
        searchBtnParams.leftMargin = dp(7);
        searchRow.addView(searchBtn, searchBtnParams);
        searchBtn.setOnClickListener(v -> search());

        LinearLayout modes = new LinearLayout(this);
        LinearLayout.LayoutParams modeParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(43));
        modeParams.topMargin = dp(9);
        header.addView(modes, modeParams);

        musicButton = makeButton("♫  YouTube Music", 14, RED);
        modes.addView(musicButton, new LinearLayout.LayoutParams(0, dp(43), 1f));
        musicButton.setOnClickListener(v -> navigateMode(true));

        videoButton = makeButton("▶  YouTube", 14, BUTTON);
        LinearLayout.LayoutParams videoParams = new LinearLayout.LayoutParams(0, dp(43), 1f);
        videoParams.leftMargin = dp(7);
        modes.addView(videoButton, videoParams);
        videoButton.setOnClickListener(v -> navigateMode(false));

        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setMax(100);
        progress.setProgressTintList(android.content.res.ColorStateList.valueOf(RED));
        progress.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(BG));
        progress.setVisibility(View.GONE);
        root.addView(progress, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(3)));

        webView = new WebView(this);
        webView.setBackgroundColor(BG);
        root.addView(webView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        statusText = new TextView(this);
        statusText.setText("Web plejer • internet je potreban");
        statusText.setTextSize(11);
        statusText.setTextColor(MUTED);
        statusText.setGravity(Gravity.CENTER);
        root.addView(statusText, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(21)));

        LinearLayout bottom = new LinearLayout(this);
        bottom.setBackgroundColor(SURFACE);
        bottom.setGravity(Gravity.CENTER);
        bottom.setPadding(dp(8), dp(7), dp(8), dp(7));
        root.addView(bottom, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(62)));

        addBottomButton(bottom, "⌂  Početna", () -> webView.loadUrl(musicMode ? URL_MUSIC : URL_YOUTUBE));
        addBottomButton(bottom, "←  Nazad", () -> {
            if (webView.canGoBack()) webView.goBack();
            else toast("Nema prethodne stranice");
        });
        addBottomButton(bottom, "☆  Sačuvaj", this::saveBookmark);
        addBottomButton(bottom, "♥  Lista", this::showBookmarks);
    }

    private void addBottomButton(LinearLayout bar, String caption, Runnable onClick) {
        TextView button = makeButton(caption, 12, BUTTON);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(46), 1f);
        params.setMargins(dp(3), 0, dp(3), 0);
        bar.addView(button, params);
        button.setOnClickListener(v -> onClick.run());
    }

    private TextView makeButton(String label, int sp, int background) {
        TextView view = new TextView(this);
        view.setText(label);
        view.setTextSize(sp);
        view.setGravity(Gravity.CENTER);
        view.setTextColor(TEXT);
        view.setBackground(roundRect(background, 10));
        view.setClickable(true);
        view.setFocusable(true);
        return view;
    }

    private GradientDrawable roundRect(int color, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radiusDp));
        return drawable;
    }

    private int dp(float value) {
        return (int) (getResources().getDisplayMetrics().density * value + 0.5f);
    }

    private void setupWebView() {
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setJavaScriptCanOpenWindowsAutomatically(false);
        s.setSupportMultipleWindows(false);
        s.setBuiltInZoomControls(true);
        s.setDisplayZoomControls(false);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(false);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        if (Build.VERSION.SDK_INT >= 26) s.setSafeBrowsingEnabled(true);
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);
        WebView.setWebContentsDebuggingEnabled(false);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri url = request.getUrl();
                String scheme = url.getScheme();
                if ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) {
                    return false;
                }
                // Ne pokusavamo da izvrsavamo intent://, javascript: ni nepoznate seme.
                if ("tel".equalsIgnoreCase(scheme) || "mailto".equalsIgnoreCase(scheme)) {
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW, url));
                    } catch (ActivityNotFoundException ignored) {
                        toast("Nema aplikacije za ovu vezu");
                    }
                }
                return true;
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                if (!shieldEnabled || request.isForMainFrame()) return null;
                String host = request.getUrl().getHost();
                if (isBlockedHost(host)) {
                    return new WebResourceResponse("text/plain", "UTF-8",
                            new ByteArrayInputStream(new byte[0]));
                }
                return null;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (isSafeWebUrl(url)) {
                    prefs.edit().putString(KEY_LAST_URL, url).apply();
                }
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request,
                                        WebResourceError error) {
                if (request.isForMainFrame()) {
                    runOnUiThread(() -> toast("Stranica nije učitana. Proveri internet ili izaberi YouTube."));
                }
            }
        });
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int value) {
                progress.setProgress(value);
                progress.setVisibility(value >= 100 ? View.GONE : View.VISIBLE);
            }

            @Override
            public void onReceivedTitle(WebView view, String title) {
                if (title != null && !title.trim().isEmpty()) {
                    statusText.setText(title.length() > 65 ? title.substring(0, 62) + "..." : title);
                }
            }
        });
    }

    private boolean isBlockedHost(String host) {
        if (host == null) return false;
        String lower = host.toLowerCase(Locale.ROOT);
        for (String blocked : blockedHosts) {
            if (lower.equals(blocked) || lower.endsWith("." + blocked)) return true;
        }
        return false;
    }

    private boolean isSafeWebUrl(String url) {
        if (url == null) return false;
        Uri u = Uri.parse(url);
        return ("https".equalsIgnoreCase(u.getScheme()) || "http".equalsIgnoreCase(u.getScheme()))
                && u.getHost() != null;
    }

    private void navigateMode(boolean music) {
        musicMode = music;
        prefs.edit().putBoolean(KEY_MUSIC, music).apply();
        updateModeButtons();
        webView.loadUrl(music ? URL_MUSIC : URL_YOUTUBE);
    }

    private void updateModeButtons() {
        musicButton.setBackground(roundRect(musicMode ? RED : BUTTON, 10));
        videoButton.setBackground(roundRect(musicMode ? BUTTON : RED, 10));
    }

    private void search() {
        String query = searchInput.getText().toString().trim();
        if (query.isEmpty()) {
            toast("Upiši naziv pesme ili izvođača");
            return;
        }
        String encoded = Uri.encode(query);
        String url = musicMode
                ? "https://music.youtube.com/search?q=" + encoded
                : "https://m.youtube.com/results?search_query=" + encoded;
        webView.loadUrl(url);
        searchInput.clearFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(searchInput.getWindowToken(), 0);
    }

    private void toggleShield() {
        shieldEnabled = !shieldEnabled;
        prefs.edit().putBoolean(KEY_SHIELD, shieldEnabled).apply();
        updateShieldButton();
        toast(shieldEnabled ? "Zaštita uključena (osnovna filtracija)" : "Zaštita isključena");
        webView.reload();
    }

    private void updateShieldButton() {
        shieldButton.setText(shieldEnabled ? "●  Štit ON" : "○  Štit OFF");
        shieldButton.setTextColor(shieldEnabled ? GREEN : MUTED);
    }

    private JSONArray loadBookmarks() {
        try {
            return new JSONArray(prefs.getString(KEY_BOOKMARKS, "[]"));
        } catch (JSONException ignored) {
            return new JSONArray();
        }
    }

    private void saveBookmark() {
        String url = webView.getUrl();
        if (!isSafeWebUrl(url)) {
            toast("Najpre otvori pesmu ili stranicu");
            return;
        }
        JSONArray old = loadBookmarks();
        for (int i = 0; i < old.length(); i++) {
            JSONObject item = old.optJSONObject(i);
            if (item != null && url.equals(item.optString("url"))) {
                toast("Već se nalazi u omiljenima");
                return;
            }
        }
        JSONArray updated = new JSONArray();
        JSONObject item = new JSONObject();
        try {
            String title = webView.getTitle();
            item.put("title", title == null || title.trim().isEmpty() ? "Sačuvana stranica" : title);
            item.put("url", url);
            updated.put(item);
            for (int i = 0; i < Math.min(old.length(), 49); i++) updated.put(old.get(i));
            prefs.edit().putString(KEY_BOOKMARKS, updated.toString()).apply();
            toast("Sačuvano u omiljene ♥");
        } catch (JSONException e) {
            toast("Nije moguće sačuvati stavku");
        }
    }

    private void showBookmarks() {
        JSONArray bookmarks = loadBookmarks();
        if (bookmarks.length() == 0) {
            new AlertDialog.Builder(this)
                    .setTitle("Omiljene pesme i stranice")
                    .setMessage("Još nema sačuvanih stavki. Otvori pesmu, pa pritisni ☆ Sačuvaj.")
                    .setPositiveButton("U redu", null)
                    .show();
            return;
        }
        List<String> titles = new ArrayList<>();
        for (int i = 0; i < bookmarks.length(); i++) {
            JSONObject item = bookmarks.optJSONObject(i);
            titles.add(item == null ? "Sačuvana stranica" : item.optString("title", "Sačuvana stranica"));
        }
        new AlertDialog.Builder(this)
                .setTitle("♥ Omiljene (" + titles.size() + ")")
                .setItems(titles.toArray(new String[0]), (dialog, which) -> bookmarkAction(which))
                .setNegativeButton("Zatvori", null)
                .show();
    }

    private void bookmarkAction(int index) {
        JSONArray list = loadBookmarks();
        JSONObject item = list.optJSONObject(index);
        if (item == null) return;
        String url = item.optString("url");
        String title = item.optString("title", "Pesma");
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setItems(new String[]{"▶ Otvori", "✕ Ukloni iz omiljenih"}, (dialog, which) -> {
                    if (which == 0) {
                        if (isSafeWebUrl(url)) webView.loadUrl(url);
                    } else {
                        JSONArray out = new JSONArray();
                        for (int i = 0; i < list.length(); i++) {
                            if (i != index) out.put(list.opt(i));
                        }
                        prefs.edit().putString(KEY_BOOKMARKS, out.toString()).apply();
                        toast("Uklonjeno iz omiljenih");
                        showBookmarks();
                    }
                })
                .setNegativeButton("Otkaži", null)
                .show();
    }

    private void showSettings() {
        String info = "MusicFlow je jednostavan pregledac za YouTube Music i YouTube. "
                + "Štit blokira neke poznate reklamne domene, ali ne garantuje uklanjanje "
                + "YouTube reklama. Reprodukcija pri zakljucanom ekranu i prijava na Google "
                + "nalog mogu biti ogranicene od strane sajta ili Androida.";
        new AlertDialog.Builder(this)
                .setTitle("O aplikaciji · MusicFlow 1.0")
                .setMessage(info)
                .setPositiveButton("Razumem", null)
                .setNeutralButton("Obriši kolačiće", (dialog, which) -> confirmClearCookies())
                .show();
    }

    private void confirmClearCookies() {
        new AlertDialog.Builder(this)
                .setTitle("Obrisati podatke prijave?")
                .setMessage("Biće obrisani kolačići i keš pregledača. Omiljene stavke ostaju sačuvane.")
                .setNegativeButton("Otkaži", null)
                .setPositiveButton("Obriši", (dialog, which) -> {
                    CookieManager.getInstance().removeAllCookies(value -> {
                        CookieManager.getInstance().flush();
                        webView.clearCache(true);
                        runOnUiThread(() -> {
                            toast("Kolačići i keš obrisani");
                            webView.loadUrl(musicMode ? URL_MUSIC : URL_YOUTUBE);
                        });
                    });
                })
                .show();
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        webView.saveState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            ViewGroup parent = (ViewGroup) webView.getParent();
            if (parent != null) parent.removeView(webView);
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }
}
