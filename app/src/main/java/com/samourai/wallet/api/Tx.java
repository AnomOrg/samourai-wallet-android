package com.samourai.wallet.api;

import org.json.JSONException;
import org.json.JSONObject;

public class Tx {

    private String strHash = null;
    private String strDirection = null;
    private String strAddress = null;
    private String strPaymentCode = null;
    private double amount = 0.0;
    private long confirmations = 0L;
    private long ts = 0L;

    public Tx(String hash, String address, double amount, long date, long confirmations) {
        this.strHash = hash;
        this.strAddress = address;
        this.amount = amount;
        this.ts = date;
        this.confirmations = confirmations;
        this.strPaymentCode = null;
    }

    public Tx(String hash, String address, double amount, long date, long confirmations, String pcode) {
        this.strHash = hash;
        this.strAddress = address;
        this.amount = amount;
        this.ts = date;
        this.confirmations = confirmations;
        this.strPaymentCode = pcode;
    }

    public Tx(JSONObject jsonObj) {
        fromJSON(jsonObj);
    }

    public String getAddress() {
        return strAddress;
    }

    public void setAddress(String address) {
        strAddress = address;
    }

    public String getHash() {
        return strHash;
    }

    public void setHash(String hash) {
        strHash = hash;
    }

    public String getDirection() {
        return strDirection;
    }

    public void setDirection(String direction) {
        strDirection = direction;
    }

    public long getTS() {
        return ts;
    }

    public void setTS(long ts) {
        this.ts = ts;
    }

    public long getConfirmations() {
        return confirmations;
    }

    public void setConfirmations(long confirmations) {
        this.confirmations = confirmations;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getPaymentCode() {
        return strPaymentCode;
    }

    public void setPaymentCode(String pcode) {
        this.strPaymentCode = pcode;
    }

    public JSONObject toJSON()  {

        JSONObject obj = new JSONObject();
        try {
            if(strHash != null)    {
                obj.put("hash", strHash);
            }
            if(strDirection != null)    {
                obj.put("direction", strDirection);
            }
            if(strAddress != null)    {
                obj.put("address", strAddress);
            }
            if(strPaymentCode != null)    {
                obj.put("pcode", strPaymentCode);
            }
            obj.put("amount", amount);
            obj.put("confirmations", confirmations);
            obj.put("ts", ts);
        }
        catch(JSONException je) {
            ;
        }

        return obj;

    }

    public void fromJSON(JSONObject jsonObj)  {

        try {
            if(jsonObj.has("hash"))    {
                strHash = jsonObj.getString("hash");
            }
            if(jsonObj.has("direction"))    {
                strDirection = jsonObj.getString("direction");
            }
            if(jsonObj.has("address"))    {
                strAddress = jsonObj.getString("address");
            }
            if(jsonObj.has("pcode"))    {
                strPaymentCode = jsonObj.getString("pcode");
            }
            if(jsonObj.has("amount"))    {
                amount = jsonObj.getDouble("amount");
            }
            if(jsonObj.has("confirmations"))    {
                confirmations = jsonObj.getLong("confirmations");
            }
            if(jsonObj.has("ts"))    {
                ts = jsonObj.getLong("ts");
            }

        }
        catch(JSONException je) {
            ;
        }

    }

}
