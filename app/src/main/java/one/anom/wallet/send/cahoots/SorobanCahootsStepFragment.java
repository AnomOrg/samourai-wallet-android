package one.anom.wallet.send.cahoots;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import one.anom.wallet.R;

public class SorobanCahootsStepFragment extends AbstractCahootsStepFragment {

    public static SorobanCahootsStepFragment newInstance(int position) {
        Bundle args = new Bundle();
        args.putInt("step", position);
        SorobanCahootsStepFragment fragment = new SorobanCahootsStepFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.cahoots_step_view_soroban, container, false);
    }
}
