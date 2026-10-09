package org.benwiley.bixi4flip;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.RectF;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.Projection;
import org.osmdroid.views.overlay.Overlay;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {

    // Change this one constant to retheme the app.
    static final int BIXI_RED = Color.parseColor("#D52B1E");
    static final int GREY = Color.parseColor("#666666");

    private static final String GBFS_URL = "https://gbfs.velobixi.com/gbfs/2-2/gbfs.json";
    private static final GeoPoint MONTREAL = new GeoPoint(45.5017, -73.5673);
    private static final long REFRESH_MS = 60_000;

    private MapView map;
    private TextView topBar;
    private TextView bottomBar;
    private StationOverlay overlay;
    private LocationManager locationManager;
    private boolean userMoved = false;
    private boolean haveFix = false;
    private String updatedText = "Loading...";

    private final Handler ui = new Handler(Looper.getMainLooper());
    private final ExecutorService io = Executors.newSingleThreadExecutor();

    private final Runnable autoRefresh = new Runnable() {
        @Override public void run() {
            refreshStations();
            ui.postDelayed(this, REFRESH_MS);
        }
    };

    private final LocationListener locationListener = new LocationListener() {
        @Override public void onLocationChanged(Location loc) { onFix(loc); stopLocation(); }
        @Override public void onStatusChanged(String p, int s, Bundle e) { }
        @Override public void onProviderEnabled(String p) { }
        @Override public void onProviderDisabled(String p) { }
    };

    // ---------------------------------------------------------------- lifecycle

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);

        // Keep the tile cache in app-private storage: no storage permission needed.
        Configuration.getInstance().setUserAgentValue("Bixi4Flip/1.0 (therealbenwiley@gmail.com)");
        File base = new File(getCacheDir(), "osmdroid");
        Configuration.getInstance().setOsmdroidBasePath(base);
        Configuration.getInstance().setOsmdroidTileCache(new File(base, "tiles"));

        getWindow().setStatusBarColor(BIXI_RED);
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        map = new MapView(this);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);
        map.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.NEVER);
        map.setMinZoomLevel(10.0);
        map.setMaxZoomLevel(19.0);
        map.getController().setZoom(14.0);
        map.getController().setCenter(MONTREAL);
        overlay = new StationOverlay();
        map.getOverlays().add(overlay);

        topBar = new TextView(this);
        topBar.setBackgroundColor(GREY);
        topBar.setTextColor(Color.WHITE);
        topBar.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        topBar.setPadding(4, 4, 4, 4);
        topBar.setSingleLine(true);
        topBar.setEllipsize(TextUtils.TruncateAt.END);
        updateTopBar();

        bottomBar = new TextView(this);
        bottomBar.setBackgroundColor(BIXI_RED);
        bottomBar.setTextColor(Color.WHITE);
        bottomBar.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        bottomBar.setPadding(4, 4, 4, 4);
        bottomBar.setSingleLine(true);
        bottomBar.setEllipsize(TextUtils.TruncateAt.END);
        updateBottomBar();

        FrameLayout root = new FrameLayout(this);
        root.addView(map, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.BOTTOM;
        root.addView(bottomBar, lp);
        FrameLayout.LayoutParams lpTop = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lpTop.gravity = Gravity.TOP;
        root.addView(topBar, lpTop);
        setContentView(root);
    }

    @Override protected void onResume() {
        super.onResume();
        map.onResume();
        startLocation();
        ui.removeCallbacks(autoRefresh);
        ui.post(autoRefresh);
    }

    @Override protected void onPause() {
        super.onPause();
        ui.removeCallbacks(autoRefresh);
        stopLocation();
        map.onPause();
    }

    // ---------------------------------------------------------------- D-pad keys

    @Override public boolean dispatchKeyEvent(KeyEvent e) {
        if (e.getAction() != KeyEvent.ACTION_DOWN) {
            // swallow the matching key-up events for keys we handle
            return isHandled(e.getKeyCode()) || super.dispatchKeyEvent(e);
        }
        int step = Math.min(map.getWidth(), map.getHeight()) / 4;
        switch (e.getKeyCode()) {
            case KeyEvent.KEYCODE_DPAD_LEFT:  pan(-step, 0); return true;
            case KeyEvent.KEYCODE_DPAD_RIGHT: pan(step, 0);  return true;
            case KeyEvent.KEYCODE_DPAD_UP:    pan(0, -step); return true;
            case KeyEvent.KEYCODE_DPAD_DOWN:  pan(0, step);  return true;
            case KeyEvent.KEYCODE_3:
            case KeyEvent.KEYCODE_POUND:
            case KeyEvent.KEYCODE_VOLUME_UP:
                // Zoom instantly instead of with transition
                setZoomNow(map.getZoomLevelDouble() + 1); return true;
                // map.getController().zoomIn(); return true;
            case KeyEvent.KEYCODE_1:
            case KeyEvent.KEYCODE_STAR:
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                // Zoom instantly instead of with transition
                setZoomNow(map.getZoomLevelDouble() - 1); return true;
                // map.getController().zoomOut(); return true;
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_5:
                recenter(); return true;
            case KeyEvent.KEYCODE_0:
                refreshStations(); return true;
        }
        return super.dispatchKeyEvent(e);
    }

    private void setZoomNow(double z) {
        map.getController().setZoom(Math.round(z));
    }

    private boolean isHandled(int k) {
        switch (k) {
            case KeyEvent.KEYCODE_DPAD_LEFT: case KeyEvent.KEYCODE_DPAD_RIGHT:
            case KeyEvent.KEYCODE_DPAD_UP: case KeyEvent.KEYCODE_DPAD_DOWN:
            case KeyEvent.KEYCODE_DPAD_CENTER: case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_1: case KeyEvent.KEYCODE_3: case KeyEvent.KEYCODE_5:
            case KeyEvent.KEYCODE_0: case KeyEvent.KEYCODE_STAR: case KeyEvent.KEYCODE_POUND:
            case KeyEvent.KEYCODE_VOLUME_UP: case KeyEvent.KEYCODE_VOLUME_DOWN:
                return true;
        }
        return false;
    }

    private void pan(int dx, int dy) {
        userMoved = true;
        Projection p = map.getProjection();
        Point c = p.toPixels(map.getMapCenter(), null);
        map.getController().setCenter(p.fromPixels(c.x + dx, c.y + dy));
    }

    private void recenter() {
        if (overlay.me != null) {
            userMoved = false;
            map.getController().setCenter(overlay.me);
            map.getController().setZoom(16.0);
        } else {
            startLocation();
        }
    }

    // ---------------------------------------------------------------- location

    @SuppressLint("MissingPermission")
    private void startLocation() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            return;
        }
        Location best = null;
        for (String prov : new String[]{LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER}) {
            try {
                if (!locationManager.isProviderEnabled(prov)) continue;
                Location l = locationManager.getLastKnownLocation(prov);
                if (l != null && (best == null || l.getTime() > best.getTime())) best = l;
                locationManager.requestLocationUpdates(prov, 0, 0, locationListener, Looper.getMainLooper());
            } catch (Exception ignored) { /* provider disabled or unavailable: just skip it */ }
        }
        if (best != null) onFix(best);
    }

    private void stopLocation() {
        try { locationManager.removeUpdates(locationListener); } catch (Exception ignored) { }
    }

    private void onFix(Location loc) {
        GeoPoint p = new GeoPoint(loc.getLatitude(), loc.getLongitude());
        overlay.me = p;
        if (!haveFix && !userMoved) {
            map.getController().setCenter(p);
            map.getController().setZoom(16.0);
        }
        haveFix = true;
        map.invalidate();
    }

    @Override public void onRequestPermissionsResult(int code, String[] perms, int[] res) {
        super.onRequestPermissionsResult(code, perms, res);
        if (res.length > 0 && res[0] == PackageManager.PERMISSION_GRANTED) startLocation();
    }

    // ---------------------------------------------------------------- Bixi data

    static class Station {
        String name = "";
        double lat, lon;
        int regular, ebikes, docks;
        boolean hasStatus;
    }

    private void refreshStations() {
        io.execute(() -> {
            try {
                JSONObject data = getJson(GBFS_URL).getJSONObject("data");
                JSONObject lang = data.has("en") ? data.getJSONObject("en")
                        : data.getJSONObject(data.keys().next());
                JSONArray feeds = lang.getJSONArray("feeds");
                String infoUrl = null, statusUrl = null;
                for (int i = 0; i < feeds.length(); i++) {
                    JSONObject f = feeds.getJSONObject(i);
                    if ("station_information".equals(f.optString("name"))) infoUrl = f.getString("url");
                    if ("station_status".equals(f.optString("name"))) statusUrl = f.getString("url");
                }
                if (infoUrl == null || statusUrl == null) throw new Exception("feeds missing");

                Map<String, Station> byId = new HashMap<>();
                JSONArray info = getJson(infoUrl).getJSONObject("data").getJSONArray("stations");
                for (int i = 0; i < info.length(); i++) {
                    JSONObject o = info.getJSONObject(i);
                    Station s = new Station();
                    s.name = o.optString("name");
                    s.lat = o.optDouble("lat");
                    s.lon = o.optDouble("lon");
                    byId.put(o.getString("station_id"), s);
                }
                JSONArray st = getJson(statusUrl).getJSONObject("data").getJSONArray("stations");
                for (int i = 0; i < st.length(); i++) {
                    JSONObject o = st.getJSONObject(i);
                    Station s = byId.get(o.optString("station_id"));
                    if (s == null) continue;
                    int total = o.optInt("num_bikes_available");
                    s.ebikes = o.optInt("num_ebikes_available");
                    // Bixi's total includes e-bikes, so regular = total - electric.
                    s.regular = Math.max(0, total - s.ebikes);
                    s.docks = o.optInt("num_docks_available");
                    s.hasStatus = true;
                }
                List<Station> list = new ArrayList<>(byId.values());
                String when = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new java.util.Date());
                ui.post(() -> {
                    overlay.stations = list;
                    updatedText = "À jour " + when;
                    updateBottomBar();
                    map.invalidate();
                });
            } catch (Exception ex) {
                ui.post(() -> { updatedText = "Hors ligne"; updateBottomBar(); });
            }
        });
    }

    private static JSONObject getJson(String url) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(10_000);
        c.setReadTimeout(15_000);
        try (BufferedReader r = new BufferedReader(new InputStreamReader(c.getInputStream(), "UTF-8"))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) sb.append(line);
            return new JSONObject(sb.toString());
        } finally {
            c.disconnect();
        }
    }

    private void updateTopBar() {
        topBar.setText("Zoom+ = 3,#,Vol+  Zoom- = 1,*,Vol-  Pos = 5,OK");
    }

    private void updateBottomBar() {
        bottomBar.setText("Reg/Élec/Vides | " + updatedText + " | Mise à jour = 0");
    }

    // ---------------------------------------------------------------- drawing

    class StationOverlay extends Overlay {
        List<Station> stations = new ArrayList<>();
        GeoPoint me;

        private final float dp = getResources().getDisplayMetrics().density;
        private final Paint dot = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint dotRing = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint box = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint boxRing = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint meDot = new Paint(Paint.ANTI_ALIAS_FLAG);

        StationOverlay() {
            dot.setColor(BIXI_RED);
            dotRing.setColor(Color.WHITE);
            box.setColor(Color.WHITE);
            boxRing.setColor(BIXI_RED);
            boxRing.setStyle(Paint.Style.STROKE);
            boxRing.setStrokeWidth(1.5f * dp);
            text.setColor(Color.BLACK);
            text.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 11,
                    getResources().getDisplayMetrics()));
            meDot.setColor(Color.parseColor("#1A73E8"));
        }

        @Override public void draw(Canvas c, MapView mv, boolean shadow) {
            if (shadow) return;
            Projection proj = mv.getProjection();
            double zoom = mv.getZoomLevelDouble();
            int w = mv.getWidth(), h = mv.getHeight();
            float r = 5 * dp, pad = 4 * dp;
            Point pt = new Point();
            List<Station> list = stations;

            // pass 1: dots
            List<float[]> visible = new ArrayList<>();
            List<Station> visibleStations = new ArrayList<>();
            for (Station s : list) {
                proj.toPixels(new GeoPoint(s.lat, s.lon), pt);
                if (pt.x < -150 || pt.x > w + 150 || pt.y < -80 || pt.y > h + 80) continue;
                c.drawCircle(pt.x, pt.y, r + dp, dotRing);
                c.drawCircle(pt.x, pt.y, r, dot);
                visible.add(new float[]{pt.x, pt.y});
                visibleStations.add(s);
            }

            // pass 2: labels (zoomed in enough)
            if (zoom >= 14.5) {
                Paint.FontMetrics fm = text.getFontMetrics();
                float lineH = fm.descent - fm.ascent;
                boolean full = zoom >= 16;
                for (int i = 0; i < visible.size(); i++) {
                    Station s = visibleStations.get(i);
                    float x = visible.get(i)[0], y = visible.get(i)[1];
                    String counts = s.hasStatus
                            ? (full ? "Reg " + s.regular + "  E " + s.ebikes + "  Vide " + s.docks
                               : s.regular + "/" + s.ebikes + "/" + s.docks)
                            : "n/a";
                    String name = full ? TextUtils.ellipsize(s.name, new android.text.TextPaint(text),
                            w * 0.8f, TextUtils.TruncateAt.END).toString() : null;
                    float tw = text.measureText(counts);
                    if (name != null) tw = Math.max(tw, text.measureText(name));
                    float bw = tw + 2 * pad;
                    float bh = (name != null ? 2 : 1) * lineH + 2 * pad;
                    RectF rect = new RectF(x - bw / 2, y - r - 3 * dp - bh, x + bw / 2, y - r - 3 * dp);
                    c.drawRoundRect(rect, 5 * dp, 5 * dp, box);
                    c.drawRoundRect(rect, 5 * dp, 5 * dp, boxRing);
                    float base = rect.top + pad - fm.ascent;
                    if (name != null) {
                        c.drawText(name, rect.left + pad, base, text);
                        base += lineH;
                    }
                    c.drawText(counts, rect.left + pad, base, text);
                }
            }

            // my location
            if (me != null) {
                proj.toPixels(me, pt);
                c.drawCircle(pt.x, pt.y, 8 * dp, dotRing);
                c.drawCircle(pt.x, pt.y, 6 * dp, meDot);
            }
        }
    }
}
