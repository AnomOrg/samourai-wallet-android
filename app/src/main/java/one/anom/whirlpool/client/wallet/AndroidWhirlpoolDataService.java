package one.anom.whirlpool.client.wallet;

import one.anom.wallet.api.APIFactory;
import one.anom.wallet.api.backend.beans.UnspentResponse;
import one.anom.wallet.send.MyTransactionOutPoint;
import one.anom.wallet.send.UTXO;

import com.samourai.whirlpool.client.wallet.WhirlpoolDataService;
import com.samourai.whirlpool.client.wallet.WhirlpoolWallet;
import com.samourai.whirlpool.client.wallet.WhirlpoolWalletConfig;
import com.samourai.whirlpool.client.wallet.WhirlpoolWalletService;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolAccount;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import java8.util.stream.Collectors;
import java8.util.stream.StreamSupport;

public class AndroidWhirlpoolDataService extends WhirlpoolDataService {
    private final Logger log = LoggerFactory.getLogger(AndroidWhirlpoolDataService.class);
    private APIFactory apiFactory;

    public AndroidWhirlpoolDataService(WhirlpoolWalletConfig config, WhirlpoolWalletService whirlpoolWalletService, APIFactory apiFactory) {
        super(config, whirlpoolWalletService);
        this.apiFactory = apiFactory;
    }

    @Override
    protected List<UnspentResponse.UnspentOutput> fetchUtxos(WhirlpoolAccount whirlpoolAccount, WhirlpoolWallet whirlpoolWallet) throws Exception {
        List<UTXO> utxos = null;
        switch(whirlpoolAccount) {
            case DEPOSIT:
                utxos = apiFactory.getUtxos(false);
                break;
            case PREMIX:
                utxos = apiFactory.getUtxosPreMix(false);
                break;
            case POSTMIX:
                utxos = apiFactory.getUtxosPostMix(false);
                break;
            case BADBANK:
                utxos = apiFactory.getUtxosBadBank(false);
                break;
            default:
                log.error("fetchUtxos: unknown whirlpoolAccount: "+whirlpoolAccount);
                break;
        }
        if (utxos == null) {
            utxos = new ArrayList<>();
        }

        List<UnspentResponse.UnspentOutput> unspentOutputs = StreamSupport.stream(utxos).map(utxo -> toUnspentOutput(utxo)).collect(Collectors.toList());
        if (log.isTraceEnabled()) {
            log.trace("fetchUtxos["+whirlpoolAccount+"]: " + utxos.size()+" utxos");
        }
        return unspentOutputs;
    }

    private UnspentResponse.UnspentOutput toUnspentOutput(UTXO utxo) {
        UnspentResponse.UnspentOutput u = new UnspentResponse.UnspentOutput();
        MyTransactionOutPoint o = utxo.getOutpoints().iterator().next();
        u.addr = o.getAddress();
        u.confirmations = o.getConfirmations();
        u.script = null; // TODO ?
        u.tx_hash = o.getTxHash().toString();
        u.tx_output_n = o.getTxOutputN();
        u.value = o.getValue().value;
        u.xpub = new UnspentResponse.UnspentOutput.Xpub();
        u.xpub.path = utxo.getPath();
        return u;
    }
}
