package one.anom.wallet.cahoots;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Looper;
import android.support.v4.content.FileProvider;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.samourai.wallet.hd.HD_Address;
import com.samourai.wallet.segwit.SegwitAddress;
import com.samourai.wallet.segwit.bech32.Bech32Segwit;

import one.anom.wallet.R;
import one.anom.wallet.SamouraiWallet;
import one.anom.wallet.api.APIFactory;
import one.anom.wallet.cahoots.psbt.PSBT;
import one.anom.wallet.hd.HD_WalletFactory;
import one.anom.wallet.segwit.BIP84Util;
import one.anom.wallet.segwit.bech32.Bech32Util;
import one.anom.wallet.send.FeeUtil;
import one.anom.wallet.send.MyTransactionOutPoint;
import one.anom.wallet.send.PushTx;
import one.anom.wallet.send.SendFactory;
import one.anom.wallet.send.UTXO;
import one.anom.wallet.util.AddressFactory;
import one.anom.wallet.util.AppUtil;
import one.anom.wallet.util.FormatsUtil;
import one.anom.wallet.whirlpool.WhirlpoolMeta;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionInput;
import org.bitcoinj.core.TransactionOutPoint;
import org.bitcoinj.crypto.MnemonicException;
import org.bitcoinj.script.Script;
import org.bouncycastle.util.encoders.Hex;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import one.anom.wallet.util.LogUtil;
import one.google.zxing.client.android.Contents;
import one.google.zxing.client.android.encode.QRCodeEncoder;

public class CahootsUtil {

    private static Context context = null;

    private static CahootsUtil instance = null;

    private CahootsUtil()    { ; }

    public static CahootsUtil getInstance(Context ctx) {

        context = ctx;

        if(instance == null)    {
            instance = new CahootsUtil();
        }

        return instance;
    }

