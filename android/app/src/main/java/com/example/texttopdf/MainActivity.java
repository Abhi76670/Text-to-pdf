package com.example.texttopdf;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.print.PdfPrint;
import android.provider.MediaStore;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.webkit.WebViewAssetLoader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import org.json.JSONObject;

public class MainActivity extends Activity {
    private static final String BASE = "https://appassets.androidplatform.net/assets/www/";
    private WebView web;
    private WebViewAssetLoader loader;

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
                try (InputStream in = new FileInputStream(tmp); OutputStream out = getContentResolver().openOutputStream(u)) {
                    byte[] buf = new byte[8192]; int n;
                    while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
                }
                js("window.__saved(" + JSONObject.quote(u.toString()) + "," + JSONObject.quote(name) + ")");
            } catch (Exception e) {
                js("window.__err(" + JSONObject.quote(String.valueOf(e.getMessage())) + ")");
            }
            runOnUiThread(hidden::destroy);
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
