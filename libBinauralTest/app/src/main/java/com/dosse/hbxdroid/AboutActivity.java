package com.dosse.hbxdroid;

import com.dosse.libBinauralTest.beta.R;

import android.app.Activity;
import android.os.Bundle;
import android.text.Html;
import android.text.method.ScrollingMovementMethod;
import android.widget.TextView;

public class AboutActivity extends Activity {

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.activity_about);
	    TextView aboutText=((TextView)findViewById(R.id.aboutText));
	    aboutText.setMovementMethod(new ScrollingMovementMethod());
	    aboutText.setText(Html.fromHtml(getString(R.string.aboutHTML)));
	}

}
