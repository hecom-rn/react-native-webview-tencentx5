package indi.fan.webviewx5;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.DownloadManager;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.Manifest;
import android.net.http.SslError;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Message;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import com.tencent.smtt.export.external.interfaces.ConsoleMessage;
import com.tencent.smtt.sdk.CookieManager;
import com.tencent.smtt.sdk.DownloadListener;
import com.tencent.smtt.sdk.GeolocationPermissions;
import com.tencent.smtt.export.external.interfaces.GeolocationPermissionsCallback;
import android.webkit.JavascriptInterface;
// import android.webkit.RenderProcessGoneDetail;

import com.tencent.smtt.export.external.interfaces.SslErrorHandler;
import android.webkit.PermissionRequest;
import android.webkit.URLUtil;
import com.tencent.smtt.sdk.ValueCallback;
import com.tencent.smtt.sdk.WebChromeClient;
import com.tencent.smtt.export.external.interfaces.IX5WebChromeClient;
import com.tencent.smtt.export.external.interfaces.IX5WebChromeClient.CustomViewCallback;
// import android.webkit.WebResourceRequest;
import com.tencent.smtt.export.external.interfaces.WebResourceRequest;
import com.tencent.smtt.export.external.interfaces.WebResourceResponse;
import com.tencent.smtt.sdk.WebSettings;
import com.tencent.smtt.sdk.WebView;
import com.tencent.smtt.sdk.WebViewClient;
// import com.tencent.smtt.sdk.WebViewClient.RenderProcessGoneDetail;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;
import androidx.core.util.Pair;

import com.facebook.common.logging.FLog;
import com.facebook.react.views.scroll.ScrollEvent;
import com.facebook.react.views.scroll.ScrollEventType;
import com.facebook.react.views.scroll.OnScrollDispatchHelper;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.CatalystInstance;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableMapKeySetIterator;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeArray;
import com.facebook.react.bridge.WritableNativeMap;
import com.facebook.react.common.MapBuilder;
import com.facebook.react.common.build.ReactBuildConfig;
import com.facebook.react.module.annotations.ReactModule;
import com.facebook.react.uimanager.SimpleViewManager;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.UIManagerHelper;
import com.facebook.react.uimanager.UIManagerModule;
import com.facebook.react.uimanager.annotations.ReactProp;
import com.facebook.react.uimanager.events.ContentSizeChangeEvent;
import com.facebook.react.uimanager.events.Event;
import com.facebook.react.uimanager.events.EventDispatcher;
import indi.fan.webviewx5.RNX5WebViewModule.ShouldOverrideUrlLoadingLock.ShouldOverrideCallbackState;
import indi.fan.webviewx5.events.TopLoadingErrorEvent;
import indi.fan.webviewx5.events.TopHttpErrorEvent;
import indi.fan.webviewx5.events.TopLoadingFinishEvent;
import indi.fan.webviewx5.events.TopLoadingProgressEvent;
import indi.fan.webviewx5.events.TopLoadingStartEvent;
import indi.fan.webviewx5.events.TopMessageEvent;
import indi.fan.webviewx5.events.TopShouldStartLoadWithRequestEvent;
import indi.fan.webviewx5.events.TopRenderProcessGoneEvent;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;



/**
 * Manages instances of {@link WebView}
 * <p>
 * Can accept following commands:
 * - GO_BACK
 * - GO_FORWARD
 * - RELOAD
 * - LOAD_URL
 * <p>
 * {@link WebView} instances could emit following direct events:
 * - topLoadingFinish
 * - topLoadingStart
 * - topLoadingStart
 * - topLoadingProgress
 * - topShouldStartLoadWithRequest
 * <p>
 * Each event will carry the following properties:
 * - target - view's react tag
 * - url - url set for the webview
 * - loading - whether webview is in a loading state
 * - title - title of the current page
 * - canGoBack - boolean, whether there is anything on a history stack to go back
 * - canGoForward - boolean, whether it is possible to request GO_FORWARD command
 */
@ReactModule(name = RNX5WebViewManager.REACT_CLASS)
public class RNX5WebViewManager extends SimpleViewManager<WebView> {
  private static final String TAG = "RNX5WebViewManager";

  public static final int COMMAND_GO_BACK = 1;
  public static final int COMMAND_GO_FORWARD = 2;
  public static final int COMMAND_RELOAD = 3;
  public static final int COMMAND_STOP_LOADING = 4;
  public static final int COMMAND_POST_MESSAGE = 5;
  public static final int COMMAND_INJECT_JAVASCRIPT = 6;
  public static final int COMMAND_LOAD_URL = 7;
  public static final int COMMAND_FOCUS = 8;

  // android commands
  public static final int COMMAND_CLEAR_FORM_DATA = 1000;
  public static final int COMMAND_CLEAR_CACHE = 1001;
  public static final int COMMAND_CLEAR_HISTORY = 1002;

  protected static final String REACT_CLASS = "RNX5WebView";
  protected static final String HTML_ENCODING = "UTF-8";
  protected static final String HTML_MIME_TYPE = "text/html";
  protected static final String JAVASCRIPT_INTERFACE = "ReactNativeWebView";
  protected static final String HTTP_METHOD_POST = "POST";
  // Use `webView.loadUrl("about:blank")` to reliably reset the view
  // state and release page resources (including any running JavaScript).
  protected static final String BLANK_URL = "about:blank";
  protected static final int SHOULD_OVERRIDE_URL_LOADING_TIMEOUT = 250;
  protected WebViewConfig mWebViewConfig;

  protected RNCWebChromeClient mWebChromeClient = null;
  protected boolean mAllowsFullscreenVideo = false;
  protected @Nullable String mUserAgent = null;
  protected @Nullable String mUserAgentWithApplicationName = null;

  public RNX5WebViewManager() {
    mWebViewConfig = new WebViewConfig() {
      public void configWebView(WebView webView) {
      }
    };
  }

  public RNX5WebViewManager(WebViewConfig webViewConfig) {
    mWebViewConfig = webViewConfig;
  }

  protected static void dispatchEvent(WebView webView, Event event) {
    ReactContext reactContext = (ReactContext) webView.getContext();
    EventDispatcher eventDispatcher = UIManagerHelper.getEventDispatcher(reactContext, webView.getId());
    eventDispatcher.dispatchEvent(event);
  }

  @Override
  public String getName() {
    return REACT_CLASS;
  }

  protected RNX5WebView createRNX5WebViewInstance(ThemedReactContext reactContext) {
    return new RNX5WebView(reactContext);
  }

