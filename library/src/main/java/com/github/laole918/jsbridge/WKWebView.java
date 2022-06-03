package com.github.laole918.jsbridge;

import android.os.Looper;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebSettings;
import android.webkit.WebView;

import java.lang.ref.WeakReference;

public class WKWebView {

    public interface JavascriptCallback {
        void onReceiveValue(String value);
    }

    public interface ScriptMessageHandler {
        void onReceiveScriptMessage(String name, String message);
    }

    private final WeakReference<WebView> webView;

    public WKWebView(WebView webView) {
        this.webView = new WeakReference<>(webView);
        setJavaScriptEnabled();
    }

    private void setJavaScriptEnabled() {
        if (webView() != null) {
            WebSettings settings = webView().getSettings();
            if (!settings.getJavaScriptEnabled()) {
                settings.setJavaScriptEnabled(true);
            }
        }
    }

    private WebView webView() {
        return webView.get();
    }

    private void runOnUiThread(Runnable action) {
        if (webView() != null) {
            webView().post(action);
        }
    }

    public void evaluateJavascript(String script) {
        evaluateJavascript(script, null);
    }

    public void evaluateJavascript(String script, JavascriptCallback callback) {
        if (Looper.getMainLooper() == Looper.myLooper()) {
            _evaluateJavascript(script, callback);
        } else {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    _evaluateJavascript(script, callback);
                }
            });
        }
    }

    private void _evaluateJavascript(String script, JavascriptCallback callback) {
        if (webView() != null) {
            webView().evaluateJavascript(script, new ValueCallback<String>() {
                @Override
                public void onReceiveValue(String s) {
                    if (callback != null) {
                        callback.onReceiveValue(s);
                    }
                }
            });
        }
    }

    public void addScriptMessageHandler(ScriptMessageHandler handler, String name) {
        if (webView() != null) {
            webView().addJavascriptInterface(new WKJavascriptInterface(name, handler), name);
        }
    }

    public void removeScriptMessageHandler(String name) {
        if (webView() != null) {
            webView().removeJavascriptInterface(name);
        }
    }

    private static class WKJavascriptInterface {

        private final String name;
        private final WeakReference<ScriptMessageHandler> handler;

        public WKJavascriptInterface(String name, ScriptMessageHandler handler) {
            this.name = name;
            this.handler = new WeakReference<>(handler);
        }

        private ScriptMessageHandler handler() {
            return handler.get();
        }

        @JavascriptInterface
        public void postMessage(String message) {
            if (handler() != null) {
                handler().onReceiveScriptMessage(name, message);
            }
        }
    }
}
