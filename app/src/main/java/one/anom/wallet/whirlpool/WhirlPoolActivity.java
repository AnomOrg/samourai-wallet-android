package one.anom.wallet.whirlpool;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.widget.TextView;

import one.anom.wallet.R;

import org.bitcoinj.core.Coin;

public class WhirlPoolActivity extends AppCompatActivity {

    private TextView tvTotalSelected = null;
    private TextView tvTotalCycle = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_whirl_pool);
        Toolbar toolbar = findViewById(R.id.toolbar_whirlpool);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        tvTotalSelected = findViewById(R.id.totalSelected);
        tvTotalCycle = findViewById(R.id.totalCycle);

        Bundle extras = getIntent().getExtras();
        if(extras != null && extras.containsKey("total"))    {
            long totalSelected = extras.getLong("total");

            tvTotalSelected.setText(Coin.valueOf(totalSelected).toPlainString() + " BTC");
            tvTotalCycle.setText(getText(R.string.cycle) + " " + Coin.valueOf(totalSelected).toPlainString() + " BTC");
        }

    }
}