  @Override
  @TargetApi(Build.VERSION_CODES.LOLLIPOP)
  protected WebView createViewInstance(ThemedReactContext reactContext) {
    RNX5WebView webView = createRNX5WebViewInstance(reactContext);
    setupWebChromeClient(reactContext, webView);
    reactContext.addLifecycleEventListener(webView);
    mWebViewConfig.configWebView(webView);
    WebSettings settings = webView.getSettings();
    settings.setBuiltInZoomControls(true);
    settings.setDisplayZoomControls(false);
    settings.setDomStorageEnabled(true);
    settings.setSupportMultipleWindows(true);

    settings.setAllowFileAccess(false);
    settings.setAllowContentAccess(false);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
      settings.setAllowFileAccessFromFileURLs(false);
      setAllowUniversalAccessFromFileURLs(webView, false);
    }
    setMixedContentMode(webView, "never");

    // Fixes broken full-screen modals/galleries due to body height being 0.
    webView.setLayoutParams(
      new LayoutParams(LayoutParams.MATCH_PARENT,
        LayoutParams.MATCH_PARENT));

    if (ReactBuildConfig.DEBUG && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
      WebView.setWebContentsDebuggingEnabled(true);
    }

    webView.setDownloadListener(new DownloadListener() {
      public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimetype, long contentLength) {
        webView.setIgnoreErrFailedForThisURL(url);

        RNX5WebViewModule module = getModule(reactContext);

        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));

        String fileName = URLUtil.guessFileName(url, contentDisposition, mimetype);
        String downloadMessage = "Downloading " + fileName;

        //Attempt to add cookie, if it exists
        URL urlObj = null;
        try {
          urlObj = new URL(url);
          String baseUrl = urlObj.getProtocol() + "://" + urlObj.getHost();
          String cookie = CookieManager.getInstance().getCookie(baseUrl);
          request.addRequestHeader("Cookie", cookie);
        } catch (MalformedURLException e) {
          System.out.println("Error getting cookie for DownloadManager: " + e.toString());
          e.printStackTrace();
        }