    public void processCahoots(String strCahoots, int ctpyAccount) {

        Stowaway stowaway = null;
        STONEWALLx2 stonewall = null;

        try {
            JSONObject obj = new JSONObject(strCahoots);
            Log.d("CahootsUtil", "incoming st:" + strCahoots);
            Log.d("CahootsUtil", "object json:" + obj.toString());
            if (obj.has("cahoots") && obj.getJSONObject("cahoots").has("type")) {

                int type = obj.getJSONObject("cahoots").getInt("type");
                switch (type) {
                    case Cahoots.CAHOOTS_STOWAWAY:
                        stowaway = new Stowaway(obj);
                        Log.d("CahootsUtil", "stowaway st:" + stowaway.toJSON().toString());
                        break;
                    case Cahoots.CAHOOTS_STONEWALLx2:
                        stonewall = new STONEWALLx2(obj);
                        Log.d("CahootsUtil", "stonewall st:" + stonewall.toJSON().toString());
                        break;
                    default:
                        Toast.makeText(context, R.string.unrecognized_cahoots, Toast.LENGTH_SHORT).show();
                        return;
                }

            } else {
                Toast.makeText(context, R.string.not_cahoots, Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (JSONException je) {
            Toast.makeText(context, R.string.cannot_process_cahoots, Toast.LENGTH_SHORT).show();
            return;
        }

        if (stowaway != null) {

            int step = stowaway.getStep();

            try {
                switch (step) {
                    case 0:
                        Log.d("CahootsUtil", "calling doStowaway1");
                        doStowaway1(stowaway);
                        break;
                    case 1:
                        doStowaway2(stowaway);
                        break;
                    case 2:
                        doStowaway3(stowaway);
                        break;
                    case 3:
                        doStowaway4(stowaway);
                        break;
                    default:
                        Toast.makeText(context, R.string.unrecognized_step, Toast.LENGTH_SHORT).show();
                        break;
                }
            } catch (Exception e) {
                Toast.makeText(context, R.string.cannot_process_stonewall, Toast.LENGTH_SHORT).show();
                Log.d("CahootsUtil", e.getMessage());
                e.printStackTrace();
            }

            return;

        } else if (stonewall != null) {

            int step = stonewall.getStep();

            try {
                switch (step) {
                    case 0:
                        stonewall.setCounterpartyAccount(ctpyAccount);  // set counterparty account
                        doSTONEWALLx2_1(stonewall);
                        break;
                    case 1:
                        doSTONEWALLx2_2(stonewall);
                        break;
                    case 2:
                        doSTONEWALLx2_3(stonewall);
                        break;
                    case 3:
                        doSTONEWALLx2_4(stonewall);
                        break;
                    default:
                        Toast.makeText(context, R.string.unrecognized_step, Toast.LENGTH_SHORT).show();
                        break;
                }
            } catch (Exception e) {
                Toast.makeText(context, R.string.cannot_process_stowaway, Toast.LENGTH_SHORT).show();
                e.printStackTrace();
                Log.d("CahootsUtil", e.getMessage());
            }

            return;

        } else {
            Toast.makeText(context, "error processing #Cahoots", Toast.LENGTH_SHORT).show();
        }

    }

    private void doCahoots(final String strCahoots) {

        Cahoots cahoots = null;
        Transaction transaction = null;
        int step = 0;
        try {
            JSONObject jsonObject = new JSONObject(strCahoots);
            if (jsonObject != null && jsonObject.has("cahoots") && jsonObject.getJSONObject("cahoots").has("step")) {
                step = jsonObject.getJSONObject("cahoots").getInt("step");
                if (step == 4 || step == 3) {
                    cahoots = new Stowaway(jsonObject);
                    transaction = cahoots.getPSBT().getTransaction();
                }
            }
        } catch (JSONException je) {
            Toast.makeText(context, je.getMessage(), Toast.LENGTH_SHORT).show();
        }

        final int _step = step;
        final Transaction _transaction = transaction;

        final int QR_ALPHANUM_CHAR_LIMIT = 4296;    // tx max size in bytes == 2148

        TextView showTx = new TextView(context);
        showTx.setText(step != 4 ? strCahoots : Hex.toHexString(transaction.bitcoinSerialize()));
        showTx.setTextIsSelectable(true);
        showTx.setPadding(40, 10, 40, 10);
        showTx.setTextSize(18.0f);
        showTx.setLines(10);

        LinearLayout hexLayout = new LinearLayout(context);
        hexLayout.setOrientation(LinearLayout.VERTICAL);
        hexLayout.addView(showTx);

        String title = context.getString(R.string.cahoots);
        title += ", ";
        title += (_step + 1);
        title += "/5";

        AlertDialog.Builder dlg = new AlertDialog.Builder(context)
                .setTitle(title)
                .setView(hexLayout)
                .setCancelable(true)
                .setPositiveButton(R.string.copy_to_clipboard, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        android.content.ClipboardManager clipboard = (android.content.ClipboardManager) context.getSystemService(android.content.Context.CLIPBOARD_SERVICE);
                        android.content.ClipData clip = null;
                        clip = android.content.ClipData.newPlainText("Cahoots", _step != 4 ? strCahoots : Hex.toHexString(_transaction.bitcoinSerialize()));
                        clipboard.setPrimaryClip(clip);
                        Toast.makeText(context, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show();

                    }
                })
                .setNegativeButton(R.string.show_qr, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        if (strCahoots.length() <= QR_ALPHANUM_CHAR_LIMIT) {

                            final ImageView ivQR = new ImageView(context);

                            Display display = ((Activity) context).getWindowManager().getDefaultDisplay();
                            Point size = new Point();
                            display.getSize(size);
                            int imgWidth = Math.max(size.x - 240, 150);

                            Bitmap bitmap = null;

                            QRCodeEncoder qrCodeEncoder = new QRCodeEncoder(strCahoots, null, Contents.Type.TEXT, BarcodeFormat.QR_CODE.toString(), imgWidth);

                            try {
                                bitmap = qrCodeEncoder.encodeAsBitmap();
                            } catch (WriterException e) {
                                e.printStackTrace();
                            }

                            ivQR.setImageBitmap(bitmap);

                            LinearLayout qrLayout = new LinearLayout(context);
                            qrLayout.setOrientation(LinearLayout.VERTICAL);
                            qrLayout.addView(ivQR);

                            new AlertDialog.Builder(context)
                                    .setTitle(R.string.cahoots)
                                    .setView(qrLayout)
                                    .setCancelable(false)
                                    .setPositiveButton(R.string.close, new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int whichButton) {

                                            dialog.dismiss();

                                        }
                                    })
                                    .setNegativeButton(R.string.share_qr, new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int whichButton) {

                                            String strFileName = AppUtil.getInstance(context).getReceiveQRFilename();
                                            File file = new File(strFileName);
                                            if (!file.exists()) {
                                                try {
                                                    file.createNewFile();
                                                } catch (Exception e) {
                                                    Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show();
                                                }
                                            }
                                            file.setReadable(true, false);

                                            FileOutputStream fos = null;
                                            try {
                                                fos = new FileOutputStream(file);
                                            } catch (FileNotFoundException fnfe) {
                                                ;
                                            }

                                            if (file != null && fos != null) {
                                                Bitmap bitmap = ((BitmapDrawable) ivQR.getDrawable()).getBitmap();
                                                bitmap.compress(Bitmap.CompressFormat.PNG, 0, fos);

                                                try {
                                                    fos.close();
                                                } catch (IOException ioe) {
                                                    ;
                                                }

                                                Intent intent = new Intent();
                                                intent.setAction(Intent.ACTION_SEND);
                                                intent.setType("image/png");
                                                if (android.os.Build.VERSION.SDK_INT >= 24) {
                                                    //From API 24 sending FIle on intent ,require custom file provider
                                                    intent.putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(
                                                            context,
                                                            context.getApplicationContext()
                                                                    .getPackageName() + ".provider", file));
                                                } else {
                                                    intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
                                                }
                                                context.startActivity(Intent.createChooser(intent, context.getText(R.string.send_tx)));
                                            }

                                        }
                                    }).show();
                        } else {

                            Toast.makeText(context, R.string.tx_too_large_qr, Toast.LENGTH_SHORT).show();

                        }

                    }
                });

        if (_step == 4) {
            dlg.setPositiveButton(R.string.broadcast, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {

                    dialog.dismiss();

                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            Looper.prepare();

                            PushTx.getInstance(context).pushTx(Hex.toHexString(_transaction.bitcoinSerialize()));

                            Looper.loop();

                        }
                    }).start();

                }
            });
            dlg.setNeutralButton(R.string.show_tx, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {

                    String tx = "";
                    if (_transaction != null) {
                        tx = _transaction.toString();
                    }

                    TextView showText = new TextView(context);
                    showText.setText(tx);
                    showText.setTextIsSelectable(true);
                    showText.setPadding(40, 10, 40, 10);
                    showText.setTextSize(18.0f);
                    showText.setLines(10);
                    new AlertDialog.Builder(context)
                            .setTitle(R.string.app_name)
                            .setView(showText)
                            .setCancelable(false)
                            .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    dialog.dismiss();
                                }
                            }).show();
                }
            });
        } else if (_step == 3) {
            dlg.setNeutralButton(R.string.show_tx, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {

                    String tx = "";
                    if (_transaction != null) {
                        tx = _transaction.toString();
                    }

                    TextView showText = new TextView(context);
                    showText.setText(tx);
                    showText.setTextIsSelectable(true);
                    showText.setPadding(40, 10, 40, 10);
                    showText.setTextSize(18.0f);
                    showText.setLines(10);
                    new AlertDialog.Builder(context)
                            .setTitle(R.string.app_name)
                            .setView(showText)
                            .setCancelable(false)
                            .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    dialog.dismiss();
                                }
                            }).show();
                }
            });
        } else {
            ;
        }

        if (!((Activity) context).isFinishing()) {
            dlg.show();
        }

    }

    public void doPSBT(final String strPSBT)    {

        String msg = null;
        PSBT psbt = new PSBT(strPSBT, SamouraiWallet.getInstance().getCurrentNetworkParams());
        try {
            psbt.read();
            msg = psbt.dump();
        }
        catch(Exception e) {
            msg = e.getMessage();
        }

        final EditText edPSBT = new EditText(context);
        edPSBT.setSingleLine(false);
        edPSBT.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        edPSBT.setLines(10);
        edPSBT.setHint(R.string.PSBT);
        edPSBT.setGravity(Gravity.START);
        TextWatcher textWatcher = new TextWatcher() {

            public void afterTextChanged(Editable s) {
                edPSBT.setSelection(0);
            }
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                ;
            }
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                ;
            }
        };
        edPSBT.addTextChangedListener(textWatcher);
        edPSBT.setText(msg);

        AlertDialog.Builder dlg = new AlertDialog.Builder(context)
                .setTitle(R.string.app_name)
                .setMessage(R.string.PSBT)
                .setView(edPSBT)
                .setCancelable(true)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        dialog.dismiss();

                    }

                });
        if(!((Activity)context).isFinishing())    {
            dlg.show();
        }

    }

    //
    // sender
    //
    public Cahoots doStowaway0(long spendAmount, int account) {
        NetworkParameters params = SamouraiWallet.getInstance().getCurrentNetworkParams();

        //
        //
        // step0: B sends spend amount to A,  creates step0
        //
        //
        LogUtil.debug("CahootsUtil", "sender account (0):" + account);
        Stowaway stowaway0 = new Stowaway(spendAmount, params, account);
        try {
            stowaway0.setFingerprint(HD_WalletFactory.getInstance(context).getFingerprint());
        }
        catch(IOException | MnemonicException.MnemonicLengthException e) {
            ;
        }
        return stowaway0;
    }

    //
    // receiver
    //
    public Cahoots doStowaway1(Stowaway stowaway0) throws Exception {

        List<UTXO> utxos = getCahootsUTXO(0);
        // sort in descending order by value
        Collections.sort(utxos, new UTXO.UTXOComparator());

        LogUtil.debug("CahootsUtil", "BIP84 utxos:" + utxos.size());

        List<UTXO> selectedUTXO = new ArrayList<UTXO>();
        long totalContributedAmount = 0L;
        List<UTXO> highUTXO = new ArrayList<UTXO>();
        for (UTXO utxo : utxos) {
            if (utxo.getValue() > stowaway0.getSpendAmount() + SamouraiWallet.bDust.longValue()) {
                highUTXO.add(utxo);
            }
        }
        if(highUTXO.size() > 0)    {
            SecureRandom random = new SecureRandom();
            UTXO utxo = highUTXO.get(random.nextInt(highUTXO.size()));
            LogUtil.debug("CahootsUtil", "BIP84 selected random utxo:" + utxo.getValue());
            selectedUTXO.add(utxo);
            totalContributedAmount = utxo.getValue();
        }
        if (selectedUTXO.size() == 0) {
            for (UTXO utxo : utxos) {
                selectedUTXO.add(utxo);
                totalContributedAmount += utxo.getValue();
                LogUtil.debug("CahootsUtil", "BIP84 selected utxo:" + utxo.getValue());
                if (totalContributedAmount > stowaway0.getSpendAmount() + SamouraiWallet.bDust.longValue()) {
                    break;
                }
            }
        }

        if (!(totalContributedAmount > stowaway0.getSpendAmount() + SamouraiWallet.bDust.longValue())) {
            return null;
        }

        LogUtil.debug("CahootsUtil", "BIP84 selected utxos:" + selectedUTXO.size());

        NetworkParameters params = stowaway0.getParams();

        //
        //
        // step1: A utxos -> B (take largest that cover amount)
        //
        //

        String zpub = BIP84Util.getInstance(context).getWallet().getAccount(0).zpubstr();
        HashMap<_TransactionOutPoint, Triple<byte[], byte[], String>> inputsA = new HashMap<_TransactionOutPoint, Triple<byte[], byte[], String>>();
//        inputsA.put(outpoint_A0, Triple.of(Hex.decode("0221b719bc26fb49971c7dd328a6c7e4d17dfbf4e2217bee33a65c53ed3daf041e"), FormatsUtil.getInstance().getFingerprintFromXPUB("vpub5Z3hqXewnCbxnMsWygxN8AyjNVJbFeV2VCdDKpTFazdoC29qK4Y5DSQ1aaAPBrsBZ1TzN5va6xGy4eWa9uAqh2AwifuA1eofkedh3eUgF6b"), "M/0/4"));
//        inputsA.put(outpoint_A1, Triple.of(Hex.decode("020ab261e1a3cf986ecb3cd02299de36295e804fd799934dc5c99dde0d25e71b93"), FormatsUtil.getInstance().getFingerprintFromXPUB("vpub5Z3hqXewnCbxnMsWygxN8AyjNVJbFeV2VCdDKpTFazdoC29qK4Y5DSQ1aaAPBrsBZ1TzN5va6xGy4eWa9uAqh2AwifuA1eofkedh3eUgF6b"), "M/0/2"));

        for (UTXO utxo : selectedUTXO) {
            for (MyTransactionOutPoint outpoint : utxo.getOutpoints()) {
                _TransactionOutPoint _outpoint = new _TransactionOutPoint(outpoint);

                ECKey eckey = SendFactory.getPrivKey(_outpoint.getAddress(), 0);
                String path = APIFactory.getInstance(context).getUnspentPaths().get(_outpoint.getAddress());
                inputsA.put(_outpoint, Triple.of(eckey.getPubKey(), stowaway0.getFingerprintCollab(), path));
            }
        }

        // destination output
        int idx = BIP84Util.getInstance(context).getWallet().getAccount(0).getReceive().getAddrIdx();
        SegwitAddress segwitAddress = BIP84Util.getInstance(context).getAddressAt(0, idx);
        HashMap<_TransactionOutput, Triple<byte[], byte[], String>> outputsA = new HashMap<_TransactionOutput, Triple<byte[], byte[], String>>();
//        byte[] scriptPubKey_A = getScriptPubKey("tb1qewwlc2dksuez3zauf38d82m7uqd4ewkf2avdl8", params);
        Pair<Byte, byte[]> pair = Bech32Segwit.decode(SamouraiWallet.getInstance().isTestNet() ? "tb" : "bc", segwitAddress.getBech32AsString());
        byte[] scriptPubKey_A = Bech32Segwit.getScriptPubkey(pair.getLeft(), pair.getRight());
        _TransactionOutput output_A0 = new _TransactionOutput(params, null, Coin.valueOf(stowaway0.getSpendAmount()), scriptPubKey_A);
        outputsA.put(output_A0, Triple.of(segwitAddress.getECKey().getPubKey(), stowaway0.getFingerprintCollab(), "M/0/" + idx));

        stowaway0.setDestination(segwitAddress.getBech32AsString());

        Stowaway stowaway1 = new Stowaway(stowaway0);
        stowaway1.inc(inputsA, outputsA, null);

        return stowaway1;
    }

    //
    // sender
    //
    public Cahoots doStowaway2(Stowaway stowaway1) throws Exception {

        LogUtil.debug("CahootsUtil", "sender account (2):" + stowaway1.getAccount());

        Transaction transaction = stowaway1.getTransaction();
        LogUtil.debug("CahootsUtil", "step2 tx:" + org.spongycastle.util.encoders.Hex.toHexString(transaction.bitcoinSerialize()));
        int nbIncomingInputs = transaction.getInputs().size();

        List<UTXO> utxos = getCahootsUTXO(stowaway1.getAccount());
        // sort in ascending order by value
        Collections.sort(utxos, new UTXO.UTXOComparator());
        Collections.reverse(utxos);

        LogUtil.debug("CahootsUtil", "BIP84 utxos:" + utxos.size());

        List<UTXO> selectedUTXO = new ArrayList<UTXO>();
        int nbTotalSelectedOutPoints = 0;
        long totalSelectedAmount = 0L;
        List<UTXO> lowUTXO = new ArrayList<UTXO>();
        for (UTXO utxo : utxos) {
            if(utxo.getValue() < stowaway1.getSpendAmount())    {
                lowUTXO.add(utxo);
            }
        }

        List<List<UTXO>> listOfLists = new ArrayList<List<UTXO>>();
        Collections.shuffle(lowUTXO);
        listOfLists.add(lowUTXO);
        listOfLists.add(utxos);
        for(List<UTXO> list : listOfLists)   {

            selectedUTXO.clear();
            totalSelectedAmount = 0L;
            nbTotalSelectedOutPoints = 0;

            for (UTXO utxo : list) {
                selectedUTXO.add(utxo);
                totalSelectedAmount += utxo.getValue();
                LogUtil.debug("BIP84 selected utxo:", "" + utxo.getValue());
                nbTotalSelectedOutPoints += utxo.getOutpoints().size();
                if (totalSelectedAmount > FeeUtil.getInstance().estimatedFeeSegwit(0, 0, nbTotalSelectedOutPoints + nbIncomingInputs, 2).longValue() + stowaway1.getSpendAmount() + SamouraiWallet.bDust.longValue()) {

                    // discard "extra" utxo, if any
                    List<UTXO> _selectedUTXO = new ArrayList<UTXO>();
                    Collections.reverse(selectedUTXO);
                    int _nbTotalSelectedOutPoints = 0;
                    long _totalSelectedAmount = 0L;
                    for (UTXO utxoSel : selectedUTXO) {
                        _selectedUTXO.add(utxoSel);
                        _totalSelectedAmount += utxoSel.getValue();
                        LogUtil.debug("CahootsUtil", "BIP84 post selected utxo:" + utxoSel.getValue());
                        _nbTotalSelectedOutPoints += utxoSel.getOutpoints().size();
                        if (_totalSelectedAmount > FeeUtil.getInstance().estimatedFeeSegwit(0, 0, _nbTotalSelectedOutPoints + nbIncomingInputs, 2).longValue() + stowaway1.getSpendAmount() + SamouraiWallet.bDust.longValue()) {
                            selectedUTXO.clear();
                            selectedUTXO.addAll(_selectedUTXO);
                            totalSelectedAmount = _totalSelectedAmount;
                            nbTotalSelectedOutPoints = _nbTotalSelectedOutPoints;
                            break;
                        }
                    }

                    break;
                }
            }
            if (totalSelectedAmount > FeeUtil.getInstance().estimatedFeeSegwit(0, 0, nbTotalSelectedOutPoints + nbIncomingInputs, 2).longValue() + stowaway1.getSpendAmount() + SamouraiWallet.bDust.longValue()) {
                break;
            }
        }

        /*
        if(lowUTXO.size() > 0)    {
            Collections.shuffle(lowUTXO);
            for (UTXO utxo : lowUTXO) {
                selectedUTXO.add(utxo);
                totalSelectedAmount += utxo.getValue();
                debug("BIP84 selected utxo:", "" + utxo.getValue());
                nbTotalSelectedOutPoints += utxo.getOutpoints().size();
                if (totalSelectedAmount > FeeUtil.getInstance().estimatedFeeSegwit(0, 0, nbTotalSelectedOutPoints + nbIncomingInputs, 2).longValue() + stowaway1.getSpendAmount() + SamouraiWallet.bDust.longValue()) {

                    // discard "extra" utxo, if any
                    List<UTXO> _selectedUTXO = new ArrayList<UTXO>();
                    Collections.reverse(selectedUTXO);
                    int _nbTotalSelectedOutPoints = 0;
                    long _totalSelectedAmount = 0L;
                    for (UTXO utxoSel : selectedUTXO) {
                        _selectedUTXO.add(utxoSel);
                        _totalSelectedAmount += utxoSel.getValue();
                        debug("CahootsUtil", "BIP84 post selected utxo:" + utxoSel.getValue());
                        _nbTotalSelectedOutPoints += utxoSel.getOutpoints().size();
                        if (_totalSelectedAmount > FeeUtil.getInstance().estimatedFeeSegwit(0, 0, _nbTotalSelectedOutPoints + nbIncomingInputs, 2).longValue() + stowaway1.getSpendAmount() + SamouraiWallet.bDust.longValue()) {
                            selectedUTXO.clear();
                            selectedUTXO.addAll(_selectedUTXO);
                            totalSelectedAmount = _totalSelectedAmount;
                            nbTotalSelectedOutPoints = _nbTotalSelectedOutPoints;
                            break;
                        }
                    }

                    break;
                }
            }

        }
        if (!(totalSelectedAmount > FeeUtil.getInstance().estimatedFeeSegwit(0, 0, nbTotalSelectedOutPoints + nbIncomingInputs, 2).longValue() + stowaway1.getSpendAmount() + SamouraiWallet.bDust.longValue())) {
            selectedUTXO.clear();
            totalSelectedAmount = 0L;
            nbTotalSelectedOutPoints = 0;
            for (UTXO utxo : utxos) {
                selectedUTXO.add(utxo);
                totalSelectedAmount += utxo.getValue();
                debug("BIP84 selected utxo:", "" + utxo.getValue());
                nbTotalSelectedOutPoints += utxo.getOutpoints().size();
                if (totalSelectedAmount > FeeUtil.getInstance().estimatedFeeSegwit(0, 0, nbTotalSelectedOutPoints + nbIncomingInputs, 2).longValue() + stowaway1.getSpendAmount() + SamouraiWallet.bDust.longValue()) {

                    // discard "extra" utxo, if any
                    List<UTXO> _selectedUTXO = new ArrayList<UTXO>();
                    Collections.reverse(selectedUTXO);
                    int _nbTotalSelectedOutPoints = 0;
                    long _totalSelectedAmount = 0L;
                    for (UTXO utxoSel : selectedUTXO) {
                        _selectedUTXO.add(utxoSel);
                        _totalSelectedAmount += utxoSel.getValue();
                        debug("CahootsUtil", "BIP84 post selected utxo:" + utxoSel.getValue());
                        _nbTotalSelectedOutPoints += utxoSel.getOutpoints().size();
                        if (_totalSelectedAmount > FeeUtil.getInstance().estimatedFeeSegwit(0, 0, _nbTotalSelectedOutPoints + nbIncomingInputs, 2).longValue() + stowaway1.getSpendAmount() + SamouraiWallet.bDust.longValue()) {
                            selectedUTXO.clear();
                            selectedUTXO.addAll(_selectedUTXO);
                            totalSelectedAmount = _totalSelectedAmount;
                            nbTotalSelectedOutPoints = _nbTotalSelectedOutPoints;
                            break;
                        }
                    }

                    break;
                }
            }
        }
        */
        if (!(totalSelectedAmount > FeeUtil.getInstance().estimatedFeeSegwit(0, 0, nbTotalSelectedOutPoints + nbIncomingInputs, 2).longValue() + stowaway1.getSpendAmount() + SamouraiWallet.bDust.longValue())) {
            return null;
        }

        LogUtil.debug("CahootsUtil", "BIP84 selected utxos:" + selectedUTXO.size());

        long fee = FeeUtil.getInstance().estimatedFeeSegwit(0, 0, nbTotalSelectedOutPoints + nbIncomingInputs, 2).longValue();
        LogUtil.debug("CahootsUtil", "fee:" + fee);

        NetworkParameters params = stowaway1.getParams();

        //
        //
        // step2: B verif, utxos -> A (take smallest that cover amount)
        //
        //

        String zpub = BIP84Util.getInstance(context).getWallet().getAccountAt(stowaway1.getAccount()).zpubstr();
        HashMap<_TransactionOutPoint, Triple<byte[], byte[], String>> inputsB = new HashMap<_TransactionOutPoint, Triple<byte[], byte[], String>>();

        for (UTXO utxo : selectedUTXO) {
            for (MyTransactionOutPoint outpoint : utxo.getOutpoints()) {
                _TransactionOutPoint _outpoint = new _TransactionOutPoint(outpoint);

                ECKey eckey = SendFactory.getPrivKey(_outpoint.getAddress(), stowaway1.getAccount());
                String path = APIFactory.getInstance(context).getUnspentPaths().get(_outpoint.getAddress());
                inputsB.put(_outpoint, Triple.of(eckey.getPubKey(), stowaway1.getFingerprint(), path));
            }
        }

        LogUtil.debug("CahootsUtil", "inputsB:" + inputsB.size());

        // change output
        SegwitAddress segwitAddress = null;
        int idx = 0;
        if (stowaway1.getAccount() == WhirlpoolMeta.getInstance(context).getWhirlpoolPostmix()) {
            idx = AddressFactory.getInstance(context).getHighestPostChangeIdx();
            HD_Address addr = BIP84Util.getInstance(context).getWallet().getAccountAt(stowaway1.getAccount()).getChange().getAddressAt(idx);
            segwitAddress = new SegwitAddress(addr.getPubKey(), SamouraiWallet.getInstance().getCurrentNetworkParams());
        } else {
            idx = BIP84Util.getInstance(context).getWallet().getAccount(0).getChange().getAddrIdx();
            segwitAddress = BIP84Util.getInstance(context).getAddressAt(1, idx);
        }
        HashMap<_TransactionOutput, Triple<byte[], byte[], String>> outputsB = new HashMap<_TransactionOutput, Triple<byte[], byte[], String>>();
        Pair<Byte, byte[]> pair = Bech32Segwit.decode(SamouraiWallet.getInstance().isTestNet() ? "tb" : "bc", segwitAddress.getBech32AsString());
        byte[] scriptPubKey_B = Bech32Segwit.getScriptPubkey(pair.getLeft(), pair.getRight());
        _TransactionOutput output_B0 = new _TransactionOutput(params, null, Coin.valueOf((totalSelectedAmount - stowaway1.getSpendAmount()) - fee), scriptPubKey_B);
        outputsB.put(output_B0, Triple.of(segwitAddress.getECKey().getPubKey(), stowaway1.getFingerprint(), "M/1/" + idx));

        LogUtil.debug("CahootsUtil", "outputsB:" + outputsB.size());

        Stowaway stowaway2 = new Stowaway(stowaway1);
        stowaway2.inc(inputsB, outputsB, null);
        stowaway2.setFeeAmount(fee);

        return stowaway2;

    }

    //
    // receiver
    //
    public Cahoots doStowaway3(Stowaway stowaway2) throws Exception {

        LogUtil.debug("CahootsUtil", "sender account (3):" + stowaway2.getAccount());

        HashMap<String, String> utxo2Address = new HashMap<String, String>();
        List<UTXO> utxos = APIFactory.getInstance(context).getUtxos(true);
        for (UTXO utxo : utxos) {
            for (MyTransactionOutPoint outpoint : utxo.getOutpoints()) {
                utxo2Address.put(outpoint.getTxHash().toString() + "-" + outpoint.getTxOutputN(), outpoint.getAddress());
                LogUtil.debug("CahootsUtil", "outpoint address:" + outpoint.getTxHash().toString() + "-" + outpoint.getTxOutputN() + "," + outpoint.getAddress());
            }
        }

        Transaction transaction = stowaway2.getPSBT().getTransaction();
        HashMap<String, ECKey> keyBag_A = new HashMap<String, ECKey>();
        for (TransactionInput input : transaction.getInputs()) {
            TransactionOutPoint outpoint = input.getOutpoint();
            String key = outpoint.getHash().toString() + "-" + outpoint.getIndex();
            if (utxo2Address.containsKey(key)) {
                String address = utxo2Address.get(key);
                ECKey eckey = SendFactory.getPrivKey(address, 0);
                keyBag_A.put(outpoint.toString(), eckey);
            }
        }

        Stowaway stowaway3 = new Stowaway(stowaway2);
        stowaway3.inc(null, null, keyBag_A);

        return stowaway3;

    }

    //
    // sender
    //
    public Cahoots doStowaway4(Stowaway stowaway3) throws Exception {

        LogUtil.debug("CahootsUtil", "sender account (4):" + stowaway3.getAccount());

        HashMap<String, String> utxo2Address = new HashMap<String, String>();
        List<UTXO> utxos = null;
        if (stowaway3.getAccount() == WhirlpoolMeta.getInstance(context).getWhirlpoolPostmix()) {
            utxos = APIFactory.getInstance(context).getUtxosPostMix(true);
        } else {
            utxos = APIFactory.getInstance(context).getUtxos(true);
        }
        for (UTXO utxo : utxos) {
            for (MyTransactionOutPoint outpoint : utxo.getOutpoints()) {
                utxo2Address.put(outpoint.getTxHash().toString() + "-" + outpoint.getTxOutputN(), outpoint.getAddress());
                LogUtil.debug("CahootsUtil", "outpoint address:" + outpoint.getTxHash().toString() + "-" + outpoint.getTxOutputN() + "," + outpoint.getAddress());
            }
        }

        Transaction transaction = stowaway3.getPSBT().getTransaction();
        HashMap<String, ECKey> keyBag_B = new HashMap<String, ECKey>();
        for (TransactionInput input : transaction.getInputs()) {
            TransactionOutPoint outpoint = input.getOutpoint();
            String key = outpoint.getHash().toString() + "-" + outpoint.getIndex();
            if (utxo2Address.containsKey(key)) {
                String address = utxo2Address.get(key);
                ECKey eckey = SendFactory.getPrivKey(address, stowaway3.getAccount());
                keyBag_B.put(outpoint.toString(), eckey);
            }
        }

        //
        //
        // step4: B verif, sig, broadcast
        //
        //

        Stowaway stowaway4 = new Stowaway(stowaway3);
        stowaway4.inc(null, null, keyBag_B);

        return stowaway4;

    }

    //
    // sender
    //
    public Cahoots doSTONEWALLx2_0(long spendAmount, String address, int account) {
        NetworkParameters params = SamouraiWallet.getInstance().getCurrentNetworkParams();

        //
        //
        // step0: B sends spend amount to A,  creates step0
        //
        //
        STONEWALLx2 stonewall0 = new STONEWALLx2(spendAmount, address, params, account);
        try {
            stonewall0.setFingerprint(HD_WalletFactory.getInstance(context).getFingerprint());
        }
        catch(IOException | MnemonicException.MnemonicLengthException e) {
            ;
        }

        return stonewall0;
    }

    //
    // counterparty
    //
    public Cahoots doSTONEWALLx2_1(STONEWALLx2 stonewall0) throws Exception {

        List<UTXO> utxos = getCahootsUTXO(stonewall0.getCounterpartyAccount());
        Collections.shuffle(utxos);

        LogUtil.debug("CahootsUtil", "BIP84 utxos:" + utxos.size());

        List<UTXO> selectedUTXO = new ArrayList<UTXO>();
        long totalContributedAmount = 0L;
        for (int step = 0; step < 3; step++) {

            if (stonewall0.getCounterpartyAccount() == 0) {
                step = 2;
            }

            List<String> seenTxs = new ArrayList<String>();
            selectedUTXO = new ArrayList<UTXO>();
            totalContributedAmount = 0L;
            for (UTXO utxo : utxos) {

                switch (step) {
                    case 0:
                        if (utxo.getPath() != null && utxo.getPath().length() > 3 && utxo.getPath().charAt(2) != '0') {
                            continue;
                        }
                        break;
                    case 1:
                        if (utxo.getPath() != null && utxo.getPath().length() > 3 && utxo.getPath().charAt(2) != '1') {
                            continue;
                        }
                        break;
                    default:
                        break;
                }

                UTXO _utxo = new UTXO();
                for (MyTransactionOutPoint outpoint : utxo.getOutpoints()) {
                    if (!seenTxs.contains(outpoint.getTxHash().toString())) {
                        _utxo.getOutpoints().add(outpoint);
                        seenTxs.add(outpoint.getTxHash().toString());
                    }
                }

                if (_utxo.getOutpoints().size() > 0) {
                    selectedUTXO.add(_utxo);
                    totalContributedAmount += _utxo.getValue();
                    LogUtil.debug("CahootsUtil", "BIP84 selected utxo:" + _utxo.getValue());
                }

                if (totalContributedAmount > stonewall0.getSpendAmount() + SamouraiWallet.bDust.longValue()) {
                    break;
                }
            }
            if (totalContributedAmount > stonewall0.getSpendAmount() + SamouraiWallet.bDust.longValue()) {
                break;
            }
        }
        if (!(totalContributedAmount > stonewall0.getSpendAmount() + SamouraiWallet.bDust.longValue())) {
            return null;
        }

        LogUtil.debug("CahootsUtil", "BIP84 selected utxos:" + selectedUTXO.size());

        NetworkParameters params = stonewall0.getParams();

        //
        //
        // step1: A utxos -> B (take largest that cover amount)
        //
        //

        String zpub = BIP84Util.getInstance(context).getWallet().getAccountAt(stonewall0.getCounterpartyAccount()).zpubstr();
        HashMap<_TransactionOutPoint, Triple<byte[], byte[], String>> inputsA = new HashMap<_TransactionOutPoint, Triple<byte[], byte[], String>>();

        for (UTXO utxo : selectedUTXO) {
            for (MyTransactionOutPoint outpoint : utxo.getOutpoints()) {
                _TransactionOutPoint _outpoint = new _TransactionOutPoint(outpoint);

                ECKey eckey = SendFactory.getPrivKey(_outpoint.getAddress(), stonewall0.getCounterpartyAccount());
                String path = APIFactory.getInstance(context).getUnspentPaths().get(_outpoint.getAddress());
                inputsA.put(_outpoint, Triple.of(eckey.getPubKey(), stonewall0.getFingerprintCollab(), path));
            }
        }

        HashMap<_TransactionOutput, Triple<byte[], byte[], String>> outputsA = new HashMap<_TransactionOutput, Triple<byte[], byte[], String>>();
        if (stonewall0.getCounterpartyAccount() == WhirlpoolMeta.getInstance(context).getWhirlpoolPostmix()) {
            // contributor mix output
            int idx = AddressFactory.getInstance(context).getHighestPostChangeIdx();
            SegwitAddress segwitAddress0 = BIP84Util.getInstance(context).getAddressAt(stonewall0.getCounterpartyAccount(), 1, idx);
            Pair<Byte, byte[]> pair0 = Bech32Segwit.decode(SamouraiWallet.getInstance().isTestNet() ? "tb" : "bc", segwitAddress0.getBech32AsString());
            byte[] scriptPubKey_A0 = Bech32Segwit.getScriptPubkey(pair0.getLeft(), pair0.getRight());
            _TransactionOutput output_A0 = new _TransactionOutput(params, null, Coin.valueOf(stonewall0.getSpendAmount()), scriptPubKey_A0);
            outputsA.put(output_A0, Triple.of(segwitAddress0.getECKey().getPubKey(), stonewall0.getFingerprintCollab(), "M/1/" + idx));

            // contributor change output
            ++idx;
            SegwitAddress segwitAddress1 = BIP84Util.getInstance(context).getAddressAt(stonewall0.getCounterpartyAccount(), 1, idx);
            Pair<Byte, byte[]> pair1 = Bech32Segwit.decode(SamouraiWallet.getInstance().isTestNet() ? "tb" : "bc", segwitAddress1.getBech32AsString());
            byte[] scriptPubKey_A1 = Bech32Segwit.getScriptPubkey(pair1.getLeft(), pair1.getRight());
            _TransactionOutput output_A1 = new _TransactionOutput(params, null, Coin.valueOf(totalContributedAmount - stonewall0.getSpendAmount()), scriptPubKey_A1);
            outputsA.put(output_A1, Triple.of(segwitAddress1.getECKey().getPubKey(), stonewall0.getFingerprintCollab(), "M/1/" + idx));
            stonewall0.setCollabChange(segwitAddress1.getBech32AsString());
        } else {
            // contributor mix output
            int idx = BIP84Util.getInstance(context).getWallet().getAccount(0).getReceive().getAddrIdx();
            SegwitAddress segwitAddress0 = BIP84Util.getInstance(context).getAddressAt(0, 0, idx);
            if (segwitAddress0.getBech32AsString().equalsIgnoreCase(stonewall0.getDestination())) {
                segwitAddress0 = BIP84Util.getInstance(context).getAddressAt(0, 0, idx + 1);
            }
            Pair<Byte, byte[]> pair0 = Bech32Segwit.decode(SamouraiWallet.getInstance().isTestNet() ? "tb" : "bc", segwitAddress0.getBech32AsString());
            byte[] scriptPubKey_A0 = Bech32Segwit.getScriptPubkey(pair0.getLeft(), pair0.getRight());
            _TransactionOutput output_A0 = new _TransactionOutput(params, null, Coin.valueOf(stonewall0.getSpendAmount()), scriptPubKey_A0);
            outputsA.put(output_A0, Triple.of(segwitAddress0.getECKey().getPubKey(), stonewall0.getFingerprintCollab(), "M/0/" + idx));

            // contributor change output
            idx = BIP84Util.getInstance(context).getWallet().getAccount(0).getChange().getAddrIdx();
            SegwitAddress segwitAddress1 = BIP84Util.getInstance(context).getAddressAt(0, 1, idx);
            Pair<Byte, byte[]> pair1 = Bech32Segwit.decode(SamouraiWallet.getInstance().isTestNet() ? "tb" : "bc", segwitAddress1.getBech32AsString());
            byte[] scriptPubKey_A1 = Bech32Segwit.getScriptPubkey(pair1.getLeft(), pair1.getRight());
            _TransactionOutput output_A1 = new _TransactionOutput(params, null, Coin.valueOf(totalContributedAmount - stonewall0.getSpendAmount()), scriptPubKey_A1);
            outputsA.put(output_A1, Triple.of(segwitAddress1.getECKey().getPubKey(), stonewall0.getFingerprintCollab(), "M/1/" + idx));
            stonewall0.setCollabChange(segwitAddress1.getBech32AsString());
        }

        STONEWALLx2 stonewall1 = new STONEWALLx2(stonewall0);
        stonewall1.inc(inputsA, outputsA, null);

        return stonewall1;
    }

    //
    // sender
    //
    public Cahoots doSTONEWALLx2_2(STONEWALLx2 stonewall1) throws Exception {

        Transaction transaction = stonewall1.getTransaction();
        LogUtil.debug("CahootsUtil", "step2 tx:" + org.spongycastle.util.encoders.Hex.toHexString(transaction.bitcoinSerialize()));
        int nbIncomingInputs = transaction.getInputs().size();

        List<UTXO> utxos = getCahootsUTXO(stonewall1.getAccount());
        Collections.shuffle(utxos);

        LogUtil.debug("CahootsUtil", "BIP84 utxos:" + utxos.size());

        List<String> seenTxs = new ArrayList<String>();
        for (TransactionInput input : transaction.getInputs()) {
            if (!seenTxs.contains(input.getOutpoint().getHash().toString())) {
                seenTxs.add(input.getOutpoint().getHash().toString());
            }
        }

        List<UTXO> selectedUTXO = new ArrayList<UTXO>();
        long totalSelectedAmount = 0L;
        int nbTotalSelectedOutPoints = 0;
        for (int step = 0; step < 3; step++) {

            if (stonewall1.getCounterpartyAccount() == 0) {
                step = 2;
            }

            List<String> _seenTxs = seenTxs;
            selectedUTXO = new ArrayList<UTXO>();
            nbTotalSelectedOutPoints = 0;
            for (UTXO utxo : utxos) {

                switch (step) {
                    case 0:
                        if (utxo.getPath() != null && utxo.getPath().length() > 3 && utxo.getPath().charAt(2) != '0') {
                            continue;
                        }
                        break;
                    case 1:
                        if (utxo.getPath() != null && utxo.getPath().length() > 3 && utxo.getPath().charAt(2) != '1') {
                            continue;
                        }
                        break;
                    default:
                        break;
                }

                UTXO _utxo = new UTXO();
                for (MyTransactionOutPoint outpoint : utxo.getOutpoints()) {
                    if (!_seenTxs.contains(outpoint.getTxHash().toString())) {
                        _utxo.getOutpoints().add(outpoint);
                        _seenTxs.add(outpoint.getTxHash().toString());
                    }
                }

                if (_utxo.getOutpoints().size() > 0) {
                    selectedUTXO.add(_utxo);
                    totalSelectedAmount += _utxo.getValue();
                    nbTotalSelectedOutPoints += _utxo.getOutpoints().size();
                    LogUtil.debug("CahootsUtil", "BIP84 selected utxo:" + _utxo.getValue());
                }

                if (totalSelectedAmount > FeeUtil.getInstance().estimatedFeeSegwit(0, 0, nbTotalSelectedOutPoints + nbIncomingInputs, 4).longValue() + stonewall1.getSpendAmount() + SamouraiWallet.bDust.longValue()) {
                    break;
                }
            }
            if (totalSelectedAmount > FeeUtil.getInstance().estimatedFeeSegwit(0, 0, nbTotalSelectedOutPoints + nbIncomingInputs, 4).longValue() + stonewall1.getSpendAmount() + SamouraiWallet.bDust.longValue()) {
                break;
            }
        }
        if (!(totalSelectedAmount > FeeUtil.getInstance().estimatedFeeSegwit(0, 0, nbTotalSelectedOutPoints + nbIncomingInputs, 4).longValue() + stonewall1.getSpendAmount() + SamouraiWallet.bDust.longValue())) {
            return null;
        }

        LogUtil.debug("CahootsUtil", "BIP84 selected utxos:" + selectedUTXO.size());

        long fee = FeeUtil.getInstance().estimatedFeeSegwit(0, 0, nbTotalSelectedOutPoints + nbIncomingInputs, 4).longValue();
        LogUtil.debug("CahootsUtil", "fee:" + fee);
        if (fee % 2L != 0) {
            fee++;
        }
        LogUtil.debug("CahootsUtil", "fee pair:" + fee);
        stonewall1.setFeeAmount(fee);

        LogUtil.debug("CahootsUtil", "destination:" + stonewall1.getDestination());
        if (transaction.getOutputs() != null && transaction.getOutputs().size() == 2) {

            int idx = -1;
            for (int i = 0; i < 2; i++) {
                byte[] buf = transaction.getOutputs().get(i).getScriptBytes();
                byte[] script = new byte[buf.length];
                script[0] = 0x00;
                System.arraycopy(buf, 1, script, 1, script.length - 1);
                LogUtil.debug("CahootsUtil", "script:" + new Script(script).toString());
                LogUtil.debug("CahootsUtil", "script hex:" + Hex.toHexString(script));
                LogUtil.debug("CahootsUtil", "address from script:" + Bech32Util.getInstance().getAddressFromScript(new Script(script)));
                if(Bech32Util.getInstance().getAddressFromScript(new Script(script)).equalsIgnoreCase(stonewall1.getCollabChange())) {
                    idx = i;
                    break;
                }
            }

            if(idx == 0 || idx == 1) {
                Coin value = transaction.getOutputs().get(idx).getValue();
                Coin _value = Coin.valueOf(value.longValue() - (fee / 2L));
                LogUtil.debug("CahootsUtil", "output value post fee:" + _value);
                transaction.getOutputs().get(idx).setValue(_value);
                stonewall1.getPSBT().setTransaction(transaction);
            }
            else {
                return null;
            }

        }
        else {
            return null;
        }

        NetworkParameters params = stonewall1.getParams();

        //
        //
        // step2: B verif, utxos -> A (take smallest that cover amount)
        //
        //

        String zpub = BIP84Util.getInstance(context).getWallet().getAccountAt(stonewall1.getAccount()).zpubstr();
        HashMap<_TransactionOutPoint, Triple<byte[], byte[], String>> inputsB = new HashMap<_TransactionOutPoint, Triple<byte[], byte[], String>>();

        for (UTXO utxo : selectedUTXO) {
            for (MyTransactionOutPoint outpoint : utxo.getOutpoints()) {
                _TransactionOutPoint _outpoint = new _TransactionOutPoint(outpoint);

                ECKey eckey = SendFactory.getPrivKey(_outpoint.getAddress(), stonewall1.getAccount());
                String path = APIFactory.getInstance(context).getUnspentPaths().get(_outpoint.getAddress());
                inputsB.put(_outpoint, Triple.of(eckey.getPubKey(), FormatsUtil.getInstance().getFingerprintFromXPUB(zpub), path));
            }
        }

        // spender change output
        HashMap<_TransactionOutput, Triple<byte[], byte[], String>> outputsB = new HashMap<_TransactionOutput, Triple<byte[], byte[], String>>();
        if (stonewall1.getAccount() == WhirlpoolMeta.getInstance(context).getWhirlpoolPostmix()) {
            int idx = AddressFactory.getInstance(context).getHighestPostChangeIdx();
            SegwitAddress segwitAddress = BIP84Util.getInstance(context).getAddressAt(stonewall1.getAccount(), 1, idx);
            Pair<Byte, byte[]> pair0 = Bech32Segwit.decode(SamouraiWallet.getInstance().isTestNet() ? "tb" : "bc", segwitAddress.getBech32AsString());
            byte[] scriptPubKey_B0 = Bech32Segwit.getScriptPubkey(pair0.getLeft(), pair0.getRight());
            _TransactionOutput output_B0 = new _TransactionOutput(params, null, Coin.valueOf((totalSelectedAmount - stonewall1.getSpendAmount()) - (fee / 2L)), scriptPubKey_B0);
            outputsB.put(output_B0, Triple.of(segwitAddress.getECKey().getPubKey(), stonewall1.getFingerprint(), "M/1/" + idx));
        } else {
            int idx = BIP84Util.getInstance(context).getWallet().getAccount(0).getChange().getAddrIdx();
            SegwitAddress segwitAddress = BIP84Util.getInstance(context).getAddressAt(0, 1, idx);
            Pair<Byte, byte[]> pair0 = Bech32Segwit.decode(SamouraiWallet.getInstance().isTestNet() ? "tb" : "bc", segwitAddress.getBech32AsString());
            byte[] scriptPubKey_B0 = Bech32Segwit.getScriptPubkey(pair0.getLeft(), pair0.getRight());
            _TransactionOutput output_B0 = new _TransactionOutput(params, null, Coin.valueOf((totalSelectedAmount - stonewall1.getSpendAmount()) - (fee / 2L)), scriptPubKey_B0);
            outputsB.put(output_B0, Triple.of(segwitAddress.getECKey().getPubKey(), stonewall1.getFingerprint(), "M/1/" + idx));
        }

        STONEWALLx2 stonewall2 = new STONEWALLx2(stonewall1);
        stonewall2.inc(inputsB, outputsB, null);

        return stonewall2;
    }

    //
    // counterparty
    //
    public Cahoots doSTONEWALLx2_3(STONEWALLx2 stonewall2) throws Exception {

        HashMap<String, String> utxo2Address = new HashMap<String, String>();
        List<UTXO> utxos = null;
        if (stonewall2.getCounterpartyAccount() == WhirlpoolMeta.getInstance(context).getWhirlpoolPostmix()) {
            utxos = APIFactory.getInstance(context).getUtxosPostMix(true);
        } else {
            utxos = APIFactory.getInstance(context).getUtxos(true);
        }
        for (UTXO utxo : utxos) {
            for (MyTransactionOutPoint outpoint : utxo.getOutpoints()) {
                utxo2Address.put(outpoint.getTxHash().toString() + "-" + outpoint.getTxOutputN(), outpoint.getAddress());
            }
        }

        Transaction transaction = stonewall2.getTransaction();
        HashMap<String, ECKey> keyBag_A = new HashMap<String, ECKey>();
        for (TransactionInput input : transaction.getInputs()) {
            TransactionOutPoint outpoint = input.getOutpoint();
            String key = outpoint.getHash().toString() + "-" + outpoint.getIndex();
            if (utxo2Address.containsKey(key)) {
                String address = utxo2Address.get(key);
                ECKey eckey = SendFactory.getPrivKey(address, stonewall2.getCounterpartyAccount());
                keyBag_A.put(outpoint.toString(), eckey);
            }
        }

        STONEWALLx2 stonewall3 = new STONEWALLx2(stonewall2);
        stonewall3.inc(null, null, keyBag_A);

        return stonewall3;
    }

    //
    // sender
    //
    public Cahoots doSTONEWALLx2_4(STONEWALLx2 stonewall3) throws Exception {

        HashMap<String, String> utxo2Address = new HashMap<String, String>();
        List<UTXO> utxos = null;
        if (stonewall3.getAccount() == WhirlpoolMeta.getInstance(context).getWhirlpoolPostmix()) {
            utxos = APIFactory.getInstance(context).getUtxosPostMix(true);
        } else {
            utxos = APIFactory.getInstance(context).getUtxos(true);
        }
        for (UTXO utxo : utxos) {
            for (MyTransactionOutPoint outpoint : utxo.getOutpoints()) {
                utxo2Address.put(outpoint.getTxHash().toString() + "-" + outpoint.getTxOutputN(), outpoint.getAddress());
            }
        }

        Transaction transaction = stonewall3.getTransaction();
        HashMap<String, ECKey> keyBag_B = new HashMap<String, ECKey>();
        for (TransactionInput input : transaction.getInputs()) {
            TransactionOutPoint outpoint = input.getOutpoint();
            String key = outpoint.getHash().toString() + "-" + outpoint.getIndex();
            if (utxo2Address.containsKey(key)) {
                String address = utxo2Address.get(key);
                ECKey eckey = SendFactory.getPrivKey(address, stonewall3.getAccount());
                keyBag_B.put(outpoint.toString(), eckey);
            }
        }

        STONEWALLx2 stonewall4 = new STONEWALLx2(stonewall3);
        stonewall4.inc(null, null, keyBag_B);

        return stonewall4;

    }

    public static List<UTXO> getCahootsUTXO(int account) {
        List<UTXO> ret = new ArrayList<UTXO>();
        List<UTXO> _utxos = null;
        if(account == WhirlpoolMeta.getInstance(context).getWhirlpoolPostmix())    {
            _utxos = APIFactory.getInstance(context).getUtxosPostMix(true);
        }
        else    {
            _utxos = APIFactory.getInstance(context).getUtxos(true);
        }
        for(UTXO utxo : _utxos)   {
            String script = Hex.toHexString(utxo.getOutpoints().get(0).getScriptBytes());
            if(script.startsWith("0014") && APIFactory.getInstance(context).getUnspentPaths().get(utxo.getOutpoints().get(0).getAddress()) != null)   {
                ret.add(utxo);
            }
        }

        return ret;
    }

    public long getCahootsValue(int account) {
        long ret = 0L;
        List<UTXO> _utxos = getCahootsUTXO(account);
        for(UTXO utxo : _utxos)   {
            ret += utxo.getValue();
        }

        return ret;
    }

}