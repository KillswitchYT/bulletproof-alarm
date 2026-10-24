package com.killswitch.alarm;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.net.Uri;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

final class Web {
    static final String HOST = "appassets.androidplatform.net";
    static final String URL = "https://" + HOST + "/assets/index.html";

    static String mime(String n) {
        if (n.endsWith(".html")) return "text/html";
        if (n.endsWith(".js")) return "application/javascript";
        if (n.endsWith(".css")) return "text/css";
        if (n.endsWith(".woff2")) return "font/woff2";
        if (n.endsWith(".png")) return "image/png";
        if (n.endsWith(".svg")) return "image/svg+xml";
        return "application/octet-stream";
    }

    static WebResourceResponse empty(int code) {
        return new WebResourceResponse("text/plain", "utf-8", code, "Blocked", null, new ByteArrayInputStream(new byte[0]));
    }

    @SuppressLint("SetJavaScriptEnabled")
    static WebView create(final Activity a) {
        WebView w = new WebView(a);
        WebSettings s = w.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(false);
        s.setForceDark(WebSettings.FORCE_DARK_OFF);
        w.setBackgroundColor(0xFF0B0E1C);
        w.setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null);
        w.setOverScrollMode(android.view.View.OVER_SCROLL_NEVER);
        w.setWebViewClient(new WebViewClient() {
            @Override public WebResourceResponse shouldInterceptRequest(WebView v, WebResourceRequest r) {
                Uri u = r.getUrl();
                if (!HOST.equals(u.getHost())) return empty(403);
                String p = u.getPath();
                if (p == null || !p.startsWith("/assets/")) return empty(404);
                String name = p.substring(8);
                try {
                    InputStream in = a.getAssets().open(name);
                    return new WebResourceResponse(mime(name), "utf-8", in);
                } catch (IOException e) { return empty(404); }
            }
            @Override public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
                return !HOST.equals(r.getUrl().getHost());
            }
        });
        w.addJavascriptInterface(new Bridge(a), "Android");
        return w;
    }
}
