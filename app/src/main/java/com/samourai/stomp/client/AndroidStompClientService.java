package com.samourai.stomp.client;


import one.anom.wallet.tor.TorManager;

public class AndroidStompClientService implements IStompClientService {
    private TorManager torManager;

    public AndroidStompClientService(TorManager torManager) {
        this.torManager = torManager;
    }

    @Override
    public IStompClient newStompClient() {
        return new AndroidStompClient(torManager);
    }
}
