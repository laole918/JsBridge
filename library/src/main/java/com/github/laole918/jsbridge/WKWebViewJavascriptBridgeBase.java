package com.github.laole918.jsbridge;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

public class WKWebViewJavascriptBridgeBase {

    private static final String TAG = WKWebViewJavascriptBridgeBase.class.getSimpleName();

    private static boolean logging = false;
    private static int logMaxLength = 500;

    public static void enableLogging() {
        logging = true;
    }

    public static void setLogMaxLength(int length) {
        logMaxLength = length;
    }

    public interface WVJBResponseCallback {
        void callback(Object responseData);
    }

    public interface WVJBHandler {
        void handle(Object data, WVJBResponseCallback callback);
    }

    public static class WVJBMessage {
        private Object data;
        private String callbackId;
        private String handlerName;

        private String responseId;
        private Object responseData;
    }

    private final WKWebView wkWebView;
    private ArrayList<WVJBMessage> startupMessageQueue = new ArrayList<>();
    private HashMap<String, WVJBResponseCallback> responseCallbacks = new HashMap<>();
    protected HashMap<String, WVJBHandler> messageHandlers = new HashMap<>();
    private long uniqueId = 0;

    public WKWebViewJavascriptBridgeBase(WKWebView wkWebView) {
        this.wkWebView = wkWebView;
    }

    public void reset() {
        startupMessageQueue = null;
        responseCallbacks = new HashMap<>();
        uniqueId = 0;
    }

    public void sendData(String handlerName, Object data, WVJBResponseCallback callback) {
        WVJBMessage message = new WVJBMessage();
        message.handlerName = handlerName;

        if (data != null) {
            message.data = data;
        }

        if (callback != null) {
            uniqueId += 1;
            String callbackId = String.format("native_iOS_cb_%s", uniqueId);
            responseCallbacks.put(callbackId, callback);
            message.callbackId = callbackId;
        }

        queueMessage(message);
    }

    public void flushMessageQueue(String messageQueueString) {
        if (messageQueueString == null || messageQueueString.length() == 0) {
            Log.d(TAG, "WARNING: ObjC got nil while fetching the message queue JSON from webview. This can happen if the WebViewJavascriptBridge JS is not currently present in the webview, e.g if the webview just loaded a new page.");
            return;
        }
        ArrayList<WVJBMessage> messages = deserializeMessageJSON(messageQueueString);
        for (WVJBMessage message : messages) {
            log("RCVD", message);
            String responseId = message.responseId;
            if  (responseId != null) {
                WVJBResponseCallback callback = responseCallbacks.get(responseId);
                if (callback == null) continue;

                callback.callback(message.responseData);
                responseCallbacks.remove(responseId);
            } else {
                WVJBResponseCallback callback;
                String callbackId = message.callbackId;
                if (callbackId != null) {
                    callback = new WVJBResponseCallback() {
                        @Override
                        public void callback(Object responseData) {
                            WVJBMessage msg = new WVJBMessage();
                            msg.responseId = callbackId;
                            msg.responseData = responseData;
                            queueMessage(msg);
                        }
                    };
                } else {
                    callback = new WVJBResponseCallback() {
                        @Override
                        public void callback(Object responseData) {
                            // no logic
                        }
                    };
                }

                String handlerName = message.handlerName;
                if (handlerName == null) continue;
                WVJBHandler handler = messageHandlers.get(handlerName);
                if (handler == null) {
                    log("RCVD", String.format("NoHandlerException, No handler for message from JS: %s", message));
                    continue;
                }

                handler.handle(message.data, callback);
            }
        }
    }

    public void injectJavascriptFile() {
        String js = WKWebViewJavascriptBridgeJS.WKWebViewJavascriptBridgeJS;
        wkWebView.evaluateJavascript(js, new WKWebView.JavascriptCallback() {
            @Override
            public void onReceiveValue(String value) {
                if (startupMessageQueue != null) {
                    for (WVJBMessage message : startupMessageQueue) {
                        dispatchMessage(message);
                    }
                }
                startupMessageQueue = null;
            }
        });
    }

    // MARK: - Private
    private void queueMessage(WVJBMessage message) {
        if (startupMessageQueue == null) {
            dispatchMessage(message);
        } else {
            startupMessageQueue.add(message);
        }
    }

    private void dispatchMessage(WVJBMessage message) {
        String messageJSON = serializeMessage(message);
        log("SEND", messageJSON);

        messageJSON = messageJSON.replace("\\", "\\\\");
        messageJSON = messageJSON.replace("\"", "\\\"");
        messageJSON = messageJSON.replace("\'", "\\\'");
        messageJSON = messageJSON.replace("\n", "\\n");
        messageJSON = messageJSON.replace("\r", "\\r");
        messageJSON = messageJSON.replace("\f", "\\f");
        messageJSON = messageJSON.replace("\u2028", "\\u2028");
        messageJSON = messageJSON.replace("\u2029", "\\u2029");

        String javascriptCommand = String.format("WKWebViewJavascriptBridge._handleMessageFromiOS('%s');", messageJSON);
        wkWebView.evaluateJavascript(javascriptCommand);
    }

    // MARK: - JSON
    private String serializeMessage(WVJBMessage message) {
        JSONObject jsonObject = new JSONObject();
        try {
            if (message.data != null) {
                jsonObject.put("data", message.data);
            }
            if (message.callbackId != null) {
                jsonObject.put("callbackID", message.callbackId);
            }
            if (message.handlerName != null) {
                jsonObject.put("handlerName", message.handlerName);
            }
            if (message.responseId != null) {
                jsonObject.put("responseID", message.responseId);
            }
            if (message.responseData != null) {
                jsonObject.put("responseData", message.responseData);
            }
        } catch (Exception ignore) {}
        return jsonObject.toString();
    }

    private ArrayList<WVJBMessage> deserializeMessageJSON(String messageJSON) {
        ArrayList<WVJBMessage> messages = new ArrayList<>();
        try {
            if (messageJSON.startsWith("\"")
                    && messageJSON.endsWith("\"")) {
                messageJSON = messageJSON.substring(1, messageJSON.length() - 1)
                        .replaceAll("\\\\", "");
            }
            JSONArray jsonArray = new JSONArray(messageJSON);
            int length = jsonArray.length();
            for (int i = 0; i < length; i ++) {
                try {
                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                    WVJBMessage message = new WVJBMessage();
                    if (jsonObject.has("data")) {
                        message.data = jsonObject.get("data");
                    }
                    if (jsonObject.has("callbackID")) {
                        message.callbackId = jsonObject.getString("callbackID");
                    }
                    if (jsonObject.has("handlerName")) {
                        message.handlerName = jsonObject.getString("handlerName");
                    }
                    if (jsonObject.has("responseID")) {
                        message.responseId = jsonObject.getString("responseID");
                    }
                    if (jsonObject.has("responseData")) {
                        message.responseData = jsonObject.get("responseData");
                    }
                    messages.add(message);
                } catch (Exception ignore) {}
            }
        } catch (Exception ignore) {}
        return messages;
    }

    // MARK: - Log
    private void log(String action, Object json) {
        if (!logging) { return; }
        String message;
        if (json instanceof String) {
            message = (String) json;
        } else if (json instanceof WVJBMessage){
            message = serializeMessage((WVJBMessage) json);
        } else {
            message = String.valueOf(json);
        }
        if (message.length() > logMaxLength) {
            Log.d(TAG, String.format("%s: %s [...]", action, message.substring(0, logMaxLength)));
        } else {
            Log.d(TAG, String.format("%s: %s", action, message));
        }
    }
}
