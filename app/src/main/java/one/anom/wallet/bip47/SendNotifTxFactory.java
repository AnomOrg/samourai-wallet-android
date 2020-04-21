package one.anom.wallet.bip47;

import one.anom.wallet.SamouraiWallet;

import java.math.BigInteger;

public class SendNotifTxFactory	{

    public static final BigInteger _bNotifTxValue = SamouraiWallet.bDust;
    public static final BigInteger _bSWFee = SamouraiWallet.bFee;
//    public static final BigInteger _bSWCeilingFee = BigInteger.valueOf(50000L);

    public static final String SAMOURAI_NOTIF_TX_FEE_ADDRESS = "bc1qncfysagz0072a894kvzyxqwpvj5ckfj5kctmtk";
    public static final String TESTNET_SAMOURAI_NOTIF_TX_FEE_ADDRESS = "tb1qh287jqsh6mkpqmd8euumyfam00fkr78qhrdnde";

//    public static final double _dSWFeeUSD = 0.5;

    private SendNotifTxFactory () { ; }

}
