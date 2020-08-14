package one.anom.wallet.service;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.samourai.wallet.bip47.rpc.PaymentCode;
import com.samourai.wallet.crypto.AESUtil;
import com.samourai.wallet.crypto.DecryptionException;
import com.samourai.wallet.util.CharSequenceX;

import one.anom.wallet.R;
import one.anom.wallet.SamouraiWallet;
import one.anom.wallet.access.AccessFactory;
import one.anom.wallet.api.APIFactory;
import one.anom.wallet.bip47.BIP47Meta;
import one.anom.wallet.bip47.BIP47Util;
import one.anom.wallet.hd.HD_WalletFactory;
import one.anom.wallet.payload.PayloadUtil;
import one.anom.wallet.segwit.BIP49Util;
import one.anom.wallet.segwit.BIP84Util;
import one.anom.wallet.util.AddressFactory;
import one.anom.wallet.util.AppUtil;

import one.anom.wallet.util.ExchangeRateFactory;
import one.anom.wallet.util.PrefsUtil;
import one.anom.wallet.whirlpool.WhirlpoolMeta;

import org.apache.commons.lang3.tuple.Pair;
import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.crypto.MnemonicException;
import org.json.JSONException;

import java.io.IOException;
import java.util.List;

import static one.anom.wallet.AnomApplication.FOREGROUND_SERVICE_CHANNEL_ID;

public class RefreshService extends IntentService {

    private boolean dragged = false;
    private boolean launch = false;
    private boolean notifTx = false;

