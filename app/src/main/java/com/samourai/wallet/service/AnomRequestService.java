package com.samourai.wallet.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.annotation.Nullable;

import com.samourai.wallet.access.AccessFactory;
import com.samourai.wallet.bip47.BIP47Util;
import com.samourai.wallet.hd.HD_Wallet;
import com.samourai.wallet.payload.PayloadUtil;
import com.samourai.wallet.util.AddressFactory;
import com.samourai.wallet.util.CharSequenceX;
import com.samourai.wallet.util.PrefsUtil;

import org.apache.commons.codec.DecoderException;
import org.bitcoinj.crypto.MnemonicException;

import java.util.ArrayList;

public class AnomRequestService extends Service {

    private boolean useSegwit = true;

    /**
     * Keeps track of all current registered clients.
     */
    ArrayList<Messenger> mClients = new ArrayList<Messenger>();

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

    /**
     * Handler of incoming messages from clients.
     */
    class IncomingHandler extends Handler {
        private Context applicationContext;
        private HD_Wallet mhdWallet;

        IncomingHandler(Context context) {

            applicationContext = context.getApplicationContext();
            useSegwit = PrefsUtil.getInstance(applicationContext).getValue(PrefsUtil.USE_SEGWIT,
                    true);
        }

        @Override
        public void handleMessage(Message msg) {

            switch (msg.what) {

                case MSG_REGISTER_CLIENT:

                    mClients.add(msg.replyTo);
                    if(mhdWallet == null) {

                        try {
                            mhdWallet = PayloadUtil.getInstance(applicationContext).
                                    restoreWalletfromJSON(new CharSequenceX(AccessFactory.
                                            getInstance(applicationContext).getGUID() + "55555"));
                        } catch (DecoderException e) {
                            e.printStackTrace();
                        } catch (MnemonicException.MnemonicLengthException e) {
                            e.printStackTrace();
                        }
                    }

                    break;
                case MSG_UNREGISTER_CLIENT:

                    mClients.remove(msg.replyTo);
                    break;
                case MSG_SAY_HELLO:

                    for (int i = mClients.size() - 1; i >= 0; i--) {

                        try {
                            Bundle bundle = new Bundle();
                            bundle.putString("hello_world", "Hello World");
                            mClients.get(i).send(Message.obtain(null, MSG_SAY_HELLO, bundle));
                        } catch (RemoteException e) {
                            // The client is dead.  Remove it from the list;
                            // we are going through the list from back to front
                            // so this is safe to do inside the loop.
                            mClients.remove(i);
                        }
                    }
                    break;
                case MSG_GET_ADDRESS:

                    for (int i = mClients.size() - 1; i >= 0; i--) {
                        try {
                            if (mhdWallet != null) {

                                String address;
                                if (useSegwit) {
                                    final String addr84 = AddressFactory.getInstance(
                                            applicationContext).getBIP84(AddressFactory.
                                            RECEIVE_CHAIN).getBech32AsString();
                                    final String addr49 = AddressFactory.getInstance(
                                            applicationContext).getBIP49(AddressFactory.
                                            RECEIVE_CHAIN).getAddressAsString();

                                    address = addr49;
                                } else {
                                    final String addr44 = AddressFactory.getInstance(
                                            applicationContext).get(AddressFactory.RECEIVE_CHAIN).
                                            getAddressString();
                                    address = addr44;
                                }

                                Bundle bundle = new Bundle();
                                bundle.putString("address", address);
                                mClients.get(i).send(Message.obtain(null, MSG_GET_ADDRESS,
                                        bundle));
                            }
                        } catch (RemoteException e) {
                            // The client is dead.  Remove it from the list;
                            // we are going through the list from back to front
                            // so this is safe to do inside the loop.
                            mClients.remove(i);
                        }
                    }
                    break;
                case MSG_GET_PAYNYM:

                    for (int i = mClients.size() - 1; i >= 0; i--) {
                        try {
                            if (mhdWallet != null) {

                                String pcode = BIP47Util.getInstance(applicationContext).
                                        getPaymentCode().toString();
                                Bundle bundle = new Bundle();
                                bundle.putString("pcode", pcode);
                                mClients.get(i).send(Message.obtain(null, MSG_GET_PAYNYM,
                                        bundle));
                            }
                        } catch (RemoteException e) {
                            // The client is dead.  Remove it from the list;
                            // we are going through the list from back to front
                            // so this is safe to do inside the loop.
                            mClients.remove(i);
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
     * Target we publish for clients to send messages to IncomingHandler.
     */
    Messenger mMessenger;

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
