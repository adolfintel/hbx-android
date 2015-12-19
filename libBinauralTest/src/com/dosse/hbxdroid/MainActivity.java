package com.dosse.hbxdroid;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.dosse.binaural.BinauralEnvelope;
import com.dosse.binaural.BinauralEnvelopePlayer;
import com.dosse.libBinauralTest.beta.R;
import com.google.ads.AdRequest;
import com.google.ads.AdView;


public class MainActivity extends Activity {
	private Button playPause, stop;
	private SeekBar prog;
	private TextView time;
	private static BEPThread bep;
	private static boolean playing = false;
	private static Notificator not;
	private static String currentPreset = "", currentPresetName= "";
	private Menu optionsMenu;
	private void removeBuyOption(){
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				optionsMenu.getItem(1).setVisible(false);
			}
		});
	}
	
	private class Notificator extends Thread {
		public void run() {
			String notificationName = getString(R.string.app_name);
			for (;;) {
				try {
					sleep(700);
				} catch (InterruptedException e) {
				}
				if (playing == true) {
					NotificationManager notifManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
					Notification note = new Notification(
							R.drawable.ic_launcher, notificationName, 0);
					note.flags = Notification.FLAG_ONGOING_EVENT;
					PendingIntent intent = PendingIntent.getActivity(
							getApplicationContext(), 0,
							new Intent(getApplicationContext(),
									MainActivity.class), 0);
					note.setLatestEventInfo(getApplicationContext(),
							notificationName, currentPresetName, intent);
					notifManager.notify(0, note);
				} else {
					NotificationManager notifManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
					notifManager.cancel(0);
				}
			}
		}
	}

	private class BEPThread extends Thread {
		private BinauralEnvelopePlayer beplayer;
		private BinauralEnvelope be;
		private boolean killASAP = false;
		private SeekBar prog;
		private TextView time;
		private Button playPause;

		public BEPThread(BinauralEnvelope be, TextView time, SeekBar prog,
				Button playPause) {
			this.be = be;
			this.time = time;
			this.prog = prog;
			this.playPause = playPause;
			playPause.setText(getString(R.string.play));
			prog.setProgress(0);
			time.setText("");
			playing = false;
			createBEPlayer(be);
		}

		public void switchPreset(BinauralEnvelope newPreset) {
			this.be = newPreset;
			beplayer.stopPlaying();
			playing = true;
			createBEPlayer(newPreset);
			beplayer.paused = false;
			playPause.setText(getString(R.string.pause));
			if (!((AudioManager) getSystemService(Context.AUDIO_SERVICE))
					.isWiredHeadsetOn())
				Toast.makeText(getApplicationContext(),
						getString(R.string.headphonesWarning),
						Toast.LENGTH_LONG).show();
		}

		public void pause() {
			beplayer.paused = true;
		}

		public void unPause() {
			beplayer.paused = false;
		}

		public void stopPlaying() {
			beplayer.stopPlaying();
			killASAP = true;
		}

		private void setPosition(double p) {
			beplayer.setPosition(p);
		}

		private void createBEPlayer(BinauralEnvelope be) {
			beplayer = new BinauralEnvelopePlayer(be);
			beplayer.paused = true;
			beplayer.start();
		}

		public void run() {
			for (;;) {
				while (beplayer.getPosition() <= 1) {
					if (killASAP) {
						return;
					}
					try {
						sleep(10);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					if (killASAP) {
						return;
					}
					if (beplayer.getPlaybackProblems() > 20) {
						beplayer.paused = true;
						beplayer.resetPlaybackProblems();
						playing = false;
						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								playPause.setText(getString(R.string.play));
								Toast.makeText(getApplicationContext(),
										getString(R.string.shitPhone),
										Toast.LENGTH_LONG).show();
							}
						});
					}
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							prog.setProgress((int) (100 * beplayer
									.getPosition()));
							int len = (int) beplayer.getLength();
							int pos = (int) (beplayer.getPosition() * beplayer
									.getLength());
							int lenH = len / 3600;
							len %= 3600;
							int lenM = len / 60;
							len %= 60;
							int lenS = len;
							int posH = pos / 3600;
							pos %= 3600;
							int posM = pos / 60;
							pos %= 60;
							int posS = pos;
							time.setText((posH < 10 ? "0" + posH : posH) + ":"
									+ (posM < 10 ? "0" + posM : posM) + ":"
									+ (posS < 10 ? "0" + posS : posS) + " / "
									+ (lenH < 10 ? "0" + lenH : lenH) + ":"
									+ (lenM < 10 ? "0" + lenM : lenM) + ":"
									+ (lenS < 10 ? "0" + lenS : lenS));
						}
					});
				}
				beplayer.stopPlaying();
				playing = false;
				createBEPlayer(be);
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						playPause.setText(getString(R.string.play));
					}
				});
			}
		}
	}

	private BinauralEnvelope loadPreset(String name) {
		BinauralEnvelope preset = null;
		try {
			if (name == null)
				throw new Exception();
			currentPreset = name;
			currentPresetName=cleanupString(name);
			if (name.toLowerCase().endsWith(".hbl")) {
				BufferedReader xmlFile = new BufferedReader(
						new InputStreamReader(openFileInput(name)));
				String xml = "";
				for (;;) {
					try {
						String line = xmlFile.readLine();
						if (line == null)
							break;
						else
							xml += line + "\n";
					} catch (IOException e) {
						break;
					}
				}
				xmlFile.close();
				preset = BinauralEnvelope.fromXML(xml);
			} else {
				FileInputStream fis = openFileInput(name);
				byte[] header = new byte[3];
				fis.read(header);
				if (header[0] == 'H' && header[1] == 'B' && header[2] == 'X') {
					ObjectInputStream ois = new ObjectInputStream(
							new GZIPInputStream(fis));
					preset = (BinauralEnvelope) ois.readObject();
					ois.close();
				} else {
					if (header[0] == 'H' && header[1] == 'B'
							&& header[2] == 'S') {
						ByteArrayOutputStream baos = new ByteArrayOutputStream();
						for (;;) {
							int in = fis.read();
							if (in == -1) {
								break;
							} else {
								baos.write(in);
							}
						}
						preset = BinauralEnvelope.fromHES(baos.toByteArray());
						fis.close();
					} else {
						throw new Exception(getString(R.string.notAPreset));
					}
				}
			}
			Toast.makeText(getApplicationContext(),
					getString(R.string.loadSuccess), Toast.LENGTH_SHORT).show();
		} catch (Exception e) {
			if (e.getMessage() != null) {
				Toast.makeText(this,
						getString(R.string.loadFail) + e.getMessage(),
						Toast.LENGTH_LONG).show();
				e.printStackTrace();
			}
			preset = new BinauralEnvelope();
			preset.setBaseF(0);
			preset.setPoint(0, 0, 0, 0, 0, 0, 0);
			preset.setPoint(0.0001, 0, 0, 0, 0, 0, 0);
			currentPreset = "";
			currentPresetName="";
		}
		return preset;
	}
	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		playPause = (Button) findViewById(R.id.p);
		stop = (Button) findViewById(R.id.s);
		prog = (SeekBar) findViewById(R.id.prog);
		time = (TextView) findViewById(R.id.t);
		new Thread() {
			public void run() {
				while(optionsMenu==null) //this workaround is cheaper than a pop station 3
					try {
						Thread.sleep(10);
					} catch (InterruptedException e) {}
				if (getPackageManager().checkSignatures(getPackageName(), "com.dosse.libbinauraltestkey")== PackageManager.SIGNATURE_MATCH) {
					removeBuyOption();
				}else{
					Looper.prepare();
					((AdView)(findViewById(R.id.adView))).loadAd(new AdRequest());
				}
			}
		}.start();
		if (bep == null) {
			BinauralEnvelopePlayer.loadNoiseFromAssets(getApplication()
					.getAssets());
			bep = new BEPThread(loadPreset(null), time, prog, playPause);
			bep.start();
		} else {
			bep.time = time;
			bep.prog = prog;
			bep.playPause = playPause;
			playPause.setText(playing ? getString(R.string.pause)
					: getString(R.string.play));
		}
		if (not == null) {
			not = new Notificator();
			not.start();
		}
		playPause.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (playing) {
					bep.pause();
					playPause.setText(getString(R.string.play));
				} else {
					if (!((AudioManager) getSystemService(Context.AUDIO_SERVICE))
							.isWiredHeadsetOn())
						Toast.makeText(getApplicationContext(),
								getString(R.string.headphonesWarning),
								Toast.LENGTH_LONG).show();
					bep.unPause();
					playPause.setText(getString(R.string.pause));
				}
				playing = !playing;
			}
		});
		stop.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (playing)
					bep.setPosition(2);
				else
					bep.setPosition(0);
			}
		});
		prog.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
			}

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				if (fromUser) {
					bep.setPosition(progress / 100.0);
				}
			}
		});
		try {
			FileInputStream firstRun = openFileInput("firstRun");
			firstRun.close();
		} catch (Exception e) {
			DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					if (which == DialogInterface.BUTTON_POSITIVE)
						startActivity(new Intent(getApplicationContext(),
								TutorialActivity.class));
				}
			};
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage(getString(R.string.wantTutorial))
					.setPositiveButton(getString(R.string.yes),
							dialogClickListener)
					.setNegativeButton(getString(R.string.no),
							dialogClickListener).show();
			try {
				FileOutputStream firstRun = openFileOutput("firstRun",
						MODE_PRIVATE);
				firstRun.close();
			} catch (Exception e1) {
				Toast.makeText(getApplicationContext(),
						getString(R.string.saveFail) + e1.getMessage(),
						Toast.LENGTH_LONG).show();
			}
		}

	}
	private String cleanupString(String s){
		String x=s.substring(0, s.lastIndexOf('_')).replace("_", " ").trim();
		String ret=""+Character.toUpperCase(x.charAt(0));
		for(int i=1;i<x.length();i++) if(x.charAt(i-1)==' ') ret+=Character.toUpperCase(x.charAt(i)); else ret+=x.charAt(i);
		return ret;
	}
	private List<Map<String, String>> presetList = new ArrayList<Map<String, String>>();

	private void populatePresetList() {
		ListView fileList = (ListView) findViewById(R.id.fileList);
		presetList.clear();
		String[] list = fileList();
		HashMap<String, String> item = new HashMap<String, String>();
		item.put("preset", getString(R.string.getPresets));
		item.put("path", "http://hbx.adolfintel.com/presets_app.html");
		item.put("cleanName",getString(R.string.getPresets));
		presetList.add(item);
		for (String f : list) {
			if (!f.toLowerCase().endsWith(".hbx")
					&& !f.toLowerCase().endsWith(".hbs")
					&& !f.toLowerCase().endsWith(".hbl"))
				continue;
			item = new HashMap<String, String>();
			item.put("preset", f);
			item.put("path", getString(R.string.downloadedPreset));
			item.put("cleanName", cleanupString(f));
			presetList.add(item);
		}
		fileList.setAdapter(new SimpleAdapter(this, presetList,
				android.R.layout.simple_list_item_2, new String[] { "cleanName",
						"path" }, new int[] { android.R.id.text1,
						android.R.id.text2 }));
		fileList.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				String path = presetList.get(arg2).get("path");
				if (path.startsWith("http://")) {
					Intent intent = new Intent(getApplicationContext(),
							BrowserActivity.class);
					intent.putExtra("url", path);
					startActivity(intent);
				} else
					bep.switchPreset(loadPreset(presetList.get(arg2).get(
							"preset")));
			}
		});
		fileList.setOnItemLongClickListener(new OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
					final int arg2, long arg3) {
				if (!presetList.get(arg2).get("path").startsWith("http://")) {
					DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							if (which == DialogInterface.BUTTON_POSITIVE) {
								deleteFile(presetList.get(arg2).get("preset"));
								Toast.makeText(getApplicationContext(),
										getString(R.string.deleted),
										Toast.LENGTH_SHORT).show();
								populatePresetList();
							}

						}
					};
					AlertDialog.Builder builder = new AlertDialog.Builder(
							MainActivity.this);
					builder.setMessage(getString(R.string.delete))
							.setPositiveButton(getString(R.string.yes),
									dialogClickListener)
							.setNegativeButton(getString(R.string.no),
									dialogClickListener).show();
				}
				return true;
			}
		});
	}

	@Override
	public void onResume() {
		populatePresetList();
		super.onResume();
	}
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		optionsMenu=menu;
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.exit) {
			NotificationManager notifManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
			notifManager.cancel(0);
			System.exit(0);
			return true;
		}
		if (item.getItemId() == R.id.about) {
			startActivity(new Intent(this, AboutActivity.class));
			return true;
		}
		if (item.getItemId() == R.id.tutorial) {
			startActivity(new Intent(this, TutorialActivity.class));
			return true;
		}
		if (item.getItemId() == R.id.market) {
			startActivity(new Intent(Intent.ACTION_VIEW,
					Uri.parse("market://details?id=com.dosse.libBinauralTest.beta")));
			return true;
		}
		if (item.getItemId() == R.id.fb) {
			startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.facebook.com/pages/HBX-Binaural-Player/153892124808340")));
			return true;
		}
		if (item.getItemId() == R.id.gplus) {
			startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://plus.google.com/107633048318981016281")));
			return true;
		}
		if (item.getItemId() == R.id.twitter) {
			startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://twitter.com/HBXBinaural")));
			return true;
		}
		return false;

	}

}
