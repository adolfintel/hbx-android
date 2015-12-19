package com.dosse.hbxdroid;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import com.dosse.libBinauralTest.beta.R;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.webkit.DownloadListener;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

public class BrowserActivity extends Activity {

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_browser);
		final WebView w = (WebView) findViewById(R.id.browser);

		w.setWebViewClient(new WebViewClient() {
			private boolean firstLoad=true;
			public void onLoadResource (WebView view, String url){
				if(firstLoad){w.setVisibility(View.INVISIBLE);} // show loading animation
			}

			public void onPageFinished(WebView view, String url) {
				if(firstLoad){w.setVisibility(View.VISIBLE); firstLoad=false;} // hide loading animation
			}

			public void onReceivedError(WebView view, int errorCode,
					String description, String failingUrl) {
				Toast.makeText(getApplicationContext(),
						getString(R.string.noConnection), Toast.LENGTH_SHORT)
						.show();
				finish();
			}

			public boolean shouldOverrideUrlLoading(WebView view, String url) {
				return false;
			}
		});
		w.getSettings().setJavaScriptEnabled(true);
		w.loadUrl(getIntent().getExtras().getString("url"));
		w.setDownloadListener(new DownloadListener() {
			@Override
			public void onDownloadStart(final String url, String userAgent,
					String contentDisposition, String mimetype,
					long contentLength) {
				Thread t = new Thread() {
					public void run() {
						try {
							URL u = new URL(url);
							String name = u.getFile();
							name = name.substring(name.lastIndexOf("/") + 1,
									name.length());
							System.out.println(name);
							URLConnection c = u.openConnection();
							c.connect();
							InputStream in = new BufferedInputStream(u
									.openStream());
							FileOutputStream out = getApplicationContext()
									.openFileOutput(name, MODE_PRIVATE);
							for (;;) {
								byte[] buff = new byte[1024];
								try {
									int l = in.read(buff);
									out.write(buff, 0, l);
								} catch (Exception e) {
									break;
								}
							}
							in.close();
							out.flush();
							out.close();
							runOnUiThread(new Runnable() {
								@Override
								public void run() {
									Toast.makeText(
											getApplicationContext(),
											getString(R.string.downloadSuccess),
											Toast.LENGTH_LONG).show();

								}
							});
						} catch (Throwable t) {
							runOnUiThread(new Runnable() {
								@Override
								public void run() {
									Toast.makeText(getApplicationContext(),
											getString(R.string.downloadFail),
											Toast.LENGTH_SHORT).show();
								}
							});
						}
					}
				};
				t.start();
				try {
					t.join();
				} catch (InterruptedException e) {
				}
			}
		});
	}
}
