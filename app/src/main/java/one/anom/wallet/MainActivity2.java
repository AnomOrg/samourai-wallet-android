package one.anom.wallet;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.widget.TextView;
import android.widget.Toast;

import com.auth0.android.jwt.JWT;
import com.invertedx.torservice.TorProxyManager;

import one.anom.wallet.access.AccessFactory;
import one.anom.wallet.api.APIFactory;
import com.samourai.wallet.crypto.AESUtil;
import one.anom.wallet.home.BalanceActivity;
import one.anom.wallet.payload.PayloadUtil;
import one.anom.wallet.prng.PRNGFixes;
import one.anom.wallet.service.BackgroundManager;
import one.anom.wallet.service.WebSocketService;
import one.anom.wallet.tor.TorManager;
import one.anom.wallet.util.AppUtil;
import com.samourai.wallet.util.CharSequenceX;
import one.anom.wallet.util.ConnectivityStatus;
import one.anom.wallet.util.LogUtil;
import one.anom.wallet.util.PrefsUtil;
import one.anom.wallet.util.TimeOutUtil;

import org.apache.commons.codec.DecoderException;
import org.bitcoinj.crypto.MnemonicException;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.params.TestNet3Params;

import io.reactivex.Completable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class MainActivity2 extends Activity {

    private ProgressDialog progress = null;
    public static final String ACTION_RESTART = "com.samourai.wallet.MainActivity2.RESTART_SERVICE";
    private AlertDialog.Builder dlg;
    private boolean pinEntryActivityLaunched = false;
    private static final String TAG = "MainActivity2";
    private TextView loaderTxView;
    private CompositeDisposable compositeDisposables = new CompositeDisposable();

    protected BroadcastReceiver receiver_restart = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            if (ACTION_RESTART.equals(intent.getAction())) {

//                ReceiversUtil.getInstance(MainActivity2.this).initReceivers();

                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                    if (AppUtil.getInstance(MainActivity2.this.getApplicationContext()).isServiceRunning(WebSocketService.class)) {
                        stopService(new Intent(MainActivity2.this.getApplicationContext(), WebSocketService.class));
                    }
                    startService(new Intent(MainActivity2.this.getApplicationContext(), WebSocketService.class));
                }

            }

        }
    };

    protected BackgroundManager.Listener bgListener = new BackgroundManager.Listener() {

        public void onBecameForeground() {
//
//            Intent intent = new Intent("com.samourai.wallet.BalanceFragment.REFRESH");
//            intent.putExtra("notifTx", false);
//            LocalBroadcastManager.getInstance(MainActivity2.this.getApplicationContext()).sendBroadcast(intent);
//
//            Intent _intent = new Intent("com.samourai.wallet.MainActivity2.RESTART_SERVICE");
//            LocalBroadcastManager.getInstance(MainActivity2.this.getApplicationContext()).sendBroadcast(_intent);

        }

        public void onBecameBackground() {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (AppUtil.getInstance(MainActivity2.this.getApplicationContext()).isServiceRunning(WebSocketService.class)) {
                    stopService(new Intent(MainActivity2.this.getApplicationContext(), WebSocketService.class));
                }
            }

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {

                        try {
                            PayloadUtil.getInstance(MainActivity2.this).saveWalletToJSON(new CharSequenceX(AccessFactory.getInstance(MainActivity2.this).getGUID() + AccessFactory.getInstance(MainActivity2.this).getPIN()));
                        } catch (Exception e) {
                            ;
                        }

                    }
                }).start();
            }

        }

    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        loaderTxView = findViewById(R.id.loader_text);


        if (PrefsUtil.getInstance(MainActivity2.this).getValue(PrefsUtil.TESTNET, false) == true) {
            AnomWallet.getInstance().setCurrentNetworkParams(TestNet3Params.get());
        }

//        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
        BackgroundManager.get(MainActivity2.this).addListener(bgListener);
