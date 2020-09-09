package one.anom.wallet.util;

import android.content.Context;
import android.widget.Toast;

import org.apache.commons.lang3.tuple.Pair;
import org.bitcoinj.crypto.MnemonicException;
import one.anom.wallet.AnomWallet;

import com.samourai.wallet.hd.HD_Address;
import com.samourai.wallet.hd.HD_Wallet;
import one.anom.wallet.hd.HD_WalletFactory;
import one.anom.wallet.segwit.BIP49Util;
import one.anom.wallet.segwit.BIP84Util;
import com.samourai.wallet.segwit.SegwitAddress;

import java.io.IOException;
import java.util.HashMap;

public class AddressFactory {

    public static final int LOOKAHEAD_GAP = 20;

    public static final int RECEIVE_CHAIN = 0;
    public static final int CHANGE_CHAIN = 1;

    private static Context context = null;
    private static AddressFactory instance = null;

    private static HashMap<Integer,Integer> highestTxReceiveIdx = null;
    private static HashMap<Integer,Integer> highestTxChangeIdx = null;

    private static int highestBIP49ReceiveIdx = 0;
    private static int highestBIP49ChangeIdx = 0;
    private static int highestBIP84ReceiveIdx = 0;
    private static int highestBIP84ChangeIdx = 0;
    private static int highestPreReceiveIdx = 0;
    private static int highestPreChangeIdx = 0;
    private static int highestPostReceiveIdx = 0;
    private static int highestPostChangeIdx = 0;
    private static int highestBadBankReceiveIdx = 0;
    private static int highestBadBankChangeIdx = 0;

    private static HashMap<String,Integer> xpub2account = null;
    private static HashMap<Integer,String> account2xpub = null;

    private AddressFactory() { ; }

    public static AddressFactory getInstance(Context ctx) {

        context = ctx;

        if(instance == null) {
            instance = new AddressFactory();

            highestTxReceiveIdx = new HashMap<Integer,Integer>();
            highestTxChangeIdx = new HashMap<Integer,Integer>();
            xpub2account = new HashMap<String,Integer>();
            account2xpub = new HashMap<Integer,String>();
        }

        return instance;
    }

    public static AddressFactory getInstance() {

        if(instance == null) {
            instance = new AddressFactory();

            highestTxReceiveIdx = new HashMap<Integer,Integer>();
            highestTxChangeIdx = new HashMap<Integer,Integer>();
            xpub2account = new HashMap<String,Integer>();
            account2xpub = new HashMap<Integer,String>();
        }

        return instance;
    }

    public Pair<Integer, HD_Address> getReceive()	{

        int idx = 0;
        HD_Address addr = null;

        try	{
            HD_Wallet hdw = HD_WalletFactory.getInstance(context).get();

            if(hdw != null)    {
                idx = HD_WalletFactory.getInstance(context).get().getAccount(AnomWallet.SAMOURAI_ACCOUNT).getChain(0).getAddrIdx();
                addr = HD_WalletFactory.getInstance(context).get().getAccount(AnomWallet.SAMOURAI_ACCOUNT).getChain(0).getAddressAt(idx);
                if(canIncReceiveAddress(AnomWallet.SAMOURAI_ACCOUNT))	{
                    HD_WalletFactory.getInstance(context).get().getAccount(AnomWallet.SAMOURAI_ACCOUNT).getChain(0).incAddrIdx();
//                    PayloadUtil.getInstance(context).saveWalletToJSON(new CharSequenceX(AccessFactory.getInstance(context).getGUID() + AccessFactory.getInstance(context).getPIN()));
                }
            }
        }
        catch(IOException ioe)	{
            ioe.printStackTrace();
            Toast.makeText(context, "HD wallet error", Toast.LENGTH_SHORT).show();
        }
        catch(MnemonicException.MnemonicLengthException mle)	{
            mle.printStackTrace();
            Toast.makeText(context, "HD wallet error", Toast.LENGTH_SHORT).show();
        }

        return Pair.of(idx, addr);

    }

    public Pair<Integer, SegwitAddress> getBIP49Receive()	{

        int idx = 0;
        HD_Address addr = null;
        SegwitAddress p2shp2wpkh = null;

//        try	{
            HD_Wallet hdw = BIP49Util.getInstance(context).getWallet();

            if(hdw != null)    {
                idx = BIP49Util.getInstance(context).getWallet().getAccount(AnomWallet.SAMOURAI_ACCOUNT).getChain(0).getAddrIdx();
                addr = BIP49Util.getInstance(context).getWallet().getAccount(AnomWallet.SAMOURAI_ACCOUNT).getChain(0).getAddressAt(idx);
                p2shp2wpkh = new SegwitAddress(addr.getPubKey(), AnomWallet.getInstance().getCurrentNetworkParams());
                if(canIncBIP49ReceiveAddress(idx))	{
                    BIP49Util.getInstance(context).getWallet().getAccount(AnomWallet.SAMOURAI_ACCOUNT).getChain(0).incAddrIdx();
//                    PayloadUtil.getInstance(context).saveWalletToJSON(new CharSequenceX(AccessFactory.getInstance(context).getGUID() + AccessFactory.getInstance(context).getPIN()));
                }
            }
//        }

        return Pair.of(idx, p2shp2wpkh);

    }

