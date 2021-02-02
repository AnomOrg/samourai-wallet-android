package com.samourai.wallet.settings

import android.os.Bundle
import android.view.View
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.transition.Transition
import one.anom.wallet.R


class MainSettingsFragment : PreferenceFragmentCompat(), Preference.OnPreferenceClickListener {

    var targetTransition: Transition? = null;

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings_root, rootKey)
        findPreference<Preference>("txs")?.onPreferenceClickListener = this
        findPreference<Preference>("wallet")?.onPreferenceClickListener = this
        findPreference<Preference>("troubleshoot")?.onPreferenceClickListener = this
        findPreference<Preference>("other")?.onPreferenceClickListener = this
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        this.targetTransition?.addTarget(view);
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onPreferenceClick(preference: Preference?): Boolean {

        val fragment = SettingsDetailsFragment(preference?.key)

        (activity as SettingsActivity).setFragment(fragment, true);
        return true
    }

}