//        }

        // Apply PRNG fixes for Android 4.1
        if (!AppUtil.getInstance(MainActivity2.this).isPRNG_FIXED()) {
            PRNGFixes.apply();
            AppUtil.getInstance(MainActivity2.this).setPRNG_FIXED(true);
        }

        if (TorManager.getInstance(getApplicationContext()).isRequired() && ConnectivityStatus.hasConnectivity(getApplicationContext()) && !TorManager.getInstance(getApplicationContext()).isConnected()) {
            loaderTxView.setText(getText(R.string.initializing_tor));
            ((AnomApplication) getApplication()).startService();
            Disposable disposable = TorManager.getInstance(this)
                    .getTorStatus()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(connection_states -> {
                        if (connection_states == TorProxyManager.ConnectionStatus.CONNECTED) {
                            initAppOnCreate();
                        }
                    });
            compositeDisposables.add(disposable);
        } else {
            initAppOnCreate();
        }

    }

    private void initAppOnCreate() {
        if (AppUtil.getInstance(MainActivity2.this).isOfflineMode() &&
                !(AccessFactory.getInstance(MainActivity2.this).getGUID().length() < 1 || !PayloadUtil.getInstance(MainActivity2.this).walletFileExists())) {
            Toast.makeText(MainActivity2.this, R.string.in_offline_mode, Toast.LENGTH_SHORT).show();
            doAppInit0(false, null, null);
        } else {
//            SSLVerifierThreadUtil.getInstance(MainActivity2.this).validateSSLThread();
//            APIFactory.getInstance(MainActivity2.this).validateAPIThread();

            String action = getIntent().getAction();
            String scheme = getIntent().getScheme();
            String strUri = null;
            boolean isDial = false;
//                String strUri = null;
            String strPCode = null;
            if (action != null && Intent.ACTION_VIEW.equals(action) && scheme.equals("bitcoin")) {
                strUri = getIntent().getData().toString();
            } else {
                Bundle extras = getIntent().getExtras();
                if (extras != null && extras.containsKey("dialed")) {
                    isDial = extras.getBoolean("dialed");
                }
                if (extras != null && extras.containsKey("uri")) {
                    strUri = extras.getString("uri");
                }
                if (extras != null && extras.containsKey("pcode")) {
                    strPCode = extras.getString("pcode");
                }
            }

            doAppInit0(isDial, strUri, strPCode);

        }

    }

    @Override
    protected void onResume() {
        if (PrefsUtil.getInstance(this).getValue(PrefsUtil.ENABLE_TOR, false) && !TorManager.getInstance(getApplicationContext()).isConnected()) {

            ((AnomApplication) getApplication()).startService();

            Disposable disposable = TorManager.getInstance(getApplicationContext())
                    .getTorStatus()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(connection_states -> {
                        if (connection_states == TorProxyManager.ConnectionStatus.CONNECTED) {
                            initAppOnResume();
                            compositeDisposables.dispose();
                        }
                    });
            compositeDisposables.add(disposable);
        } else {
            initAppOnResume();
        }
        super.onResume();

    }

    private void initAppOnResume() {

        AppUtil.getInstance(MainActivity2.this).setIsInForeground(true);

        AppUtil.getInstance(MainActivity2.this).deleteQR();
        AppUtil.getInstance(MainActivity2.this).deleteBackup();

        IntentFilter filter_restart = new IntentFilter(ACTION_RESTART);
        LocalBroadcastManager.getInstance(MainActivity2.this).registerReceiver(receiver_restart, filter_restart);

        doAppInit0(false, null, null);

    }

    @Override
    protected void onPause() {
        super.onPause();

        LocalBroadcastManager.getInstance(MainActivity2.this).unregisterReceiver(receiver_restart);

        AppUtil.getInstance(MainActivity2.this).setIsInForeground(false);
    }

    @Override
    protected void onDestroy() {
        compositeDisposables.dispose();
        AppUtil.getInstance(MainActivity2.this).deleteQR();
        AppUtil.getInstance(MainActivity2.this).deleteBackup();

//        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
        BackgroundManager.get(this).removeListener(bgListener);
//        }

        super.onDestroy();
    }

    private void initDialog() {
        Intent intent = new Intent(MainActivity2.this, LandingActivity.class);
        startActivity(intent);
        finish();
    }

    private void validatePIN(String strUri) {
        if (!pinEntryActivityLaunched) {

            if (AccessFactory.getInstance(MainActivity2.this).isLoggedIn() && !TimeOutUtil.getInstance().isTimedOut()) {
                return;
            }

            AccessFactory.getInstance(MainActivity2.this).setIsLoggedIn(false);
            Intent intent = new Intent(MainActivity2.this, PinEntryActivity.class);
            if (strUri != null) {
                intent.putExtra("uri", strUri);
                PrefsUtil.getInstance(MainActivity2.this).setValue("SCHEMED_URI", strUri);
            }
            if (getBundleExtras() != null) {
                intent.putExtras(getBundleExtras());
            }
            startActivity(intent);
            finish();
            pinEntryActivityLaunched = true;

        }
    }

    private void launchFromDialer(final String pin) {

        if (progress != null && progress.isShowing()) {
            progress.dismiss();
            progress = null;
        }

        progress = new ProgressDialog(MainActivity2.this);
        progress.setCancelable(false);
        progress.setTitle(R.string.app_name);
        progress.setMessage(getString(R.string.please_wait));
        progress.show();

        new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();

                try {
                    PayloadUtil.getInstance(MainActivity2.this).restoreWalletfromJSON(new CharSequenceX(AccessFactory.getInstance(MainActivity2.this).getGUID() + pin));

                    if (progress != null && progress.isShowing()) {
                        progress.dismiss();
                        progress = null;
                    }

                    AccessFactory.getInstance(MainActivity2.this).setIsLoggedIn(true);
                    TimeOutUtil.getInstance().updatePin();
                    AppUtil.getInstance(MainActivity2.this).restartApp();
                } catch (MnemonicException.MnemonicLengthException mle) {
                    mle.printStackTrace();
                } catch (DecoderException de) {
                    de.printStackTrace();
                } finally {
                    if (progress != null && progress.isShowing()) {
                        progress.dismiss();
                        progress = null;
                    }
                }

                Looper.loop();

            }
        }).start();

    }

    private void doAppInit0(final boolean isDial, final String strUri, final String strPCode) {

        Disposable disposable = Completable.fromCallable(() -> {

            if (!APIFactory.getInstance(MainActivity2.this).APITokenRequired()) {
                doAppInit1(isDial, strUri, strPCode);
                return false;
            }
            boolean needToken = false;
            if (APIFactory.getInstance(MainActivity2.this).getAccessToken() == null) {
                needToken = true;
            } else {
                JWT jwt = new JWT(APIFactory.getInstance(MainActivity2.this).getAccessToken());
                if (jwt.isExpired(APIFactory.getInstance(MainActivity2.this).getAccessTokenRefresh())) {
                    APIFactory.getInstance(MainActivity2.this).getToken(true);
                    needToken = true;
                }
            }
            if (needToken && !AppUtil.getInstance(MainActivity2.this).isOfflineMode()) {

                APIFactory.getInstance(MainActivity2.this).stayingAlive();

                doAppInit1(isDial, strUri, strPCode);

                return true;

            } else {
                doAppInit1(isDial, strUri, strPCode);
            }
            return true;
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(() -> {
                }, e -> {
                    LogUtil.error(TAG, e.getMessage());
                    e.printStackTrace();
                    Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                });
        compositeDisposables.add(disposable);


    }


    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    private Bundle getBundleExtras() {
        Bundle bundle = getIntent().getExtras();
        if (bundle == null) {
            return null;
        }
        if (Intent.ACTION_VIEW.equals(getIntent().getAction()) && getIntent().getScheme() != null && getIntent().getScheme().equals("bitcoin")) {
            bundle.putString("uri", getIntent().getData().toString());
        } else {
            if (bundle.containsKey("uri")) {
                bundle.putString("uri", bundle.getString("uri"));
            }
        }

        return bundle;

    }

    private void doAppInit1(boolean isDial, final String strUri, final String strPCode) {

        if (AccessFactory.getInstance(MainActivity2.this).getGUID().length() < 1 || !PayloadUtil.getInstance(MainActivity2.this).walletFileExists()) {
            AccessFactory.getInstance(MainActivity2.this).setIsLoggedIn(false);
            if (AppUtil.getInstance(MainActivity2.this).isSideLoaded()) {
                runOnUiThread(this::doSelectNet);
            } else {
                runOnUiThread(this::initDialog);
            }
        } else if (isDial && AccessFactory.getInstance(MainActivity2.this).validateHash(PrefsUtil.getInstance(MainActivity2.this).getValue(PrefsUtil.ACCESS_HASH, ""), AccessFactory.getInstance(MainActivity2.this).getGUID(), new CharSequenceX(AccessFactory.getInstance(MainActivity2.this).getPIN()), AESUtil.DefaultPBKDF2Iterations)) {
            TimeOutUtil.getInstance().updatePin();
            launchFromDialer(AccessFactory.getInstance(MainActivity2.this).getPIN());
        } else if (TimeOutUtil.getInstance().isTimedOut()) {
            AccessFactory.getInstance(MainActivity2.this).setIsLoggedIn(false);
            validatePIN(strUri);
        } else if (AccessFactory.getInstance(MainActivity2.this).isLoggedIn() && !TimeOutUtil.getInstance().isTimedOut()) {
            TimeOutUtil.getInstance().updatePin();

            Intent intent = new Intent(MainActivity2.this, BalanceActivity.class);
            intent.putExtra("notifTx", true);
            intent.putExtra("fetch", true);
            if (getBundleExtras() != null) {
                intent.putExtras(getBundleExtras());
            }
            startActivity(intent);
        } else {
            AccessFactory.getInstance(MainActivity2.this).setIsLoggedIn(false);
            validatePIN(strUri == null ? null : strUri);
        }

    }

    private void doSelectNet() {
        if (dlg != null) {
            return;
        }
        dlg = new AlertDialog.Builder(this)
                .setTitle(R.string.app_name)
                .setMessage(R.string.select_network)
                .setCancelable(false)
                .setPositiveButton(R.string.MainNet, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        dialog.dismiss();
                        PrefsUtil.getInstance(MainActivity2.this).removeValue(PrefsUtil.TESTNET);
                        AnomWallet.getInstance().setCurrentNetworkParams(MainNetParams.get());
                        initDialog();

                    }
                })
                .setNegativeButton(R.string.TestNet, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        dialog.dismiss();
                        PrefsUtil.getInstance(MainActivity2.this).setValue(PrefsUtil.TESTNET, true);
                        AnomWallet.getInstance().setCurrentNetworkParams(TestNet3Params.get());
                        initDialog();

                    }
                });
        if (!isFinishing()) {
            dlg.show();
        }

    }

}