    public Pair<Integer, SegwitAddress> getBIP84Receive()	{

        int idx = 0;
        HD_Address addr = null;
        SegwitAddress p2wpkh = null;

//        try	{
        HD_Wallet hdw = BIP84Util.getInstance(context).getWallet();

        if(hdw != null)    {
            idx = BIP84Util.getInstance(context).getWallet().getAccount(AnomWallet.SAMOURAI_ACCOUNT).getChain(0).getAddrIdx();
            addr = BIP84Util.getInstance(context).getWallet().getAccount(AnomWallet.SAMOURAI_ACCOUNT).getChain(0).getAddressAt(idx);
            p2wpkh = new SegwitAddress(addr.getPubKey(), AnomWallet.getInstance().getCurrentNetworkParams());
            if(canIncBIP84ReceiveAddress(idx))	{
                BIP84Util.getInstance(context).getWallet().getAccount(AnomWallet.SAMOURAI_ACCOUNT).getChain(0).incAddrIdx();
//                    PayloadUtil.getInstance(context).saveWalletToJSON(new CharSequenceX(AccessFactory.getInstance(context).getGUID() + AccessFactory.getInstance(context).getPIN()));
            }
        }

        return Pair.of(idx, p2wpkh);

    }

    public HD_Address get(int accountIdx, int chain, int idx)	{

        HD_Address addr = null;

        try	{
            addr = HD_WalletFactory.getInstance(context).get().getAccount(accountIdx).getChain(chain).getAddressAt(idx);
        }
        catch(IOException ioe)	{
            ioe.printStackTrace();
            Toast.makeText(context, "HD wallet error", Toast.LENGTH_SHORT).show();
        }
        catch(MnemonicException.MnemonicLengthException mle)	{
            mle.printStackTrace();
            Toast.makeText(context, "HD wallet error", Toast.LENGTH_SHORT).show();
        }

        return addr;
    }

    public int getHighestTxReceiveIdx(int account)  {
        if(highestTxReceiveIdx.get(account) != null)  {
           return highestTxReceiveIdx.get(account);
        }
        else  {
            return (highestTxReceiveIdx.size() > 0) ? highestTxReceiveIdx.get(0) : 0;
        }
    }

    public void setHighestTxReceiveIdx(int account, int idx) {
 //       Log.i("AddressFactory", "setting highestTxReceiveIdx to " + idx);
        highestTxReceiveIdx.put(account, idx);
    }

    public int getHighestTxChangeIdx(int account) {
        if(highestTxChangeIdx.get(account) != null)  {
            return highestTxChangeIdx.get(account);
        }
        else  {
            return (highestTxChangeIdx.size() > 0) ? highestTxChangeIdx.get(0) : 0;
        }
    }

    public void setHighestTxChangeIdx(int account, int idx) {
 //       Log.i("AddressFactory", "setting highestTxChangeIdx to " + idx);
        highestTxChangeIdx.put(account, idx);
    }

    public int getHighestBIP49ReceiveIdx()  {
        return highestBIP49ReceiveIdx;
    }

    public void setHighestBIP49ReceiveIdx(int idx) {
        highestBIP49ReceiveIdx = idx;
    }

    public int getHighestBIP49ChangeIdx() {
        return highestBIP49ChangeIdx;
    }

    public void setHighestBIP49ChangeIdx(int idx) {
        highestBIP49ChangeIdx = idx;
    }

    public int getHighestBIP84ReceiveIdx()  {
        return highestBIP84ReceiveIdx;
    }

    public void setHighestBIP84ReceiveIdx(int idx) {
        highestBIP84ReceiveIdx = idx;
    }

    public int getHighestBIP84ChangeIdx() {
        return highestBIP84ChangeIdx;
    }

    public void setHighestBIP84ChangeIdx(int idx) {
        highestBIP84ChangeIdx = idx;
    }

    public int getHighestPreReceiveIdx() {
        return highestPreReceiveIdx;
    }

    public void setHighestPreReceiveIdx(int idx) {
        highestPreReceiveIdx = idx;
    }

    public int getHighestPreChangeIdx() {
        return highestPreChangeIdx;
    }

    public void setHighestPreChangeIdx(int idx) {
        highestPreChangeIdx = idx;
    }

    public int getHighestPostReceiveIdx() {
        return highestPostReceiveIdx;
    }

    public void setHighestPostReceiveIdx(int idx) {
        highestPostReceiveIdx = idx;
    }

    public int getHighestPostChangeIdx() {
        return highestPostChangeIdx;
    }

    public void setHighestPostChangeIdx(int idx) {
        highestPostChangeIdx = idx;
    }

    public int getHighestBadBankReceiveIdx() {
        return highestBadBankReceiveIdx;
    }

    public void setHighestBadBankReceiveIdx(int idx) {
        highestBadBankReceiveIdx = idx;
    }

    public int getHighestBadBankChangeIdx() {
        return highestBadBankChangeIdx;
    }

    public void setHighestBadBankChangeIdx(int idx) {
        highestBadBankChangeIdx = idx;
    }

    public boolean canIncReceiveAddress(int account, int idx) {
        if(highestTxReceiveIdx.get(account) != null) {
            return ((idx - highestTxReceiveIdx.get(account)) < (LOOKAHEAD_GAP - 1));
        }
        else {
            return ((idx - highestTxReceiveIdx.get(0)) < (LOOKAHEAD_GAP - 1));
        }
    }

    public boolean canIncReceiveAddress(int account) {
        try {
            return canIncReceiveAddress(account, HD_WalletFactory.getInstance(context).get().getAccount(account).getReceive().getAddrIdx());
        } catch (Exception e) {
            return false;
        }
    }

    public boolean canIncBIP49ReceiveAddress(int idx) {
        return ((idx - highestBIP49ReceiveIdx) < (LOOKAHEAD_GAP - 1));
    }

    public boolean canIncBIP84ReceiveAddress(int idx) {
        return ((idx - highestBIP84ReceiveIdx) < (LOOKAHEAD_GAP - 1));
    }

    public HashMap<String,Integer> xpub2account()   {
        return xpub2account;
    }

    public HashMap<Integer,String> account2xpub()   {
        return account2xpub;
    }

}