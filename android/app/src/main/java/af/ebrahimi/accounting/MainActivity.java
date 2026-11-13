package af.ebrahimi.accounting;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintManager;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;
import android.webkit.GeolocationPermissions;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

public class MainActivity extends Activity {
    static final String HOME = "https://alijojo2122joon-jpg.github.io/Ibrahimi-Accounting-/";
    static final String HOST = "alijojo2122joon-jpg.github.io";
    static final int REQ_FILE = 11, REQ_LOC = 12, REQ_CAM = 13;
    WebView web;
    ValueCallback<Uri[]> fileCb;
    Uri cameraUri;
    GeolocationPermissions.Callback geoCb; String geoOrigin;

    @SuppressLint("SetJavaScriptEnabled")
    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        web = new WebView(this);
        web.setBackgroundColor(Color.parseColor("#E6EAEF"));
        setContentView(web);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setGeolocationEnabled(true);
        s.setJavaScriptCanOpenWindowsAutomatically(true);
        s.setSupportMultipleWindows(false);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setAllowFileAccess(false);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);
        s.setUserAgentString(s.getUserAgentString() + " EbrahimiApp/1");
        web.addJavascriptInterface(new Bridge(), "AndroidBridge");
        web.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) { return handle(r.getUrl()); }
            @Override public void onReceivedError(WebView v, android.webkit.WebResourceRequest r, android.webkit.WebResourceError e) {
                if (r.isForMainFrame()) v.loadData("<html dir='rtl'><body style='font-family:sans-serif;background:#e6eaef;padding:40px;text-align:center'><h2>انترنت نیست</h2><p>بار اول برای باز شدن برنامه انترنت لازم است. بعد از آن بدون انترنت هم کار می‌کند.</p><p><a href='" + HOME + "'>دوباره امتحان کنید</a></p></body></html>", "text/html; charset=utf-8", "UTF-8");
            }
        });
        web.setWebChromeClient(new WebChromeClient() {
            @Override public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback cb) {
                if (hasPerm(Manifest.permission.ACCESS_FINE_LOCATION)) { cb.invoke(origin, true, false); return; }
                geoCb = cb; geoOrigin = origin;
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, REQ_LOC);
            }
            @Override public boolean onShowFileChooser(WebView v, ValueCallback<Uri[]> cb, FileChooserParams p) {
                if (fileCb != null) fileCb.onReceiveValue(null);
                fileCb = cb;
                if (!hasPerm(Manifest.permission.CAMERA)) requestPermissions(new String[]{Manifest.permission.CAMERA}, REQ_CAM);
                openChooser();
                return true;
            }
        });
        Uri data = getIntent() != null ? getIntent().getData() : null;
        if (b != null) web.restoreState(b); else web.loadUrl(data != null && HOST.equals(data.getHost()) ? data.toString() : HOME);
    }

    void openChooser() {
        Intent pick = new Intent(Intent.ACTION_GET_CONTENT); pick.addCategory(Intent.CATEGORY_OPENABLE); pick.setType("image/*");
        Intent chooser = Intent.createChooser(pick, "عکس");
        if (hasPerm(Manifest.permission.CAMERA)) {
            try {
                File f = new File(getCacheDir(), "cam_" + System.currentTimeMillis() + ".jpg");
                cameraUri = FileProvider.getUriForFile(this, "af.ebrahimi.accounting.files", f);
                Intent cam = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                cam.putExtra(MediaStore.EXTRA_OUTPUT, cameraUri);
                cam.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{cam});
            } catch (Exception e) { cameraUri = null; }
        }
        try { startActivityForResult(chooser, REQ_FILE); } catch (ActivityNotFoundException e) { if (fileCb != null) fileCb.onReceiveValue(null); fileCb = null; }
    }

    boolean hasPerm(String p) { return checkSelfPermission(p) == PackageManager.PERMISSION_GRANTED; }

    boolean handle(Uri u) {
        String sch = u.getScheme() == null ? "" : u.getScheme();
        if (("https".equals(sch) || "http".equals(sch)) && HOST.equals(u.getHost()) && u.getPath() != null && u.getPath().startsWith("/Ibrahimi-Accounting-")) return false;
        if ("https".equals(sch) && u.getHost() != null && u.getHost().endsWith("supabase.co")) return false;
        try {
            Intent i = "tel".equals(sch) ? new Intent(Intent.ACTION_DIAL, u) : new Intent(Intent.ACTION_VIEW, u);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        } catch (Exception e) { Toast.makeText(this, "برنامه‌ای برای باز کردن این لینک پیدا نشد", Toast.LENGTH_SHORT).show(); }
        return true;
    }

    @Override protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req != REQ_FILE || fileCb == null) return;
        Uri[] out = null;
        if (res == RESULT_OK) {
            if (data != null && data.getData() != null) out = new Uri[]{data.getData()};
            else if (cameraUri != null) out = new Uri[]{cameraUri};
        }
        fileCb.onReceiveValue(out); fileCb = null;
    }

    @Override public void onRequestPermissionsResult(int req, String[] perms, int[] res) {
        if (req == REQ_LOC && geoCb != null) {
            boolean ok = res.length > 0 && res[0] == PackageManager.PERMISSION_GRANTED;
            geoCb.invoke(geoOrigin, ok, false); geoCb = null;
        }
    }

    @Override protected void onNewIntent(Intent i) {
        super.onNewIntent(i);
        Uri d = i.getData(); if (d != null && HOST.equals(d.getHost())) web.loadUrl(d.toString());
    }

    @Override protected void onSaveInstanceState(Bundle o) { super.onSaveInstanceState(o); web.saveState(o); }

    @Override public void onBackPressed() {
        web.evaluateJavascript("(function(){try{var m=document.getElementById('modal');if(m&&!m.classList.contains('hide')){closeModal();return 'modal';}var w=document.getElementById('mapwrap');if(w&&w.classList.contains('mapfs')){mapFull();return 'map';}if(typeof R!=='undefined'&&R.v!=='dash'){go('dash');return 'nav';}}catch(e){}return 'exit';})()", v -> {
            if (v != null && v.contains("exit")) MainActivity.super.onBackPressed();
        });
    }

    File writeTemp(String name, byte[] bytes) throws Exception {
        File f = new File(getCacheDir(), name.replaceAll("[\\\\/:*?\"<>|]", "_"));
        try (FileOutputStream o = new FileOutputStream(f)) { o.write(bytes); }
        return f;
    }

    class Bridge {
        @JavascriptInterface public String version() { return "1"; }

        @JavascriptInterface public void saveFile(String name, String b64, String mime) {
            try {
                byte[] bytes = Base64.decode(b64, Base64.DEFAULT);
                if (Build.VERSION.SDK_INT >= 29) {
                    ContentValues cv = new ContentValues();
                    cv.put(MediaStore.Downloads.DISPLAY_NAME, name);
                    cv.put(MediaStore.Downloads.MIME_TYPE, mime);
                    cv.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Ebrahimi");
                    Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv);
                    try (OutputStream o = getContentResolver().openOutputStream(uri)) { o.write(bytes); }
                    toast("ذخیره شد: Download/Ebrahimi/" + name);
                } else {
                    File dir = new File(getExternalFilesDir(null), "Ebrahimi"); dir.mkdirs();
                    File f = new File(dir, name);
                    try (FileOutputStream o = new FileOutputStream(f)) { o.write(bytes); }
                    share(f, mime, name);
                }
            } catch (Exception e) { toast("ذخیره نشد: " + e.getMessage()); }
        }

        @JavascriptInterface public void shareFile(String name, String b64, String mime) {
            try { share(writeTemp(name, Base64.decode(b64, Base64.DEFAULT)), mime, name); }
            catch (Exception e) { toast("فرستاده نشد: " + e.getMessage()); }
        }

        @JavascriptInterface public void print(String title) {
            runOnUiThread(() -> {
                try {
                    PrintManager pm = (PrintManager) getSystemService(Context.PRINT_SERVICE);
                    PrintDocumentAdapter ad = web.createPrintDocumentAdapter(title == null ? "Ebrahimi" : title);
                    pm.print(title == null ? "Ebrahimi" : title, ad, new PrintAttributes.Builder().setMediaSize(PrintAttributes.MediaSize.ISO_A4).build());
                } catch (Exception e) { toast("چاپ ممکن نشد"); }
            });
        }
    }

    void share(File f, String mime, String title) {
        Uri u = FileProvider.getUriForFile(this, "af.ebrahimi.accounting.files", f);
        Intent i = new Intent(Intent.ACTION_SEND); i.setType(mime); i.putExtra(Intent.EXTRA_STREAM, u); i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        runOnUiThread(() -> startActivity(Intent.createChooser(i, title)));
    }

    void toast(String m) { runOnUiThread(() -> Toast.makeText(this, m, Toast.LENGTH_LONG).show()); }
}
