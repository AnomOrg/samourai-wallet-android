package com.samourai.wallet.service;

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

import com.samourai.wallet.access.AccessFactory;
import com.samourai.wallet.api.APIFactory;
import com.samourai.wallet.bip47.BIP47Util;
import com.samourai.wallet.hd.HD_Wallet;
import com.samourai.wallet.hd.HD_WalletFactory;
import com.samourai.wallet.payload.PayloadUtil;
import com.samourai.wallet.util.AddressFactory;
import com.samourai.wallet.util.CharSequenceX;
import com.samourai.wallet.util.PrefsUtil;

import org.apache.commons.codec.DecoderException;
import org.bitcoinj.crypto.MnemonicException;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

public class AnomRequestService extends Service {

    /**
     * Command to the service to register a client, receiving callbacks
     * from the service.  The Message's replyTo field must be a Messenger of
     * the client where callbacks should be sent.
     */
    static final int MSG_REGISTER_CLIENT = 1;
    /**
     * Command to the service to unregister a client, ot stop receiving callbacks
     * from the service.  The Message's replyTo field must be a Messenger of
     * the client as previously given with MSG_REGISTER_CLIENT.
     */
    static final int MSG_UNREGISTER_CLIENT = 2;
    static final int MSG_SAY_HELLO = 3;
    static final int MSG_GET_ADDRESS = 4;
    static final int MSG_GET_PAYNYM = 5;
    static final int MSG_GET_PIN = 6;

    static final String BITCOIN_ADDRESS = "address";
    static final String PAY_NUM_CODE = "pcode";
    static final String PIN = "pin";

    /**
     * Target we publish for clients to send messages to IncomingHandler.
     */
    Messenger mMessenger;
    /**
     * Keeps track of all current registered clients.
     */
    ArrayList<Messenger> mClients = new ArrayList<Messenger>();

    /**
     * Handler of incoming messages from clients.
     */
    static class IncomingHandler extends Handler {

        private boolean useSegwit;
        private WeakReference<AnomRequestService> anomRequestServiceWeakReference;
        private HD_Wallet mhdWallet;

        IncomingHandler(AnomRequestService anomRequestService) {

            anomRequestServiceWeakReference = new WeakReference<>(anomRequestService);
            useSegwit = PrefsUtil.getInstance(anomRequestServiceWeakReference.get()).
                    getValue(PrefsUtil.USE_SEGWIT, true);
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
                case MSG_SAY_HELLO:

                    for (int i = anomRequestService.mClients.size() - 1; i >= 0; i--) {
                        try {
                            Bundle bundle = new Bundle();
                            bundle.putString("hello_world", "Hello World");
                            anomRequestService.mClients.get(i).send(
                                    Message.obtain(null, MSG_SAY_HELLO, bundle));
                        } catch (RemoteException e) {
                            // The client is dead.  Remove it from the list;
                            // we are going through the list from back to front
                            // so this is safe to do inside the loop.
                            anomRequestService.mClients.remove(i);
                        }
                    }
                    break;
                case MSG_GET_ADDRESS:

                    // check if wallet is restored with the pin
                    if (mhdWallet == null) {
                        Bundle bundle = new Bundle();
                        for (int i = anomRequestService.mClients.size() - 1; i >= 0; i--) {

                            try {
                                anomRequestService.mClients.get(i).send(Message.obtain(
                                        null, MSG_GET_PIN, bundle));
                            } catch (RemoteException e) {
                                e.printStackTrace();
                            }
                        }
                        break;
                    }

                    APIFactory.getInstance(anomRequestService).initWallet();
                    for (int i = anomRequestService.mClients.size() - 1; i >= 0; i--) {
                        try {
                            if (mhdWallet != null) {

                                String address;
                                if (useSegwit) {
                                    final String addr84 = AddressFactory.
                                            getInstance(anomRequestService).
                                            getBIP84(AddressFactory.RECEIVE_CHAIN).
                                            getBech32AsString();

                                    final String addr49 = AddressFactory
                                            .getInstance(anomRequestService).
                                                    getBIP49(AddressFactory.RECEIVE_CHAIN).
                                                    getAddressAsString();

                                    address = addr49;
                                } else {
                                    final String addr44 = AddressFactory.
                                            getInstance(anomRequestService).
                                            get(AddressFactory.RECEIVE_CHAIN).getAddressString();

                                    address = addr44;
                                }

                                Bundle bundle = new Bundle();
                                bundle.putString(BITCOIN_ADDRESS, address);
                                anomRequestService.mClients.get(i).send(Message.obtain(
                                        null, MSG_GET_ADDRESS, bundle));
                            }
                        } catch (RemoteException e) {
                            // The client is dead.  Remove it from the list;
                            // we are going through the list from back to front
                            // so this is safe to do inside the loop.
                            anomRequestService.mClients.remove(i);
                        }
                    }
                    break;
                case MSG_GET_PAYNYM:

                    // check if wallet is restored with the pin
                    if (mhdWallet == null) {
                        Bundle bundle = new Bundle();
                        for (int i = anomRequestService.mClients.size() - 1; i >= 0; i--) {

                            try {
                                anomRequestService.mClients.get(i).send(Message.obtain(
                                        null, MSG_GET_PIN, bundle));
                            } catch (RemoteException e) {
                                e.printStackTrace();
                            }
                        }
                        break;
                    }

                    for (int i = anomRequestService.mClients.size() - 1; i >= 0; i--) {
                        try {
                            if (mhdWallet != null) {

                                String pcode = BIP47Util.getInstance(anomRequestService).
                                        getPaymentCode().toString();

                                Bundle bundle = new Bundle();
                                bundle.putString(PAY_NUM_CODE, pcode);
                                anomRequestService.mClients.get(i).send(
                                        Message.obtain(null, MSG_GET_PAYNYM, bundle));
                            }
                        } catch (RemoteException e) {
                            // The client is dead.  Remove it from the list;
                            // we are going through the list from back to front
                            // so this is safe to do inside the loop.
                            anomRequestService.mClients.remove(i);
                        }
                    }
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
                    break;
                default:
                    super.handleMessage(msg);
                    break;
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