        //Finish setting up request
        request.addRequestHeader("User-Agent", userAgent);
        request.setTitle(fileName);
        request.setDescription(downloadMessage);
        request.allowScanningByMediaScanner();
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);

        module.setDownloadRequest(request);

        if (module.grantFileDownloaderPermissions()) {
          module.downloadFile();
        }
      }
    });

    return webView;
  }

  @ReactProp(name = "javaScriptEnabled")
  public void setJavaScriptEnabled(WebView view, boolean enabled) {
    view.getSettings().setJavaScriptEnabled(enabled);
  }

  @ReactProp(name = "setSupportMultipleWindows")
  public void setSupportMultipleWindows(WebView view, boolean enabled){
    view.getSettings().setSupportMultipleWindows(enabled);
  }

  @ReactProp(name = "showsHorizontalScrollIndicator")
  public void setShowsHorizontalScrollIndicator(WebView view, boolean enabled) {
    view.setHorizontalScrollBarEnabled(enabled);
  }

  @ReactProp(name = "showsVerticalScrollIndicator")
  public void setShowsVerticalScrollIndicator(WebView view, boolean enabled) {
    view.setVerticalScrollBarEnabled(enabled);
  }

  @ReactProp(name = "cacheEnabled")
  public void setCacheEnabled(WebView view, boolean enabled) {
    if (enabled) {
      Context ctx = view.getContext();
      if (ctx != null) {
        view.getSettings().setAppCachePath(ctx.getCacheDir().getAbsolutePath());
        view.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
        view.getSettings().setAppCacheEnabled(true);
      }
    } else {
      view.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
      view.getSettings().setAppCacheEnabled(false);
    }
  }

  @ReactProp(name = "cacheMode")
  public void setCacheMode(WebView view, String cacheModeString) {
    Integer cacheMode;
    switch (cacheModeString) {
      case "LOAD_CACHE_ONLY":
        cacheMode = WebSettings.LOAD_CACHE_ONLY;
        break;
      case "LOAD_CACHE_ELSE_NETWORK":
        cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK;
        break;
      case "LOAD_NO_CACHE":
        cacheMode = WebSettings.LOAD_NO_CACHE;
        break;
      case "LOAD_DEFAULT":
      default:
        cacheMode = WebSettings.LOAD_DEFAULT;
        break;
    }
    view.getSettings().setCacheMode(cacheMode);
  }

  @ReactProp(name = "androidHardwareAccelerationDisabled")
  public void setHardwareAccelerationDisabled(WebView view, boolean disabled) {
    if (disabled) {
      view.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }
  }

  @ReactProp(name = "androidLayerType")
  public void setLayerType(WebView view, String layerTypeString) {
    int layerType = View.LAYER_TYPE_NONE;
    switch (layerTypeString) {
        case "hardware":
          layerType = View.LAYER_TYPE_HARDWARE;
          break;
        case "software":
          layerType = View.LAYER_TYPE_SOFTWARE;
          break;
    }
    view.setLayerType(layerType, null);
  }


  @ReactProp(name = "overScrollMode")
  public void setOverScrollMode(WebView view, String overScrollModeString) {
    Integer overScrollMode;
    switch (overScrollModeString) {
      case "never":
        overScrollMode = View.OVER_SCROLL_NEVER;
        break;
      case "content":
        overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS;
        break;
      case "always":
      default:
        overScrollMode = View.OVER_SCROLL_ALWAYS;
        break;
    }
    view.setOverScrollMode(overScrollMode);
  }

  @ReactProp(name = "thirdPartyCookiesEnabled")
  public void setThirdPartyCookiesEnabled(WebView view, boolean enabled) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      CookieManager.getInstance().setAcceptThirdPartyCookies(view, enabled);
    }
  }

  @ReactProp(name = "textZoom")
  public void setTextZoom(WebView view, int value) {
    view.getSettings().setTextZoom(value);
  }

  @ReactProp(name = "scalesPageToFit")
  public void setScalesPageToFit(WebView view, boolean enabled) {
    view.getSettings().setLoadWithOverviewMode(enabled);
    view.getSettings().setUseWideViewPort(enabled);
  }

  @ReactProp(name = "domStorageEnabled")
  public void setDomStorageEnabled(WebView view, boolean enabled) {
    view.getSettings().setDomStorageEnabled(enabled);
  }

  @ReactProp(name = "userAgent")
  public void setUserAgent(WebView view, @Nullable String userAgent) {
    if (userAgent != null) {
      mUserAgent = userAgent;
    } else {
      mUserAgent = null;
    }
    this.setUserAgentString(view);
  }

  @ReactProp(name = "applicationNameForUserAgent")
  public void setApplicationNameForUserAgent(WebView view, @Nullable String applicationName) {
    if(applicationName != null) {
      if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
        String defaultUserAgent = WebSettings.getDefaultUserAgent(view.getContext());
        mUserAgentWithApplicationName = defaultUserAgent + " " + applicationName;
      }
    } else {
      mUserAgentWithApplicationName = null;
    }
    this.setUserAgentString(view);
  }

  protected void setUserAgentString(WebView view) {
    if(mUserAgent != null) {
      view.getSettings().setUserAgentString(mUserAgent);
    } else if(mUserAgentWithApplicationName != null) {
      view.getSettings().setUserAgentString(mUserAgentWithApplicationName);
    } else if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
      // handle unsets of `userAgent` prop as long as device is >= API 17
      view.getSettings().setUserAgentString(WebSettings.getDefaultUserAgent(view.getContext()));
    }
  }

  @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
  @ReactProp(name = "mediaPlaybackRequiresUserAction")
  public void setMediaPlaybackRequiresUserAction(WebView view, boolean requires) {
    view.getSettings().setMediaPlaybackRequiresUserGesture(requires);
  }

  @ReactProp(name = "javaScriptCanOpenWindowsAutomatically")
  public void setJavaScriptCanOpenWindowsAutomatically(WebView view, boolean enabled) {
    view.getSettings().setJavaScriptCanOpenWindowsAutomatically(enabled);
  }

  @ReactProp(name = "allowFileAccessFromFileURLs")
  public void setAllowFileAccessFromFileURLs(WebView view, boolean allow) {
    view.getSettings().setAllowFileAccessFromFileURLs(allow);
  }

  @ReactProp(name = "allowUniversalAccessFromFileURLs")
  public void setAllowUniversalAccessFromFileURLs(WebView view, boolean allow) {
    view.getSettings().setAllowUniversalAccessFromFileURLs(allow);
  }

  @ReactProp(name = "saveFormDataDisabled")
  public void setSaveFormDataDisabled(WebView view, boolean disable) {
    view.getSettings().setSaveFormData(!disable);
  }

  @ReactProp(name = "injectedJavaScript")
  public void setInjectedJavaScript(WebView view, @Nullable String injectedJavaScript) {
    ((RNX5WebView) view).setInjectedJavaScript(injectedJavaScript);
  }

  @ReactProp(name = "injectedJavaScriptBeforeContentLoaded")
  public void setInjectedJavaScriptBeforeContentLoaded(WebView view, @Nullable String injectedJavaScriptBeforeContentLoaded) {
    ((RNX5WebView) view).setInjectedJavaScriptBeforeContentLoaded(injectedJavaScriptBeforeContentLoaded);
  }

  @ReactProp(name = "injectedJavaScriptForMainFrameOnly")
  public void setInjectedJavaScriptForMainFrameOnly(WebView view, boolean enabled) {
    ((RNX5WebView) view).setInjectedJavaScriptForMainFrameOnly(enabled);
  }

  @ReactProp(name = "injectedJavaScriptBeforeContentLoadedForMainFrameOnly")
  public void setInjectedJavaScriptBeforeContentLoadedForMainFrameOnly(WebView view, boolean enabled) {
    ((RNX5WebView) view).setInjectedJavaScriptBeforeContentLoadedForMainFrameOnly(enabled);
  }

  @ReactProp(name = "messagingEnabled")
  public void setMessagingEnabled(WebView view, boolean enabled) {
    ((RNX5WebView) view).setMessagingEnabled(enabled);
  }

  @ReactProp(name = "messagingModuleName")
  public void setMessagingModuleName(WebView view, String moduleName) {
    ((RNX5WebView) view).setMessagingModuleName(moduleName);
  }

  @ReactProp(name = "incognito")
  public void setIncognito(WebView view, boolean enabled) {
    // Don't do anything when incognito is disabled
    if (!enabled) {
      return;
    }

    // Remove all previous cookies
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      CookieManager.getInstance().removeAllCookies(null);
    } else {
      CookieManager.getInstance().removeAllCookie();
    }

    // Disable caching
    view.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
    view.getSettings().setAppCacheEnabled(false);
    view.clearHistory();
    view.clearCache(true);

    // No form data or autofill enabled
    view.clearFormData();
    view.getSettings().setSavePassword(false);
    view.getSettings().setSaveFormData(false);
  }

  @ReactProp(name = "source")
  public void setSource(WebView view, @Nullable ReadableMap source) {
    if (source != null) {
      if (source.hasKey("html")) {
        String html = source.getString("html");
        String baseUrl = source.hasKey("baseUrl") ? source.getString("baseUrl") : "";
        view.loadDataWithBaseURL(baseUrl, html, HTML_MIME_TYPE, HTML_ENCODING, null);
        return;
      }
      if (source.hasKey("uri")) {
        String url = source.getString("uri");
        String previousUrl = view.getUrl();
        if (previousUrl != null && previousUrl.equals(url)) {
          return;
        }
        if (source.hasKey("method")) {
          String method = source.getString("method");
          if (method.equalsIgnoreCase(HTTP_METHOD_POST)) {
            byte[] postData = null;
            if (source.hasKey("body")) {
              String body = source.getString("body");
              try {
                postData = body.getBytes("UTF-8");
              } catch (UnsupportedEncodingException e) {
                postData = body.getBytes();
              }
            }
            if (postData == null) {
              postData = new byte[0];
            }
            view.postUrl(url, postData);
            return;
          }
        }
        HashMap<String, String> headerMap = new HashMap<>();
        if (source.hasKey("headers")) {
          ReadableMap headers = source.getMap("headers");
          ReadableMapKeySetIterator iter = headers.keySetIterator();
          while (iter.hasNextKey()) {
            String key = iter.nextKey();
            if ("user-agent".equals(key.toLowerCase(Locale.ENGLISH))) {
              if (view.getSettings() != null) {
                view.getSettings().setUserAgentString(headers.getString(key));
              }
            } else {
              headerMap.put(key, headers.getString(key));
            }
          }
        }
        view.loadUrl(url, headerMap);
        return;
      }
    }
    view.loadUrl(BLANK_URL);
  }

  @ReactProp(name = "onContentSizeChange")
  public void setOnContentSizeChange(WebView view, boolean sendContentSizeChangeEvents) {
    ((RNX5WebView) view).setSendContentSizeChangeEvents(sendContentSizeChangeEvents);
  }

  @ReactProp(name = "mixedContentMode")
  public void setMixedContentMode(WebView view, @Nullable String mixedContentMode) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      if (mixedContentMode == null || "never".equals(mixedContentMode)) {
        // view.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        view.getSettings().setMixedContentMode(1);
      } else if ("always".equals(mixedContentMode)) {
        // view.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        view.getSettings().setMixedContentMode(0);
      } else if ("compatibility".equals(mixedContentMode)) {
        // view.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        view.getSettings().setMixedContentMode(2);
      }
    }
  }

  @ReactProp(name = "urlPrefixesForDefaultIntent")
  public void setUrlPrefixesForDefaultIntent(
    WebView view,
    @Nullable ReadableArray urlPrefixesForDefaultIntent) {
    RNX5WebViewClient client = ((RNX5WebView) view).getRNX5WebViewClient();
    if (client != null && urlPrefixesForDefaultIntent != null) {
      client.setUrlPrefixesForDefaultIntent(urlPrefixesForDefaultIntent);
    }
  }

  @ReactProp(name = "allowsFullscreenVideo")
  public void setAllowsFullscreenVideo(
    WebView view,
    @Nullable Boolean allowsFullscreenVideo) {
    mAllowsFullscreenVideo = allowsFullscreenVideo != null && allowsFullscreenVideo;
    setupWebChromeClient((ReactContext)view.getContext(), view);
  }

  @ReactProp(name = "allowFileAccess")
  public void setAllowFileAccess(
    WebView view,
    @Nullable Boolean allowFileAccess) {
    view.getSettings().setAllowFileAccess(allowFileAccess != null && allowFileAccess);
  }

  @ReactProp(name = "geolocationEnabled")
  public void setGeolocationEnabled(
    WebView view,
    @Nullable Boolean isGeolocationEnabled) {
    view.getSettings().setGeolocationEnabled(isGeolocationEnabled != null && isGeolocationEnabled);
  }

  @ReactProp(name = "onScroll")
  public void setOnScroll(WebView view, boolean hasScrollEvent) {
    ((RNX5WebView) view).setHasScrollEvent(hasScrollEvent);
  }

  @Override
  protected void addEventEmitters(ThemedReactContext reactContext, WebView view) {
    // Do not register default touch emitter and let WebView implementation handle touches
    view.setWebViewClient(new RNX5WebViewClient());
  }

  @Override
  public Map getExportedCustomDirectEventTypeConstants() {
    Map export = super.getExportedCustomDirectEventTypeConstants();
    if (export == null) {
      export = MapBuilder.newHashMap();
    }
    export.put(TopLoadingProgressEvent.EVENT_NAME, MapBuilder.of("registrationName", "onLoadingProgress"));
    export.put(TopShouldStartLoadWithRequestEvent.EVENT_NAME, MapBuilder.of("registrationName", "onShouldStartLoadWithRequest"));
    export.put(ScrollEventType.getJSEventName(ScrollEventType.SCROLL), MapBuilder.of("registrationName", "onScroll"));
    export.put(TopHttpErrorEvent.EVENT_NAME, MapBuilder.of("registrationName", "onHttpError"));
    // export.put(TopRenderProcessGoneEvent.EVENT_NAME, MapBuilder.of("registrationName", "onRenderProcessGone"));
    return export;
  }

  @Override
  public @Nullable
  Map<String, Integer> getCommandsMap() {
    return MapBuilder.<String, Integer>builder()
      .put("goBack", COMMAND_GO_BACK)
      .put("goForward", COMMAND_GO_FORWARD)
      .put("reload", COMMAND_RELOAD)
      .put("stopLoading", COMMAND_STOP_LOADING)
      .put("postMessage", COMMAND_POST_MESSAGE)
      .put("injectJavaScript", COMMAND_INJECT_JAVASCRIPT)
      .put("loadUrl", COMMAND_LOAD_URL)
      .put("requestFocus", COMMAND_FOCUS)
      .put("clearFormData", COMMAND_CLEAR_FORM_DATA)
      .put("clearCache", COMMAND_CLEAR_CACHE)
      .put("clearHistory", COMMAND_CLEAR_HISTORY)
      .build();
  }

  @Override
  public void receiveCommand(WebView root, int commandId, @Nullable ReadableArray args) {
    switch (commandId) {
      case COMMAND_GO_BACK:
        root.goBack();
        break;
      case COMMAND_GO_FORWARD:
        root.goForward();
        break;
      case COMMAND_RELOAD:
        root.reload();
        break;
      case COMMAND_STOP_LOADING:
        root.stopLoading();
        break;
      case COMMAND_POST_MESSAGE:
        try {
          RNX5WebView reactWebView = (RNX5WebView) root;
          JSONObject eventInitDict = new JSONObject();
          eventInitDict.put("data", args.getString(0));
          reactWebView.evaluateJavascriptWithFallback("(function () {" +
            "var event;" +
            "var data = " + eventInitDict.toString() + ";" +
            "try {" +
            "event = new MessageEvent('message', data);" +
            "} catch (e) {" +
            "event = document.createEvent('MessageEvent');" +
            "event.initMessageEvent('message', true, true, data.data, data.origin, data.lastEventId, data.source);" +
            "}" +
            "document.dispatchEvent(event);" +
            "})();");
        } catch (JSONException e) {
          throw new RuntimeException(e);
        }
        break;
      case COMMAND_INJECT_JAVASCRIPT:
        RNX5WebView reactWebView = (RNX5WebView) root;
        reactWebView.evaluateJavascriptWithFallback(args.getString(0));
        break;
      case COMMAND_LOAD_URL:
        if (args == null) {
          throw new RuntimeException("Arguments for loading an url are null!");
        }
        ((RNX5WebView) root).progressChangedFilter.setWaitingForCommandLoadUrl(false);
        root.loadUrl(args.getString(0));
        break;
      case COMMAND_FOCUS:
        root.requestFocus();
        break;
      case COMMAND_CLEAR_FORM_DATA:
        root.clearFormData();
        break;
      case COMMAND_CLEAR_CACHE:
        boolean includeDiskFiles = args != null && args.getBoolean(0);
        root.clearCache(includeDiskFiles);
        break;
      case COMMAND_CLEAR_HISTORY:
        root.clearHistory();
        break;
    }
  }

  @Override
  public void onDropViewInstance(WebView webView) {
    super.onDropViewInstance(webView);
    ((ThemedReactContext) webView.getContext()).removeLifecycleEventListener((RNX5WebView) webView);
    ((RNX5WebView) webView).cleanupCallbacksAndDestroy();
    mWebChromeClient = null;
  }

  public static RNX5WebViewModule getModule(ReactContext reactContext) {
    return reactContext.getNativeModule(RNX5WebViewModule.class);
  }

  protected void setupWebChromeClient(ReactContext reactContext, WebView webView) {
    if (mAllowsFullscreenVideo) {
      int initialRequestedOrientation = reactContext.getCurrentActivity().getRequestedOrientation();
      mWebChromeClient = new RNCWebChromeClient(reactContext, webView) {
        @Override
        public Bitmap getDefaultVideoPoster() {
          return Bitmap.createBitmap(50, 50, Bitmap.Config.ARGB_8888);
        }

        @Override
        public void onShowCustomView(View view, IX5WebChromeClient.CustomViewCallback callback) {
          if (mVideoView != null) {
            callback.onCustomViewHidden();
            return;
          }

          mVideoView = view;
          mCustomViewCallback = callback;

          mReactContext.getCurrentActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);

          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            mVideoView.setSystemUiVisibility(FULLSCREEN_SYSTEM_UI_VISIBILITY);
            mReactContext.getCurrentActivity().getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
          }

          mVideoView.setBackgroundColor(Color.BLACK);
          getRootView().addView(mVideoView, FULLSCREEN_LAYOUT_PARAMS);
          mWebView.setVisibility(View.GONE);

          mReactContext.addLifecycleEventListener(this);
        }

        @Override
        public void onHideCustomView() {
          if (mVideoView == null) {
            return;
          }

          mVideoView.setVisibility(View.GONE);
          getRootView().removeView(mVideoView);
          mCustomViewCallback.onCustomViewHidden();

          mVideoView = null;
          mCustomViewCallback = null;

          mWebView.setVisibility(View.VISIBLE);

          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            mReactContext.getCurrentActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
          }
          mReactContext.getCurrentActivity().setRequestedOrientation(initialRequestedOrientation);

          mReactContext.removeLifecycleEventListener(this);
        }
      };
      webView.setWebChromeClient(mWebChromeClient);
    } else {
      if (mWebChromeClient != null) {
        mWebChromeClient.onHideCustomView();
      }
      mWebChromeClient = new RNCWebChromeClient(reactContext, webView) {
        @Override
        public Bitmap getDefaultVideoPoster() {
          return Bitmap.createBitmap(50, 50, Bitmap.Config.ARGB_8888);
        }
      };
      webView.setWebChromeClient(mWebChromeClient);
    }
  }

  protected static class RNX5WebViewClient extends WebViewClient {

    protected boolean mLastLoadFailed = false;
    protected @Nullable
    ReadableArray mUrlPrefixesForDefaultIntent;
    protected RNX5WebView.ProgressChangedFilter progressChangedFilter = null;
    protected @Nullable String ignoreErrFailedForThisURL = null;

    public void setIgnoreErrFailedForThisURL(@Nullable String url) {
      ignoreErrFailedForThisURL = url;
    }

    @Override
    public void onPageFinished(WebView webView, String url) {
      super.onPageFinished(webView, url);

      if (!mLastLoadFailed) {
        RNX5WebView reactWebView = (RNX5WebView) webView;

        reactWebView.callInjectedJavaScript();

        emitFinishEvent(webView, url);
      }
    }

    @Override
    public void onPageStarted(WebView webView, String url, Bitmap favicon) {
      super.onPageStarted(webView, url, favicon);
      mLastLoadFailed = false;

      RNX5WebView reactWebView = (RNX5WebView) webView;
      reactWebView.callInjectedJavaScriptBeforeContentLoaded();

      dispatchEvent(
        webView,
        new TopLoadingStartEvent(
          webView.getId(),
          createWebViewEvent(webView, url)));
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
      final RNX5WebView RNX5WebView = (RNX5WebView) view;
      final boolean isJsDebugging = ((ReactContext) view.getContext()).getJavaScriptContextHolder().get() == 0;

      if (!isJsDebugging && RNX5WebView.mCatalystInstance != null) {
        final Pair<Integer, AtomicReference<ShouldOverrideCallbackState>> lock = RNX5WebViewModule.shouldOverrideUrlLoadingLock.getNewLock();
        final int lockIdentifier = lock.first;
        final AtomicReference<ShouldOverrideCallbackState> lockObject = lock.second;

        final WritableMap event = createWebViewEvent(view, url);
        event.putInt("lockIdentifier", lockIdentifier);
        RNX5WebView.sendDirectMessage("onShouldStartLoadWithRequest", event);

        try {
          assert lockObject != null;
          synchronized (lockObject) {
            final long startTime = SystemClock.elapsedRealtime();
            while (lockObject.get() == ShouldOverrideCallbackState.UNDECIDED) {
              if (SystemClock.elapsedRealtime() - startTime > SHOULD_OVERRIDE_URL_LOADING_TIMEOUT) {
                FLog.w(TAG, "Did not receive response to shouldOverrideUrlLoading in time, defaulting to allow loading.");
                RNX5WebViewModule.shouldOverrideUrlLoadingLock.removeLock(lockIdentifier);
                return false;
              }
              lockObject.wait(SHOULD_OVERRIDE_URL_LOADING_TIMEOUT);
            }
          }
        } catch (InterruptedException e) {
          FLog.e(TAG, "shouldOverrideUrlLoading was interrupted while waiting for result.", e);
          RNX5WebViewModule.shouldOverrideUrlLoadingLock.removeLock(lockIdentifier);
          return false;
        }

        final boolean shouldOverride = lockObject.get() == ShouldOverrideCallbackState.SHOULD_OVERRIDE;
        RNX5WebViewModule.shouldOverrideUrlLoadingLock.removeLock(lockIdentifier);

        return shouldOverride;
      } else {
        FLog.w(TAG, "Couldn't use blocking synchronous call for onShouldStartLoadWithRequest due to debugging or missing Catalyst instance, falling back to old event-and-load.");
        progressChangedFilter.setWaitingForCommandLoadUrl(true);
        dispatchEvent(
          view,
          new TopShouldStartLoadWithRequestEvent(
            view.getId(),
            createWebViewEvent(view, url)));
        return true;
      }
    }

    @TargetApi(Build.VERSION_CODES.N)
    // @Override
    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
      final String url = request.getUrl().toString();
      return this.shouldOverrideUrlLoading(view, url);
    }

    // @Override
    public void onReceivedSslError(final WebView webView, final SslErrorHandler handler, final SslError error) {
        // onReceivedSslError is called for most requests, per Android docs: https://developer.android.com/reference/android/webkit/WebViewClient#onReceivedSslError(android.webkit.WebView,%2520android.webkit.SslErrorHandler,%2520android.net.http.SslError)
        // WebView.getUrl() will return the top-level window URL.
        // If a top-level navigation triggers this error handler, the top-level URL will be the failing URL (not the URL of the currently-rendered page).
        // This is desired behavior. We later use these values to determine whether the request is a top-level navigation or a subresource request.
        String topWindowUrl = webView.getUrl();
        String failingUrl = error.getUrl();

        // Cancel request after obtaining top-level URL.
        // If request is cancelled before obtaining top-level URL, undesired behavior may occur.
        // Undesired behavior: Return value of WebView.getUrl() may be the current URL instead of the failing URL.
        handler.cancel();

        if (!topWindowUrl.equalsIgnoreCase(failingUrl)) {
          // If error is not due to top-level navigation, then do not call onReceivedError()
          Log.w("RNX5WebViewManager", "Resource blocked from loading due to SSL error. Blocked URL: "+failingUrl);
          return;
        }

        int code = error.getPrimaryError();
        String description = "";
        String descriptionPrefix = "SSL error: ";

        // https://developer.android.com/reference/android/net/http/SslError.html
        switch (code) {
          case SslError.SSL_DATE_INVALID:
            description = "The date of the certificate is invalid";
            break;
          case SslError.SSL_EXPIRED:
            description = "The certificate has expired";
            break;
          case SslError.SSL_IDMISMATCH:
            description = "Hostname mismatch";
            break;
          case SslError.SSL_INVALID:
            description = "A generic error occurred";
            break;
          case SslError.SSL_NOTYETVALID:
            description = "The certificate is not yet valid";
            break;
          case SslError.SSL_UNTRUSTED:
            description = "The certificate authority is not trusted";
            break;
          default:
            description = "Unknown SSL Error";
            break;
        }

        description = descriptionPrefix + description;

        this.onReceivedError(
          webView,
          code,
          description,
          failingUrl
        );
    }

    @Override
    public void onReceivedError(
      WebView webView,
      int errorCode,
      String description,
      String failingUrl) {

      if (ignoreErrFailedForThisURL != null
          && failingUrl.equals(ignoreErrFailedForThisURL)
          && errorCode == -1
          && description.equals("net::ERR_FAILED")) {

        // This is a workaround for a bug in the WebView.
        // See these chromium issues for more context:
        // https://bugs.chromium.org/p/chromium/issues/detail?id=1023678
        // https://bugs.chromium.org/p/chromium/issues/detail?id=1050635
        // This entire commit should be reverted once this bug is resolved in chromium.
        setIgnoreErrFailedForThisURL(null);
        return;
      }

      super.onReceivedError(webView, errorCode, description, failingUrl);
      mLastLoadFailed = true;

      // In case of an error JS side expect to get a finish event first, and then get an error event
      // Android WebView does it in the opposite way, so we need to simulate that behavior
      emitFinishEvent(webView, failingUrl);

      WritableMap eventData = createWebViewEvent(webView, failingUrl);
      eventData.putDouble("code", errorCode);
      eventData.putString("description", description);

      dispatchEvent(
        webView,
        new TopLoadingErrorEvent(webView.getId(), eventData));
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    // @Override
    public void onReceivedHttpError(
      WebView webView,
      WebResourceRequest request,
      WebResourceResponse errorResponse) {
      super.onReceivedHttpError(webView, request, errorResponse);

      if (request.isForMainFrame()) {
        WritableMap eventData = createWebViewEvent(webView, request.getUrl().toString());
        eventData.putInt("statusCode", errorResponse.getStatusCode());
        eventData.putString("description", errorResponse.getReasonPhrase());

        dispatchEvent(
          webView,
          new TopHttpErrorEvent(webView.getId(), eventData));
      }
    }

    // @TargetApi(Build.VERSION_CODES.O)
    // // @Override
    // public boolean onRenderProcessGone(WebView webView, WebViewClient.RenderProcessGoneDetail detail) {
    //     // WebViewClient.onRenderProcessGone was added in O.
    //     if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
    //         return false;
    //     }
    //     super.onRenderProcessGone(webView, detail);

    //     if(detail.didCrash()){
    //       Log.e("RNX5WebViewManager", "The WebView rendering process crashed.");
    //     }
    //     else{
    //       Log.w("RNX5WebViewManager", "The WebView rendering process was killed by the system.");
    //     }

    //     // if webView is null, we cannot return any event
    //     // since the view is already dead/disposed
    //     // still prevent the app crash by returning true.
    //     if(webView == null){
    //       return true;
    //     }

    //     WritableMap event = createWebViewEvent(webView, webView.getUrl());
    //     event.putBoolean("didCrash", detail.didCrash());

    //     dispatchEvent(
    //       webView,
    //       new TopRenderProcessGoneEvent(webView.getId(), event)
    //     );

    //     // returning false would crash the app.
    //     return true;
    // }

    protected void emitFinishEvent(WebView webView, String url) {
      dispatchEvent(
        webView,
        new TopLoadingFinishEvent(
          webView.getId(),
          createWebViewEvent(webView, url)));
    }

    protected WritableMap createWebViewEvent(WebView webView, String url) {
      WritableMap event = Arguments.createMap();
      event.putDouble("target", webView.getId());
      // Don't use webView.getUrl() here, the URL isn't updated to the new value yet in callbacks
      // like onPageFinished
      event.putString("url", url);
      event.putBoolean("loading", !mLastLoadFailed && webView.getProgress() != 100);
      event.putString("title", webView.getTitle());
      event.putBoolean("canGoBack", webView.canGoBack());
      event.putBoolean("canGoForward", webView.canGoForward());
      return event;
    }

    public void setUrlPrefixesForDefaultIntent(ReadableArray specialUrls) {
      mUrlPrefixesForDefaultIntent = specialUrls;
    }

    public void setProgressChangedFilter(RNX5WebView.ProgressChangedFilter filter) {
      progressChangedFilter = filter;
    }
  }

  protected static class RNCWebChromeClient extends WebChromeClient implements LifecycleEventListener {
    protected static final FrameLayout.LayoutParams FULLSCREEN_LAYOUT_PARAMS = new FrameLayout.LayoutParams(
      LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT, Gravity.CENTER);

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    protected static final int FULLSCREEN_SYSTEM_UI_VISIBILITY = View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
      View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
      View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
      View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
      View.SYSTEM_UI_FLAG_FULLSCREEN |
      View.SYSTEM_UI_FLAG_IMMERSIVE |
      View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;

    protected ReactContext mReactContext;
    protected View mWebView;

    protected View mVideoView;
    // protected WebChromeClient.CustomViewCallback mCustomViewCallback;调整
    protected IX5WebChromeClient.CustomViewCallback mCustomViewCallback;

    protected RNX5WebView.ProgressChangedFilter progressChangedFilter = null;

    public RNCWebChromeClient(ReactContext reactContext, WebView webView) {
      this.mReactContext = reactContext;
      this.mWebView = webView;
    }

    @Override
    public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg) {

      final WebView newWebView = new WebView(view.getContext());
      final WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
      transport.setWebView(newWebView);
      resultMsg.sendToTarget();

      return true;
    }

    @Override
    public boolean onConsoleMessage(ConsoleMessage message) {
      if (ReactBuildConfig.DEBUG) {
        return super.onConsoleMessage(message);
      }
      // Ignore console logs in non debug builds.
      return true;
    }

    // Fix WebRTC permission request error.
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    // @Override
    public void onPermissionRequest(final PermissionRequest request) {
      String[] requestedResources = request.getResources();
      ArrayList<String> permissions = new ArrayList<>();
      ArrayList<String> grantedPermissions = new ArrayList<String>();
      for (int i = 0; i < requestedResources.length; i++) {
        if (requestedResources[i].equals(PermissionRequest.RESOURCE_AUDIO_CAPTURE)) {
          permissions.add(Manifest.permission.RECORD_AUDIO);
        } else if (requestedResources[i].equals(PermissionRequest.RESOURCE_VIDEO_CAPTURE)) {
          permissions.add(Manifest.permission.CAMERA);
        }
        // TODO: RESOURCE_MIDI_SYSEX, RESOURCE_PROTECTED_MEDIA_ID.
      }

      for (int i = 0; i < permissions.size(); i++) {
        if (ContextCompat.checkSelfPermission(mReactContext, permissions.get(i)) != PackageManager.PERMISSION_GRANTED) {
          continue;
        }
        if (permissions.get(i).equals(Manifest.permission.RECORD_AUDIO)) {
          grantedPermissions.add(PermissionRequest.RESOURCE_AUDIO_CAPTURE);
        } else if (permissions.get(i).equals(Manifest.permission.CAMERA)) {
          grantedPermissions.add(PermissionRequest.RESOURCE_VIDEO_CAPTURE);
        }
      }

      if (grantedPermissions.isEmpty()) {
        request.deny();
      } else {
        String[] grantedPermissionsArray = new String[grantedPermissions.size()];
        grantedPermissionsArray = grantedPermissions.toArray(grantedPermissionsArray);
        request.grant(grantedPermissionsArray);
      }
    }

    @Override
    public void onProgressChanged(WebView webView, int newProgress) {
      super.onProgressChanged(webView, newProgress);
      final String url = webView.getUrl();
      if (progressChangedFilter.isWaitingForCommandLoadUrl()) {
        return;
      }
      WritableMap event = Arguments.createMap();
      event.putDouble("target", webView.getId());
      event.putString("title", webView.getTitle());
      event.putString("url", url);
      event.putBoolean("canGoBack", webView.canGoBack());
      event.putBoolean("canGoForward", webView.canGoForward());
      event.putDouble("progress", (float) newProgress / 100);
      dispatchEvent(
        webView,
        new TopLoadingProgressEvent(
          webView.getId(),
          event));
    }

    @Override
    public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissionsCallback callback) {
      callback.invoke(origin, true, false);
    }

    protected void openFileChooser(ValueCallback<Uri> filePathCallback, String acceptType) {
      getModule(mReactContext).startPhotoPickerIntent(filePathCallback, acceptType);
    }

    protected void openFileChooser(ValueCallback<Uri> filePathCallback) {
      getModule(mReactContext).startPhotoPickerIntent(filePathCallback, "");
    }

    public void openFileChooser(ValueCallback<Uri> filePathCallback, String acceptType, String capture) {
      getModule(mReactContext).startPhotoPickerIntent(filePathCallback, acceptType);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
      String[] acceptTypes = fileChooserParams.getAcceptTypes();
      boolean allowMultiple = fileChooserParams.getMode() == WebChromeClient.FileChooserParams.MODE_OPEN_MULTIPLE;
      return getModule(mReactContext).startPhotoPickerIntent(filePathCallback, acceptTypes, allowMultiple);
    }

    @Override
    public void onHostResume() {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && mVideoView != null && mVideoView.getSystemUiVisibility() != FULLSCREEN_SYSTEM_UI_VISIBILITY) {
        mVideoView.setSystemUiVisibility(FULLSCREEN_SYSTEM_UI_VISIBILITY);
      }
    }

    @Override
    public void onHostPause() { }

    @Override
    public void onHostDestroy() { }

    protected ViewGroup getRootView() {
      return (ViewGroup) mReactContext.getCurrentActivity().findViewById(android.R.id.content);
    }

    public void setProgressChangedFilter(RNX5WebView.ProgressChangedFilter filter) {
      progressChangedFilter = filter;
    }
  }

  /**
   * Subclass of {@link WebView} that implements {@link LifecycleEventListener} interface in order
   * to call {@link WebView#destroy} on activity destroy event and also to clear the client
   */
  protected static class RNX5WebView extends WebView implements LifecycleEventListener {
    protected @Nullable
    String injectedJS;
    protected @Nullable
    String injectedJSBeforeContentLoaded;

    /**
     * android.webkit.WebChromeClient fundamentally does not support JS injection into frames other
     * than the main frame, so these two properties are mostly here just for parity with iOS & macOS.
     */
    protected boolean injectedJavaScriptForMainFrameOnly = true;
    protected boolean injectedJavaScriptBeforeContentLoadedForMainFrameOnly = true;

    protected boolean messagingEnabled = false;
    protected @Nullable
    String messagingModuleName;
    protected @Nullable
    RNX5WebViewClient mRNX5WebViewClient;
    protected @Nullable
    CatalystInstance mCatalystInstance;
    protected boolean sendContentSizeChangeEvents = false;
    private OnScrollDispatchHelper mOnScrollDispatchHelper;
    protected boolean hasScrollEvent = false;
    protected ProgressChangedFilter progressChangedFilter;

    /**
     * WebView must be created with an context of the current activity
     * <p>
     * Activity Context is required for creation of dialogs internally by WebView
     * Reactive Native needed for access to ReactNative internal system functionality
     */
    public RNX5WebView(ThemedReactContext reactContext) {
      super(reactContext);
//      this.createCatalystInstance();
      progressChangedFilter = new ProgressChangedFilter();
    }

    public void setIgnoreErrFailedForThisURL(String url) {
      mRNX5WebViewClient.setIgnoreErrFailedForThisURL(url);
    }

    public void setSendContentSizeChangeEvents(boolean sendContentSizeChangeEvents) {
      this.sendContentSizeChangeEvents = sendContentSizeChangeEvents;
    }

    public void setHasScrollEvent(boolean hasScrollEvent) {
      this.hasScrollEvent = hasScrollEvent;
    }

    @Override
    public void onHostResume() {
      // do nothing
    }

    @Override
    public void onHostPause() {
      // do nothing
    }

    @Override
    public void onHostDestroy() {
      cleanupCallbacksAndDestroy();
    }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
      super.onSizeChanged(w, h, ow, oh);

      if (sendContentSizeChangeEvents) {
        dispatchEvent(
          this,
          new ContentSizeChangeEvent(
            this.getId(),
            w,
            h
          )
        );
      }
    }

    @Override
    public void setWebViewClient(WebViewClient client) {
      super.setWebViewClient(client);
      if (client instanceof RNX5WebViewClient) {
        mRNX5WebViewClient = (RNX5WebViewClient) client;
        mRNX5WebViewClient.setProgressChangedFilter(progressChangedFilter);
      }
    }

    WebChromeClient mWebChromeClient;
    @Override
    public void setWebChromeClient(WebChromeClient client) {
      this.mWebChromeClient = client;
      super.setWebChromeClient(client);
      if (client instanceof RNCWebChromeClient) {
        ((RNCWebChromeClient) client).setProgressChangedFilter(progressChangedFilter);
      }
    }

    public @Nullable
    RNX5WebViewClient getRNX5WebViewClient() {
      return mRNX5WebViewClient;
    }

    public void setInjectedJavaScript(@Nullable String js) {
      injectedJS = js;
    }

    public void setInjectedJavaScriptBeforeContentLoaded(@Nullable String js) {
      injectedJSBeforeContentLoaded = js;
    }

    public void setInjectedJavaScriptForMainFrameOnly(boolean enabled) {
      injectedJavaScriptForMainFrameOnly = enabled;
    }

    public void setInjectedJavaScriptBeforeContentLoadedForMainFrameOnly(boolean enabled) {
      injectedJavaScriptBeforeContentLoadedForMainFrameOnly = enabled;
    }

    protected RNX5WebViewBridge createRNX5WebViewBridge(RNX5WebView webView) {
      return new RNX5WebViewBridge(webView);
    }

    protected void createCatalystInstance() {
      ReactContext reactContext = (ReactContext) this.getContext();

      if (reactContext != null) {
        mCatalystInstance = reactContext.getCatalystInstance();
      }
    }

    @SuppressLint("AddJavascriptInterface")
    public void setMessagingEnabled(boolean enabled) {
      if (messagingEnabled == enabled) {
        return;
      }

      messagingEnabled = enabled;

      if (enabled) {
        addJavascriptInterface(createRNX5WebViewBridge(this), JAVASCRIPT_INTERFACE);
      } else {
        removeJavascriptInterface(JAVASCRIPT_INTERFACE);
      }
    }

    public void setMessagingModuleName(String moduleName) {
      messagingModuleName = moduleName;
    }

    protected void evaluateJavascriptWithFallback(String script) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
        evaluateJavascript(script, null);
        return;
      }

      try {
        loadUrl("javascript:" + URLEncoder.encode(script, "UTF-8"));
      } catch (UnsupportedEncodingException e) {
        // UTF-8 should always be supported
        throw new RuntimeException(e);
      }
    }

    public void callInjectedJavaScript() {
      if (getSettings().getJavaScriptEnabled() &&
        injectedJS != null &&
        !TextUtils.isEmpty(injectedJS)) {
        evaluateJavascriptWithFallback("(function() {\n" + injectedJS + ";\n})();");
      }
    }

    public void callInjectedJavaScriptBeforeContentLoaded() {
      if (getSettings().getJavaScriptEnabled() &&
      injectedJSBeforeContentLoaded != null &&
      !TextUtils.isEmpty(injectedJSBeforeContentLoaded)) {
        evaluateJavascriptWithFallback("(function() {\n" + injectedJSBeforeContentLoaded + ";\n})();");
      }
    }

    public void onMessage(String message) {
      ReactContext reactContext = (ReactContext) this.getContext();
      RNX5WebView mContext = this;

      if (mRNX5WebViewClient != null) {
        WebView webView = this;
        webView.post(new Runnable() {
          @Override
          public void run() {
            if (mRNX5WebViewClient == null) {
              return;
            }
            WritableMap data = mRNX5WebViewClient.createWebViewEvent(webView, webView.getUrl());
            data.putString("data", message);

            if (mCatalystInstance != null) {
              mContext.sendDirectMessage("onMessage", data);
            } else {
              dispatchEvent(webView, new TopMessageEvent(webView.getId(), data));
            }
          }
        });
      } else {
        WritableMap eventData = Arguments.createMap();
        eventData.putString("data", message);

        if (mCatalystInstance != null) {
          this.sendDirectMessage("onMessage", eventData);
        } else {
          dispatchEvent(this, new TopMessageEvent(this.getId(), eventData));
        }
      }
    }

    protected void sendDirectMessage(final String method, WritableMap data) {
      WritableNativeMap event = new WritableNativeMap();
      event.putMap("nativeEvent", data);

      WritableNativeArray params = new WritableNativeArray();
      params.pushMap(event);

      mCatalystInstance.callFunction(messagingModuleName, method, params);
    }

    protected void onScrollChanged(int x, int y, int oldX, int oldY) {
      super.onScrollChanged(x, y, oldX, oldY);

      if (!hasScrollEvent) {
        return;
      }

      if (mOnScrollDispatchHelper == null) {
        mOnScrollDispatchHelper = new OnScrollDispatchHelper();
      }

      if (mOnScrollDispatchHelper.onScrollChanged(x, y)) {
        ScrollEvent event = ScrollEvent.obtain(
                this.getId(),
                ScrollEventType.SCROLL,
                x,
                y,
                mOnScrollDispatchHelper.getXFlingVelocity(),
                mOnScrollDispatchHelper.getYFlingVelocity(),
                this.computeHorizontalScrollRange(),
                this.computeVerticalScrollRange(),
                this.getWidth(),
                this.getHeight());

        dispatchEvent(this, event);
      }
    }

    protected void cleanupCallbacksAndDestroy() {
      setWebViewClient(null);
      destroy();
    }

    @Override
    public void destroy() {
      if (mWebChromeClient != null) {
        mWebChromeClient.onHideCustomView();
      }
      super.destroy();
    }

    protected class RNX5WebViewBridge {
      RNX5WebView mContext;

      RNX5WebViewBridge(RNX5WebView c) {
        mContext = c;
      }

      /**
       * This method is called whenever JavaScript running within the web view calls:
       * - window[JAVASCRIPT_INTERFACE].postMessage
       */
      @JavascriptInterface
      public void postMessage(String message) {
        mContext.onMessage(message);
      }
    }

    protected static class ProgressChangedFilter {
      private boolean waitingForCommandLoadUrl = false;

      public void setWaitingForCommandLoadUrl(boolean isWaiting) {
        waitingForCommandLoadUrl = isWaiting;
      }

      public boolean isWaitingForCommandLoadUrl() {
        return waitingForCommandLoadUrl;
      }
    }
  }
}
