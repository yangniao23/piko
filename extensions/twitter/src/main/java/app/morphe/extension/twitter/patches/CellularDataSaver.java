package app.morphe.extension.twitter.patches;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import java.util.Objects;
import app.morphe.extension.twitter.settings.Settings;

/** Opt-in controller. Native bridge bodies are supplied by the bytecode patch. */
public final class CellularDataSaver {
    private static final String TAG = "PikoCellularSaver";
    private static CellularDataSaver instance;
    private final Context context;
    private final ConnectivityManager connectivity;
    private final SharedPreferences preferences;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Object manager;
    private boolean registered;
    private Network lastNetwork;
    private Boolean lastPolicy;
    private final SharedPreferences.OnSharedPreferenceChangeListener listener = (prefs, key) -> {
        if (key == null || Settings.CELLULAR_DATA_SAVER.key.equals(key)) handler.post(this::refresh);
    };
    private final ConnectivityManager.NetworkCallback callback = new ConnectivityManager.NetworkCallback() {
        @Override public void onCapabilitiesChanged(Network network, NetworkCapabilities caps) {
            update(network, caps);
        }
        @Override public void onLost(Network network) {
            // Do not overwrite a policy already received for the replacement network.
            if (Objects.equals(network, lastNetwork)) update(null, null);
        }
    };

    private CellularDataSaver(Context context) {
        this.context = context;
        connectivity = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        preferences = context.getSharedPreferences(Settings.SHARED_PREF_NAME, Context.MODE_PRIVATE);
    }

    public static void start(Context context) {
        if (instance != null) return;
        instance = new CellularDataSaver(context.getApplicationContext());
        instance.preferences.registerOnSharedPreferenceChangeListener(instance.listener);
        instance.handler.post(instance::refresh);
    }

    public static boolean isEnabled() {
        return instance != null && instance.registered
                && instance.preferences.getBoolean(Settings.CELLULAR_DATA_SAVER.key, false);
    }

    private void refresh() {
        try {
            boolean enabled = preferences.getBoolean(Settings.CELLULAR_DATA_SAVER.key, false);
            if (!enabled) {
                if (registered) connectivity.unregisterNetworkCallback(callback);
                registered = false;
                lastNetwork = null;
                lastPolicy = null;
                return;
            }
            if (connectivity == null || registered) return;
            if (manager == null) manager = createManager(context);
            connectivity.registerDefaultNetworkCallback(callback, handler);
            registered = true;
            Network network = connectivity.getActiveNetwork();
            update(network, network == null ? null : connectivity.getNetworkCapabilities(network));
        } catch (Exception e) {
            Log.e(TAG, "Could not configure automatic data saver", e);
        }
    }

    private void update(Network network, NetworkCapabilities caps) {
        if (!isEnabled()) return;
        boolean cellular = caps != null
                && caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                && !caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
        if (Objects.equals(network, lastNetwork) && Objects.equals(lastPolicy, cellular)) return;
        try {
            if (readEnabled(manager) != cellular) writeEnabled(manager, cellular);
            lastNetwork = network;
            lastPolicy = cellular;
        } catch (Exception e) {
            Log.e(TAG, "Could not update automatic data saver", e);
        }
    }

    private static Object createManager(Context context) {
        throw new IllegalStateException("Native data saver bridge was not patched");
    }
    private static boolean readEnabled(Object manager) {
        throw new IllegalStateException("Native data saver bridge was not patched");
    }
    private static void writeEnabled(Object manager, boolean enabled) {
        throw new IllegalStateException("Native data saver bridge was not patched");
    }
}
