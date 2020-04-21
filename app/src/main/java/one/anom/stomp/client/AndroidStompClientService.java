package one.anom.stomp.client;

import com.samourai.stomp.client.IStompClient;
import com.samourai.stomp.client.IStompClientService;

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
