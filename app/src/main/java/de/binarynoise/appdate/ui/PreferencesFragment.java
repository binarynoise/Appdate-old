package de.binarynoise.appdate.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.fragment.app.Fragment;

import de.binarynoise.appdate.R;

import static de.binarynoise.appdate.SFC.sfcm;

/**
 * A simple {@link Fragment} subclass.
 */
public class PreferencesFragment extends Fragment {
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		// Inflate the layout for this fragment
		return inflater.inflate(R.layout.fragment_preferences, container, false);
	}
	
	@Override
	public void onStart() {
		super.onStart();
		requireContext();
		requireActivity();
		sfcm.sfc.preferencesFragment = this;
	}
	
	@Override
	public void onStop() {
		super.onStop();
		if (sfcm.sfc.preferencesFragment == this)
			sfcm.sfc.preferencesFragment = null;
	}
}
