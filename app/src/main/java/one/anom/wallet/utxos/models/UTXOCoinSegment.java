package one.anom.wallet.utxos.models;


import one.anom.wallet.send.MyTransactionOutPoint;
import one.anom.wallet.send.UTXO;


/**
 * Sections for UTXO lists
 */
public class UTXOCoinSegment extends UTXOCoin {

    //for UTXOActivity
    public boolean isActive = false;

    //for whirlpool utxo list
    public boolean unCycled = false;

    public UTXOCoinSegment(MyTransactionOutPoint outPoint, UTXO utxo) {
        super(outPoint, utxo);
    }
}