package one.anom.wallet.whirlpool;

import one.anom.wallet.SamouraiWallet;
import one.anom.wallet.send.FeeUtil;
import one.anom.wallet.send.MyTransactionOutPoint;
import one.anom.wallet.util.LogUtil;
import one.anom.wallet.segwit.bech32.Bech32Util;
import one.anom.wallet.utxos.models.UTXOCoin;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.params.TestNet3Params;
import org.bitcoinj.script.Script;
import org.bouncycastle.util.encoders.Hex;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class WhirlpoolTx0 {

    private long pool = 0L;
    private List<MyTransactionOutPoint> outpoints = null;
    private long feeSatB = 0L;
    private int premixRequested = 0;
    private Transaction tx0 = null;

    private WhirlpoolTx0()  { ; }

    public WhirlpoolTx0(long pool, List<MyTransactionOutPoint> outpoints, long feeSatB, int premixRequested)   {
        this.pool = pool;
        this.outpoints = outpoints;
        this.feeSatB = feeSatB;
        this.premixRequested = premixRequested;
    }

    public WhirlpoolTx0(long pool, long feeSatB, int premixRequested, List<UTXOCoin> coins)   {
        this.pool = pool;
        this.feeSatB = feeSatB;
        this.premixRequested = premixRequested;
        this.outpoints = new ArrayList<>();
        for(UTXOCoin coin : coins)   {
            outpoints.add(coin.getOutPoint());
        }
    }

    public long getPool() {
        return pool;
    }

    public void setPool(long pool)   {
        this.pool = pool;
    }

    public List<MyTransactionOutPoint> getOutpoints() {
        return outpoints;
    }

    public long getFeeSatB() {
        return feeSatB;
    }

    public void setFeeSatB(long feeSatB) {
        this.feeSatB = feeSatB;
    }

    public int getPremixRequested() {
        if(nbPossiblePremix() < premixRequested || premixRequested == 0)    {
            return nbPossiblePremix();
        }
        else {
            return premixRequested;
        }
    }

    public long getFeeSamourai()    {
        return (long)(getPool() * WhirlpoolMeta.WHIRLPOOL_FEE_RATE_POOL_DENOMINATION);
    }

    public long getEstimatedBytes()    {

        int nbP2PKH = 0;
        int nbP2SH = 0;
        int nbP2WPKH = 0;

        for(MyTransactionOutPoint outPoint : outpoints)    {
            if(Bech32Util.getInstance().isP2WPKHScript(Hex.toHexString(outPoint.getScriptBytes())))    {
                nbP2WPKH++;
            }
            else    {
                String address = new Script(outPoint.getScriptBytes()).getToAddress(SamouraiWallet.getInstance().getCurrentNetworkParams()).toString();
                if(Address.fromBase58(SamouraiWallet.getInstance().getCurrentNetworkParams(), address).isP2SHAddress())    {
                    nbP2SH++;
                }
                else    {
                    nbP2PKH++;
                }
            }

        }

        return FeeUtil.getInstance().estimatedSizeSegwit(nbP2PKH, nbP2SH, nbP2WPKH) + 80;
    }

    public long getChange() {
        return getAmountSelected() - ((getPremixRequested() * getPremixAmount()) + getFeeSamourai() + getFee());
    }

    public long getFee() {
        return getFeeSatB() * getEstimatedBytes();
    }

    public long getAmountAfterWhirlpoolFee() {
        return getAmountSelected() - getFeeSamourai();
    }

    public long getPremixAmount()   {
        return getPool() + (getFeeSatB() * 102L);
    }

    public long getAmountSelected() {

        long ret = 0L;

        for(MyTransactionOutPoint outpoint : outpoints)   {
            ret += outpoint.getValue().value;
        }

        return ret;
    }

    public int nbPossiblePremix()   {

        int ret = (int)((getAmountSelected() - (long)(getFeeSamourai() + getFee())) / getPool());

        return ret > 0 ? ret : 0;

    }

    //
    // return signed tx0 for params passed to constructor
    //
    public Transaction getTx0() {
        return tx0;
    }

    public void make()  throws Exception {

        tx0 = null;

        LogUtil.debug("WhirlpoolTx0", "make: ");
        //
        // calc fee here using feeSatB and utxos passed
        //
        if(getChange() < 0L)    {
            LogUtil.debug("WhirlpoolTx0", "Cannot make premix: negative change:" + getAmountSelected());
            throw  new Exception("Cannot make premix: negative change:"+getAmountSelected());
        }
        if(nbPossiblePremix() < 1)    {
            LogUtil.debug("WhirlpoolTx0", "Cannot make premix: insufficient selected amount:" + getAmountSelected());
            throw  new Exception("Cannot make premix: insufficient selected amount:"+getAmountSelected());
        }

        LogUtil.debug("WhirlpoolTx0", "amount selected:" + getAmountSelected() / 1e8);
        LogUtil.debug("WhirlpoolTx0", "amount requested:" + ((getPremixRequested() * getPool())  / 1e8));
        LogUtil.debug("WhirlpoolTx0", "nb premix possible:" + nbPossiblePremix());
        LogUtil.debug("WhirlpoolTx0", "amount after Whirlpool fee:" + getAmountAfterWhirlpoolFee() / 1e8);
        LogUtil.debug("WhirlpoolTx0", "fee samourai:" + new DecimalFormat("0.########").format(getFeeSamourai() / 1e8));
        LogUtil.debug("WhirlpoolTx0", "fee miners:" + new DecimalFormat("0.########").format(getFee() / 1e8));
        LogUtil.debug("WhirlpoolTx0", "change amount:" + getChange() / 1e8);

        // [WIP] stub;
        tx0 = new Transaction(SamouraiWallet.getInstance().getCurrentNetworkParams() instanceof TestNet3Params ? TestNet3Params.get() : MainNetParams.get());

    }

}