    public RefreshService() {
        super("RefreshService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationChannel channel = new NotificationChannel("1001",
                    "refresh_channel",
                    NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("refresh");
            if (nm != null) {
                nm.createNotificationChannel(channel);
            }
            Notification.Builder builder = new Notification.Builder(this, "1001")
                    .setContentTitle(getString(R.string.app_name))
                    .setContentText("refresh")
                    .setAutoCancel(true);
            Notification notification = builder.build();
            startForeground(1001, notification);
        } else {
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                    .setContentTitle(getString(R.string.app_name))
                    .setContentText("refresh")
                    .setAutoCancel(true);
            Notification notification = builder.build();
            startForeground(1001, notification);
        }

        dragged = intent.getBooleanExtra("dragged", false);
        launch = intent.getBooleanExtra("launch", false);
        notifTx = intent.getBooleanExtra("notifTx", false);


        APIFactory.getInstance(RefreshService.this).stayingAlive();

        APIFactory.getInstance(RefreshService.this).initWallet();

        try {
            int acc = 0;

            if (AddressFactory.getInstance().getHighestTxReceiveIdx(acc) > HD_WalletFactory.getInstance(RefreshService.this).get().getAccount(acc).getReceive().getAddrIdx()) {
                HD_WalletFactory.getInstance(RefreshService.this).get().getAccount(acc).getReceive().setAddrIdx(AddressFactory.getInstance().getHighestTxReceiveIdx(acc));
            }
            if (AddressFactory.getInstance().getHighestTxChangeIdx(acc) > HD_WalletFactory.getInstance(RefreshService.this).get().getAccount(acc).getChange().getAddrIdx()) {
                HD_WalletFactory.getInstance(RefreshService.this).get().getAccount(acc).getChange().setAddrIdx(AddressFactory.getInstance().getHighestTxChangeIdx(acc));
            }

            if (AddressFactory.getInstance().getHighestBIP49ReceiveIdx() > BIP49Util.getInstance(RefreshService.this).getWallet().getAccount(0).getReceive().getAddrIdx()) {
                BIP49Util.getInstance(RefreshService.this).getWallet().getAccount(0).getReceive().setAddrIdx(AddressFactory.getInstance().getHighestBIP49ReceiveIdx());
            }
            if (AddressFactory.getInstance().getHighestBIP49ChangeIdx() > BIP49Util.getInstance(RefreshService.this).getWallet().getAccount(0).getChange().getAddrIdx()) {
                BIP49Util.getInstance(RefreshService.this).getWallet().getAccount(0).getChange().setAddrIdx(AddressFactory.getInstance().getHighestBIP49ChangeIdx());
            }

            if (AddressFactory.getInstance().getHighestBIP84ReceiveIdx() > BIP84Util.getInstance(RefreshService.this).getWallet().getAccount(0).getReceive().getAddrIdx()) {
                BIP84Util.getInstance(RefreshService.this).getWallet().getAccount(0).getReceive().setAddrIdx(AddressFactory.getInstance().getHighestBIP84ReceiveIdx());
            }
            if (AddressFactory.getInstance().getHighestBIP84ChangeIdx() > BIP84Util.getInstance(RefreshService.this).getWallet().getAccount(0).getChange().getAddrIdx()) {
                BIP84Util.getInstance(RefreshService.this).getWallet().getAccount(0).getChange().setAddrIdx(AddressFactory.getInstance().getHighestBIP84ChangeIdx());
            }

        } catch (IOException ioe) {
            ioe.printStackTrace();
        } catch (MnemonicException.MnemonicLengthException mle) {
            mle.printStackTrace();
        } catch (NullPointerException npe) {
            npe.printStackTrace();
        }
        finally {
            Intent _intent = new Intent("com.samourai.wallet.BalanceFragment.DISPLAY");
            LocalBroadcastManager.getInstance(RefreshService.this).sendBroadcast(_intent);
            ExchangeRateFactory.getInstance(RefreshService.this).exchangeRateThread();
        }

        PrefsUtil.getInstance(RefreshService.this).setValue(PrefsUtil.FIRST_RUN, false);

        if (notifTx && !AppUtil.getInstance(RefreshService.this).isOfflineMode()) {
            //
            // check for incoming payment code notification tx
            //
            try {
                PaymentCode pcode = BIP47Util.getInstance(RefreshService.this).getPaymentCode();
//                    Log.i("BalanceFragment", "payment code:" + pcode.toString());
//                    Log.i("BalanceFragment", "notification address:" + pcode.notificationAddress().getAddressString());
                APIFactory.getInstance(RefreshService.this).getNotifAddress(pcode.notificationAddress(SamouraiWallet.getInstance().getCurrentNetworkParams()).getAddressString());
            } catch (AddressFormatException afe) {
                afe.printStackTrace();
                Toast.makeText(RefreshService.this, "HD wallet error", Toast.LENGTH_SHORT).show();
            }

            //
            // check on outgoing payment code notification tx
            //
            List<Pair<String, String>> outgoingUnconfirmed = BIP47Meta.getInstance().getOutgoingUnconfirmed();
//                Log.i("BalanceFragment", "outgoingUnconfirmed:" + outgoingUnconfirmed.size());
            for (Pair<String, String> pair : outgoingUnconfirmed) {
//                    Log.i("BalanceFragment", "outgoing payment code:" + pair.getLeft());
//                    Log.i("BalanceFragment", "outgoing payment code tx:" + pair.getRight());
                int confirmations = APIFactory.getInstance(RefreshService.this).getNotifTxConfirmations(pair.getRight());
                if (confirmations > 0) {
                    BIP47Meta.getInstance().setOutgoingStatus(pair.getLeft(), BIP47Meta.STATUS_SENT_CFM);
                }
                if (confirmations == -1) {
                    BIP47Meta.getInstance().setOutgoingStatus(pair.getLeft(), BIP47Meta.STATUS_NOT_SENT);
                }
            }

            Intent _intent = new Intent("one.anom.wallet.MainActivity2.RESTART_SERVICE");
            LocalBroadcastManager.getInstance(RefreshService.this).sendBroadcast(_intent);
        }

        if (launch) {

            if (PrefsUtil.getInstance(RefreshService.this).getValue(PrefsUtil.GUID_V, 0) < 4) {
                Log.i("RefreshService", "guid_v < 4");
                try {
                    String _guid = AccessFactory.getInstance(RefreshService.this).createGUID();
                    String _hash = AccessFactory.getInstance(RefreshService.this).getHash(_guid, new CharSequenceX(AccessFactory.getInstance(RefreshService.this).getPIN()), AESUtil.DefaultPBKDF2Iterations);

                    PayloadUtil.getInstance(RefreshService.this).saveWalletToJSON(new CharSequenceX(_guid + AccessFactory.getInstance().getPIN()));

                    PrefsUtil.getInstance(RefreshService.this).setValue(PrefsUtil.ACCESS_HASH, _hash);
                    PrefsUtil.getInstance(RefreshService.this).setValue(PrefsUtil.ACCESS_HASH2, _hash);

                    Log.i("RefreshService", "guid_v == 4");
                } catch (MnemonicException.MnemonicLengthException | IOException | JSONException | DecryptionException e) {
                    ;
                }
            }

            if (PrefsUtil.getInstance(RefreshService.this).getValue(PrefsUtil.XPUB44LOCK, false) == false) {

                try {
                    String[] s = HD_WalletFactory.getInstance(RefreshService.this).get().getXPUBs();
                    APIFactory.getInstance(RefreshService.this).lockXPUB(s[0], 44, null);
                } catch (IOException | MnemonicException.MnemonicLengthException e) {
                    ;
                }

            }

            if (PrefsUtil.getInstance(RefreshService.this).getValue(PrefsUtil.XPUB49LOCK, false) == false) {
                String ypub = BIP49Util.getInstance(RefreshService.this).getWallet().getAccount(0).ypubstr();
                APIFactory.getInstance(RefreshService.this).lockXPUB(ypub, 49, null);
            }

            if (PrefsUtil.getInstance(RefreshService.this).getValue(PrefsUtil.XPUB84LOCK, false) == false) {
                String zpub = BIP84Util.getInstance(RefreshService.this).getWallet().getAccount(0).zpubstr();
                APIFactory.getInstance(RefreshService.this).lockXPUB(zpub, 84, null);
            }

            if (PrefsUtil.getInstance(RefreshService.this).getValue(PrefsUtil.XPUBPRELOCK, false) == false) {
                String zpub = BIP84Util.getInstance(RefreshService.this).getWallet().getAccountAt(WhirlpoolMeta.getInstance(RefreshService.this).getWhirlpoolPremixAccount()).zpubstr();
                APIFactory.getInstance(RefreshService.this).lockXPUB(zpub, 84, PrefsUtil.XPUBPRELOCK);
            }

            if (PrefsUtil.getInstance(RefreshService.this).getValue(PrefsUtil.XPUBPOSTLOCK, false) == false) {
                String zpub = BIP84Util.getInstance(RefreshService.this).getWallet().getAccountAt(WhirlpoolMeta.getInstance(RefreshService.this).getWhirlpoolPostmix()).zpubstr();
                APIFactory.getInstance(RefreshService.this).lockXPUB(zpub, 84, PrefsUtil.XPUBPRELOCK);
            }


            if(PrefsUtil.getInstance(RefreshService.this).getValue(PrefsUtil.XPUBBADBANKLOCK, false) == false)    {
                String zpub = BIP84Util.getInstance(RefreshService.this).getWallet().getAccountAt(WhirlpoolMeta.getInstance(RefreshService.this).getWhirlpoolBadBank()).zpubstr();
                APIFactory.getInstance(RefreshService.this).lockXPUB(zpub, 84, PrefsUtil.XPUBBADBANKLOCK);
            }

        }
        else    {

            try {
                PayloadUtil.getInstance(RefreshService.this).saveWalletToJSON(new CharSequenceX(AccessFactory.getInstance(RefreshService.this).getGUID() + AccessFactory.getInstance(RefreshService.this).getPIN()));
            } catch (Exception e) {
                ;
            }

        }

        Intent _intent = new Intent("one.anom.wallet.BalanceFragment.DISPLAY");
        LocalBroadcastManager.getInstance(RefreshService.this).sendBroadcast(_intent);

    }

}
