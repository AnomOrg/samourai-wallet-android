package com.samourai.wallet.anom;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.samourai.wallet.MainActivity2;
import com.samourai.wallet.access.AccessFactory;
import com.samourai.wallet.bip47.BIP47Util;
import com.samourai.wallet.hd.HD_Wallet;
import com.samourai.wallet.hd.HD_WalletFactory;
import com.samourai.wallet.payload.PayloadUtil;
import com.samourai.wallet.util.CharSequenceX;
import com.samourai.wallet.util.PrefsUtil;

import org.apache.commons.codec.DecoderException;
import org.bitcoinj.crypto.MnemonicException;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

public class AnomRequestService extends Service {

    /**
     * Command to the service to register a client, receiving callbacks
     * from the service. The Message's replyTo field must be a Messenger of
     * the client where callbacks should be sent.
     */
    static final int MSG_REGISTER_CLIENT = 1;

    /**
     * Command to the service to unregister a client, to stop receiving callbacks
     * from the service. The Message's replyTo field must be a Messenger of
     * the client as previously given with MSG_REGISTER_CLIENT.
     */
    static final int MSG_UNREGISTER_CLIENT = 2;
    static final int MSG_GET_PAYNYM = 3;
    static final int MSG_GET_PIN = 4;
    static final int MSG_INITIATE_PAYMENT = 5;

    static final String PAY_NUM_CODE = "pcode";
    static final String PIN = "pin";

    /**
     * Target we publish for clients to send messages to IncomingHandler.
     */
    Messenger mMessenger;

    /**
     * Keeps track of all current registered clients.
     */
    ArrayList<Messenger> mClients = new ArrayList<>();

    private static int mLastMessageRequest = -1;

    /**
     * Handler of incoming messages from clients.
     */
    static class IncomingHandler extends Handler {

        private WeakReference<AnomRequestService> anomRequestServiceWeakReference;
        private HD_Wallet mhdWallet;

        IncomingHandler(AnomRequestService anomRequestService) {

            anomRequestServiceWeakReference = new WeakReference<>(anomRequestService);
        }

        @Override
        public void handleMessage(Message msg) {

            final AnomRequestService anomRequestService = anomRequestServiceWeakReference.get();
            switch (msg.what) {

                case MSG_REGISTER_CLIENT:

                    anomRequestService.mClients.add(msg.replyTo);
                    break;
                case MSG_UNREGISTER_CLIENT:

                    anomRequestService.mClients.remove(msg.replyTo);
                    if (anomRequestService.mClients.size() == 0) {
                        HD_WalletFactory.getInstance(anomRequestService).clear();
                    }
                    break;
                case MSG_GET_PAYNYM:

                    // check if wallet is restored with the pin
                    if (mhdWallet == null) {
                        Bundle bundle = new Bundle();
                        for (int i = anomRequestService.mClients.size() - 1; i >= 0; i--) {

                            try {
                                anomRequestService.mClients.get(i).send(Message.obtain(null,
                                        MSG_GET_PIN, bundle));
                            } catch (RemoteException e) {
                                e.printStackTrace();
                            }
                        }
                        mLastMessageRequest = MSG_GET_PAYNYM;
                        break;
                    }

                    sendPaynymCode(anomRequestService);
                    break;
                case MSG_GET_PIN:

                    String pin = null;
                    if (msg.obj != null) {
                        Bundle bundle = (Bundle) msg.obj;
                        final String data = bundle.getString(PIN);

                        if (!TextUtils.isEmpty(data)) {
                            pin = data;
                        }
                    }

                    if (mhdWallet == null && anomRequestService.mClients.size() == 1 &&
                            pin != null) {

                        try {
                            mhdWallet = PayloadUtil.getInstance(anomRequestService).
                                    restoreWalletfromJSON(new CharSequenceX(AccessFactory.
                                            getInstance(anomRequestService).getGUID() + pin));
                        } catch (DecoderException e) {
                            e.printStackTrace();
                        } catch (MnemonicException.MnemonicLengthException e) {
                            e.printStackTrace();
                        }
                    }

                    if (mLastMessageRequest == MSG_GET_PAYNYM) {

                        sendPaynymCode(anomRequestService);
                    }

                    // reset last message request
                    mLastMessageRequest = -1;
                    break;
                case MSG_INITIATE_PAYMENT:

                    double amount = 0.00;
                    String pCode = null;
                    if (msg.obj != null) {

                        Bundle bundle = (Bundle) msg.obj;

                        amount = bundle.getDouble("amount");
                        final String data = bundle.getString(PAY_NUM_CODE);
                        if (!TextUtils.isEmpty(data)) {

                            pCode = data;
                        }
                    }

                    if (pCode != null) {

                        AccessFactory.getInstance(anomRequestService).setIsLoggedIn(false);
                        Intent intent = new Intent(anomRequestService, MainActivity2.class);

                        if (PrefsUtil.getInstance(anomRequestService).getValue(
                                PrefsUtil.ICON_HIDDEN, false)) {

                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK |
                                    Intent.FLAG_ACTIVITY_NEW_TASK |
                                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS |
                                    Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        } else {

                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK |
                                    Intent.FLAG_ACTIVITY_NEW_TASK |
                                    Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        }

                        Bundle bundle = new Bundle();
                        bundle.putString(PAY_NUM_CODE, pCode);
                        bundle.putDouble("amount", amount);

                        intent.putExtras(bundle);

                        anomRequestService.startActivity(intent);
                    }
                    break;
                default:

                    super.handleMessage(msg);
                    break;
            }
        }

        private void sendPaynymCode(AnomRequestService anomRequestService) {

            String pCode;
            for (int i = anomRequestService.mClients.size() - 1; i >= 0; i--) {

                try {
                    if (mhdWallet != null) {

                        pCode = BIP47Util.getInstance(anomRequestService).getPaymentCode().
                                toString();

                        Bundle bundle = new Bundle();
                        bundle.putString(PAY_NUM_CODE, pCode);
                        anomRequestService.mClients.get(i).send(Message.obtain(null,
                                MSG_GET_PAYNYM, bundle));
                    }
                } catch (RemoteException e) {

                    // The client is dead.  Remove it from the list;
                    // we are going through the list from back to front
                    // so this is safe to do inside the loop.
                    anomRequestService.mClients.remove(i);
                }
            }
        }
    }

    /**
     * When binding to the service, we return an interface to our messenger
     * for sending messages to the service.
     */
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {

        mMessenger = new Messenger(new IncomingHandler(this));
        return mMessenger.getBinder();
    }
}