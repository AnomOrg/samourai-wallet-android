package one.anom.wallet;

import android.app.Activity;
import android.os.Bundle;

import one.anom.wallet.util.TimeOutUtil;

public class ExodusActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TimeOutUtil.getInstance().reset();
        
        finish();
    }

}