package com.techapps.pdftotext;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.print.PdfPrint;
import android.provider.MediaStore;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.webkit.WebViewAssetLoader;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import org.json.JSONObject;

public class MainActivity extends Activity {
    private static final String BASE = "https://appassets.androidplatform.net/assets/www/";
    private WebView web;
    private WebViewAssetLoader loader;

    // ---- Rewarded ad state (main thread only) ----
    private final Handler ui = new Handler(Looper.getMainLooper());
    private RewardedAd rewarded;
    private long rewardedAt;
    private boolean adLoading, pendingShow;
    private int loadFails;
    private String lastAdError;
    private static final long AD_WAIT_MS = 5000;
    private final Runnable adTimeout = () -> {
        if (pendingShow) {
            pendingShow = false;
            adFailed(lastAdError != null ? lastAdError : "No ad received within 5 seconds. Check your internet connection.");
        }
    };

    private void adFailed(String msg) { js("window.__ad('failed'," + JSONObject.quote(msg) + ")"); }

    private boolean adFresh() {
        return rewarded != null && System.currentTimeMillis() - rewardedAt < 55 * 60 * 1000L;
    }

    private void loadAd() {
        if (adLoading || adFresh()) return;
        rewarded = null;
        adLoading = true;
        RewardedAd.load(this, getString(R.string.admob_rewarded_id), new AdRequest.Builder().build(), new RewardedAdLoadCallback() {
            @Override public void onAdLoaded(RewardedAd ad) {
                rewarded = ad; rewardedAt = System.currentTimeMillis(); adLoading = false; loadFails = 0; lastAdError = null;
                if (pendingShow) { ui.removeCallbacks(adTimeout); pendingShow = false; presentAd(); }
            }
            @Override public void onAdFailedToLoad(LoadAdError e) {
                rewarded = null; adLoading = false; loadFails++;
                lastAdError = "Ad failed to load (code " + e.getCode() + "): " + e.getMessage();
                if (pendingShow) { ui.removeCallbacks(adTimeout); pendingShow = false; adFailed(lastAdError); }
                ui.postDelayed(MainActivity.this::loadAd, Math.min(60000L, 4000L << Math.min(loadFails, 4)));
            }
        });
    }

    private void presentAd() {
        final RewardedAd ad = rewarded;
        rewarded = null;
        if (ad == null) { adFailed("Ad is not ready. Please try again."); loadAd(); return; }
        final boolean[] earned = {false};
        ad.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override public void onAdDismissedFullScreenContent() { js("window.__ad('" + (earned[0] ? "earned" : "closed") + "')"); loadAd(); }
            @Override public void onAdFailedToShowFullScreenContent(AdError e) { adFailed("Ad failed to show (code " + e.getCode() + "): " + e.getMessage()); loadAd(); }
        });
        ad.show(this, item -> earned[0] = true);
    }

    private WebViewClient client() {
        return new WebViewClient() {
            @Override public WebResourceResponse shouldInterceptRequest(WebView v, WebResourceRequest r) {
                return loader.shouldInterceptRequest(r.getUrl());
            }
        };
    }

    private WebView newWeb() {
        WebView w = new WebView(this);
        WebSettings s = w.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        w.setWebViewClient(client());
        return w;
    }

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        loader = new WebViewAssetLoader.Builder().addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this)).build();
        web = newWeb();
        web.addJavascriptInterface(new Bridge(), "Android");
        setContentView(web);
        web.loadUrl(BASE + "index.html");
        MobileAds.initialize(this, status -> runOnUiThread(this::loadAd));
    }

    @Override public void onBackPressed() {
        web.evaluateJavascript("window.__back && window.__back()", v -> { if (!"true".equals(v)) finish(); });
    }

    private void js(String code) { runOnUiThread(() -> web.evaluateJavascript(code, null)); }

    private void export(final WebView hidden, final String name) {
        final File tmp = new File(getCacheDir(), "out.pdf");
        PdfPrint.print(hidden.createPrintDocumentAdapter("doc"), tmp, ok -> new Thread(() -> {
            try {
                if (!ok) throw new Exception("PDF engine failed");
                ContentValues v = new ContentValues();
                v.put(MediaStore.MediaColumns.DISPLAY_NAME, name + ".pdf");
                v.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
                v.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
                Uri u = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, v);
                if (u == null) throw new Exception("Could not create the PDF file");
                try (InputStream in = new FileInputStream(tmp); OutputStream out = getContentResolver().openOutputStream(u)) {
                    if (out == null) throw new Exception("Could not open the PDF file");
                    byte[] buf = new byte[8192]; int n;
                    while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
                }
                js("window.__saved(" + JSONObject.quote(u.toString()) + "," + JSONObject.quote(name) + ")");
            } catch (Exception e) {
                js("window.__err(" + JSONObject.quote(e.getMessage() == null ? "Unknown PDF export error" : e.getMessage()) + ")");
            } finally {
                if (tmp.exists()) tmp.delete();
                runOnUiThread(hidden::destroy);
            }
        }).start());
    }

    private class Bridge {
        @JavascriptInterface public void savePdf(final String html, String rawName) {
            final String name = rawName.replaceAll("[^\\w\\- ]", "_");
            runOnUiThread(() -> {
                final WebView h = newWeb();
                h.addJavascriptInterface(new Object() {
                    @JavascriptInterface public void ready() { runOnUiThread(() -> export(h, name)); }
                }, "P");
                h.loadDataWithBaseURL(BASE, html, "text/html", "utf-8", null);
            });
        }
        @JavascriptInterface public void showRewarded() {
            runOnUiThread(() -> {
                ui.removeCallbacks(adTimeout);
                if (adFresh()) { pendingShow = false; presentAd(); return; }
                pendingShow = true;
                ui.postDelayed(adTimeout, AD_WAIT_MS);
                loadAd();
            });
        }
        @JavascriptInterface public void cancelAd() {
            runOnUiThread(() -> { pendingShow = false; ui.removeCallbacks(adTimeout); });
        }
        @JavascriptInterface public void openFile(String uri) {
            Intent i = new Intent(Intent.ACTION_VIEW).setDataAndType(Uri.parse(uri), "application/pdf").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            try { startActivity(i); } catch (Exception ignored) { }
        }
        @JavascriptInterface public void shareFile(String uri) {
            Intent i = new Intent(Intent.ACTION_SEND).setType("application/pdf").putExtra(Intent.EXTRA_STREAM, Uri.parse(uri)).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(i, "Share PDF"));
        }
    }
}
