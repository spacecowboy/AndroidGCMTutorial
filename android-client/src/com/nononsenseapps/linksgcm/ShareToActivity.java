package com.nononsenseapps.linksgcm;

import android.os.Bundle;
import android.widget.Toast;
import android.app.Activity;
import android.content.Intent;

public class ShareToActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//setContentView(R.layout.activity_share_to);
		final Intent intent = getIntent();
		if (intent != null) {
			handleIntent(intent);
		}
		Toast.makeText(this, R.string.link_added, Toast.LENGTH_SHORT).show();
		finish();
	}

	private void handleIntent(final Intent intent) {
		if (Intent.ACTION_SEND.equals(intent.getAction())) {
			AddLinkService.addLink(this, intent.getStringExtra(Intent.EXTRA_TEXT));
		}
	}

}
