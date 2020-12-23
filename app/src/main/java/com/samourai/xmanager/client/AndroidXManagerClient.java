package com.samourai.xmanager.client;

import android.content.Context;

import com.samourai.http.client.AndroidHttpClient;
import one.anom.wallet.SamouraiWallet;
import one.anom.wallet.tor.TorManager;

public class AndroidXManagerClient extends XManagerClient {
    private static AndroidXManagerClient instance;

    public static AndroidXManagerClient getInstance(Context ctx) {
        if (instance == null) {
            instance = new AndroidXManagerClient(ctx);
        }
        return instance;
    }

    private AndroidXManagerClient(Context ctx) {
        super(SamouraiWallet.getInstance().isTestNet(), TorManager.INSTANCE.isConnected(), AndroidHttpClient.getInstance(ctx));
    }
}
