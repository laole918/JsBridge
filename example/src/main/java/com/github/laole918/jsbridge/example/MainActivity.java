package com.github.laole918.jsbridge.example;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.github.laole918.jsbridge.WKWebViewJavascriptBridge;
import com.github.laole918.jsbridge.WKWebViewJavascriptBridgeBase;

import org.json.JSONObject;

import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private WebView webView;
    private WKWebViewJavascriptBridge bridge;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        setContentView(R.layout.activity_main);
        if (BuildConfig.DEBUG) {
            WebView.setWebContentsDebuggingEnabled(true);
            WKWebViewJavascriptBridge.enableLogging();
        }
        webView = findViewById(R.id.webView);

        Button callbackBtn = findViewById(R.id.callbackBtn);
        callbackBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                callHandler();
            }
        });
        Button reloadBtn = findViewById(R.id.reloadBtn);
        reloadBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                reloadWebView();
            }
        });

        webView.setInitialScale(100);
        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient());

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setAllowContentAccess(true);
        settings.setAllowFileAccess(true);
        settings.setDatabaseEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccessFromFileURLs(true);
        settings.setAllowUniversalAccessFromFileURLs(true);

        bridge = new WKWebViewJavascriptBridge(webView);
        bridge.registerHandler("testiOSCallback", new WKWebViewJavascriptBridgeBase.WVJBHandler() {
            @Override
            public void handle(Object data, WKWebViewJavascriptBridgeBase.WVJBResponseCallback callback) {
                Log.d(TAG, String.format("testiOSCallback called: %s", data == null ? "null" : data.toString()));
                callback.callback("Response from testiOSCallback");
            }
        });
        HashMap<String, String> data = new HashMap<>();
        data.put("foo", "before ready");
        bridge.callHandler("testJavascriptHandler", new JSONObject(data), null);

        webView.loadUrl("file:///android_asset/Demo.html");
    }

    private void callHandler() {
        HashMap<String, String> data = new HashMap<>();
        data.put("greetingFromiOS", "Hi there, JS!");
        bridge.callHandler("testJavascriptHandler", new JSONObject(data), new WKWebViewJavascriptBridgeBase.WVJBResponseCallback() {
            @Override
            public void callback(Object responseData) {
                Log.d(TAG, String.format("testJavascriptHandler responded: %s)", responseData == null ? "null" : responseData.toString()));
            }
        });
    }

    private void reloadWebView() {
        webView.reload();
    }
}