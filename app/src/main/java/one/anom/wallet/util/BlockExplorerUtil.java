package one.anom.wallet.util;

import one.anom.wallet.AnomWallet;

public class BlockExplorerUtil {

    private static CharSequence[] blockExplorers = { "Smartbit", "Blockchain Reader (Yogh)", "BlockCypher", "OXT" };
    private static CharSequence[] blockExplorerTxUrls = { "https://www.smartbit.com.au/tx/", "http://srv1.yogh.io/#tx:id:", "https://live.blockcypher.com/btc/tx/", "https://m.oxt.me/transaction/" };
    private static CharSequence[] blockExplorerAddressUrls = { "https://www.smartbit.com.au/address/", "http://srv1.yogh.io/#addr:id:", "https://live.blockcypher.com/btc/address/", "https://live.blockcypher.com/btc/address/" };

    private static CharSequence[] tBlockExplorers = { "Smartbit", "BlockCypher" };
    private static CharSequence[] tBlockExplorerTxUrls = { "https://testnet.smartbit.com.au/tx/", "https://live.blockcypher.com/btc-testnet/tx/" };
    private static CharSequence[] tBlockExplorerAddressUrls = { "https://testnet.smartbit.com.au/address/", "https://live.blockcypher.com/btc-testnet/address/" };

    private static BlockExplorerUtil instance = null;

    private BlockExplorerUtil() { ; }

    public static BlockExplorerUtil getInstance() {

        if(instance == null) {
            instance = new BlockExplorerUtil();
        }

        return instance;
    }

    public CharSequence[] getBlockExplorers() {

        if(AnomWallet.getInstance().isTestNet())    {
            return tBlockExplorers;
        }
        else    {
            return blockExplorers;
        }

    }

    public CharSequence[] getBlockExplorerTxUrls() {

        if(AnomWallet.getInstance().isTestNet())    {
            return tBlockExplorerTxUrls;
        }
        else    {
            return blockExplorerTxUrls;
        }

    }

    public CharSequence[] getBlockExplorerAddressUrls() {

        if(AnomWallet.getInstance().isTestNet())    {
            return tBlockExplorerAddressUrls;
        }
        else    {
            return blockExplorerAddressUrls;
        }

    }

}
