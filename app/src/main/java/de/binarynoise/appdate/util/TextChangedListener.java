package de.binarynoise.appdate.util;

import android.text.Editable;
import android.text.TextWatcher;

@SuppressWarnings("FinalMethod")
public abstract class TextChangedListener implements TextWatcher {
	@Override
	public final void beforeTextChanged(CharSequence s, int start, int count, int after) {}
	
	@Override
	public final void onTextChanged(CharSequence s, int start, int before, int count) {
		onTextChange(s.toString());
	}
	
	@Override
	public final void afterTextChanged(Editable s) {}
	
	public abstract void onTextChange(String s);
}
