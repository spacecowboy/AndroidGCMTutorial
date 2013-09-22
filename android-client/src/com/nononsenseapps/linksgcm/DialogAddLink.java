package com.nononsenseapps.linksgcm;

import android.app.DialogFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.EditText;

/**
 * Simple confirm dialog fragment.
 * 
 */
public class DialogAddLink extends DialogFragment {

	public DialogAddLink() {
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setStyle(DialogFragment.STYLE_NORMAL,
				android.R.style.Theme_Holo_Light_Dialog);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		getDialog().setTitle(R.string.add_link);
		final View v = inflater.inflate(R.layout.dialog_add_link, container,
				false);
		final EditText uriText = (EditText) v.findViewById(R.id.uriText);

		v.findViewById(R.id.dialog_no).setOnClickListener(
				new OnClickListener() {

					@Override
					public void onClick(View v) {
						getDialog().dismiss();
					}
				});

		v.findViewById(R.id.dialog_yes).setOnClickListener(
				new OnClickListener() {

					@Override
					public void onClick(View v) {
						final String uri = uriText.getText().toString().trim();
						if (!uri.isEmpty()) {
							// Add in background
							AddLinkService.addLink(getActivity(), uri);
							getDialog().dismiss();
						}
					}
				});

		return v;
	}
}
