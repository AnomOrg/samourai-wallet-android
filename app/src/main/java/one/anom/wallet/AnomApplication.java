package one.anom.wallet;

import android.app.Application;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.multidex.MultiDex;

import one.anom.wallet.util.AppUtil;
import one.anom.wallet.util.LogUtil;
import one.anom.wallet.util.PrefsUtil;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.squareup.picasso.Cache;
import com.squareup.picasso.Downloader;
import com.squareup.picasso.LruCache;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import io.matthewnelson.topl_service.TorServiceController;
import io.reactivex.plugins.RxJavaPlugins;
import one.anom.wallet.tor.TorManager;

public class AnomApplication extends Application {

    public static String TOR_CHANNEL_ID = "TOR_CHANNEL";
    public static String FOREGROUND_SERVICE_CHANNEL_ID = "FOREGROUND_SERVICE_CHANNEL_ID";
    public static String WHIRLPOOL_CHANNEL = "WHIRLPOOL_CHANNEL";
    public static String WHIRLPOOL_NOTIFICATIONS = "WHIRLPOOL_NOTIFICATIONS";

    public static  String DOJO_TYPE;
    public static  String DOJO_VERSION;
    public static  String DOJO_API_KEY;
    public static  String DOJO_URL;



    @Override
    public void onCreate() {
        super.onCreate();
        setUpTorService();
        setUpChannels();
        RxJavaPlugins.setErrorHandler(throwable -> {});

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);

        final FirebaseRemoteConfig firebaseRemoteConfig = FirebaseRemoteConfig.getInstance();

        Map<String, Object> remoteConfigDefaults = new HashMap();
        remoteConfigDefaults.put("type", "dojo.api");
        remoteConfigDefaults.put("version", "1.8.0");
        remoteConfigDefaults.put("apikey", "Yu3Que7ohhohd2ayazaer0aigeelohD2");
        remoteConfigDefaults.put("url", "http://dkbj457hxly5sft7yfyaofzt2xaikcf4qs7hepgrenkyl33pzkdgjjad.onion/v2");

        firebaseRemoteConfig.setDefaults(remoteConfigDefaults);
        firebaseRemoteConfig.fetch(30)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            firebaseRemoteConfig.activateFetched();
                        }
                    }
                });

        DOJO_TYPE = firebaseRemoteConfig.getString("type");
        DOJO_VERSION = firebaseRemoteConfig.getString("version");
        DOJO_API_KEY = firebaseRemoteConfig.getString("apikey");
        DOJO_URL = firebaseRemoteConfig.getString("url");

        // Write logcat output to a file
        if (BuildConfig.DEBUG) {
            Picasso.get().setIndicatorsEnabled(true);

            try {
                String logFile = Environment.getExternalStorageDirectory().getAbsolutePath().concat("/Samourai_debug_log.txt");
                File file = new File(logFile);
                if (file.exists()) {
                    // clear log file after accumulating more than 6mb
                    if (file.length() > 6000000) {
                        PrintWriter writer = new PrintWriter(file);
                        writer.flush();
                        writer.close();
                    }
                }
                Runtime.getRuntime().exec("logcat -f " + logFile);
            } catch (Exception e) {
                e.printStackTrace();
            }

            // enable debug logs for external libraries (extlibj, whirlpool-client...)
            LogUtil.setLoggersDebug();
        }
    }

    public void startService() {
        TorServiceController.startTor();
    }

    private void setUpChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel whirlpoolChannel = new NotificationChannel(
                    WHIRLPOOL_CHANNEL,
                    "Whirlpool service ",
                    NotificationManager.IMPORTANCE_LOW
            );
            whirlpoolChannel.enableLights(false);
            whirlpoolChannel.enableVibration(false);
            whirlpoolChannel.setSound(null, null);

            NotificationChannel serviceChannel = new NotificationChannel(
                    TOR_CHANNEL_ID,
                    "Tor service ",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            serviceChannel.setSound(null, null);
            NotificationManager manager = getSystemService(NotificationManager.class);

            NotificationChannel refreshService = new NotificationChannel(
                    FOREGROUND_SERVICE_CHANNEL_ID,
                    "Samourai Service",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            refreshService.setSound(null, null);
            refreshService.setImportance(NotificationManager.IMPORTANCE_LOW);
            refreshService.setLockscreenVisibility(Notification.VISIBILITY_SECRET);


            NotificationChannel whirlpoolNotifications = new NotificationChannel(
                    WHIRLPOOL_NOTIFICATIONS,
                    "Mix status notifications",
                    NotificationManager.IMPORTANCE_HIGH
            );
            whirlpoolChannel.enableLights(true);

            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
                manager.createNotificationChannel(refreshService);
                manager.createNotificationChannel(whirlpoolChannel);
                manager.createNotificationChannel(whirlpoolNotifications);
            }
        }
    }

    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    @Override
    public void onTerminate() {
        TorServiceController.stopTor();
        super.onTerminate();
    }

    private void setUpTorService() {
        TorManager.INSTANCE.setUp(this);
        if (PrefsUtil.getInstance(this).getValue(PrefsUtil.ENABLE_TOR, false) && !PrefsUtil.getInstance(this).getValue(PrefsUtil.OFFLINE, false)) {
            startService();
        }
    }

}
