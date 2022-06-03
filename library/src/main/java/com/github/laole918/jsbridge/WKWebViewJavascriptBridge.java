package com.github.laole918.jsbridge;

import android.util.Log;
import android.webkit.WebView;

public class WKWebViewJavascriptBridge implements WKWebView.ScriptMessageHandler {

    private static final String TAG = WKWebViewJavascriptBridge.class.getSimpleName();

    public static void enableLogging() {
        WKWebViewJavascriptBridgeBase.enableLogging();
    }

    private static final String iOS_Native_InjectJavascript = "iOS_Native_InjectJavascript";
    private static final String iOS_Native_FlushMessageQueue = "iOS_Native_FlushMessageQueue";

    private final WKWebView wkWebView;
    private final WKWebViewJavascriptBridgeBase base;

    public WKWebViewJavascriptBridge(WebView webView) {
        this.wkWebView = new WKWebView(webView);
        base = new WKWebViewJavascriptBridgeBase(wkWebView);
        addScriptMessageHandlers();
    }

    public void destroy() {
        removeScriptMessageHandlers();
    }

    // MARK: - Public Funcs
    public void reset() {
        base.reset();
    }

    public void registerHandler(String handlerName, WKWebViewJavascriptBridgeBase.WVJBHandler handler) {
        base.messageHandlers.put(handlerName, handler);
    }

    public WKWebViewJavascriptBridgeBase.WVJBHandler removeHandler(String handlerName) {
        return base.messageHandlers.remove(handlerName);
    }

    public void callHandler(String handlerName, Object data, WKWebViewJavascriptBridgeBase.WVJBResponseCallback callback) {
        base.sendData(handlerName, data, callback);
    }

    // MARK: - Private Funcs
    private void flushMessageQueue() {
        wkWebView.evaluateJavascript("WKWebViewJavascriptBridge._fetchQueue();", new WKWebView.JavascriptCallback() {
            @Override
            public void onReceiveValue(String result) {
                if (result == null || result.length() == 0) {
                    Log.e(TAG, "WARNING: Error when trying to fetch data from WKWebView");
                }
                base.flushMessageQueue(result);
            }
        });
    }

    private void addScriptMessageHandlers() {
        wkWebView.addScriptMessageHandler(this, iOS_Native_InjectJavascript);
        wkWebView.addScriptMessageHandler(this, iOS_Native_FlushMessageQueue);
    }

    private void removeScriptMessageHandlers() {
        wkWebView.removeScriptMessageHandler(iOS_Native_InjectJavascript);
        wkWebView.removeScriptMessageHandler(iOS_Native_FlushMessageQueue);
    }

    @Override
    public void onReceiveScriptMessage(String name, String message) {
        if (name.equals(iOS_Native_InjectJavascript)) {
            base.injectJavascriptFile();
        }

        if (name.equals(iOS_Native_FlushMessageQueue)) {
            flushMessageQueue();
        }
    }

}
