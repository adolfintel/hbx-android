package com.dosse.hbxdroid;

import com.dosse.libBinauralTest.beta.R;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;

public class TutorialActivity extends Activity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_tutorial);
		MyPagerAdapter adapter = new MyPagerAdapter();
		ViewPager myPager = (ViewPager) findViewById(R.id.panelpager);
		myPager.setAdapter(adapter);
		myPager.setCurrentItem(0);
	}

	private class MyPagerAdapter extends PagerAdapter {
		public int getCount() {
			return 5;
		}

		public Object instantiateItem(View collection, int position) {
			LayoutInflater inflater = (LayoutInflater) collection.getContext()
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			int resId = 0;
			switch (position) {
			case 0:
				resId = R.layout.page0;
				break;
			case 1:
				resId = R.layout.page1;
				break;
			case 2:
				resId = R.layout.page2;
				break;
			case 3:
				resId = R.layout.page3;
				break;
			case 4:
				resId = R.layout.page4;
				break;
			}
		
			View view = inflater.inflate(resId, null);
			((ViewPager) collection).addView(view, 0);
			return view;
		}

		public void destroyItem(View arg0, int arg1, Object arg2) {
			((ViewPager) arg0).removeView((View) arg2);
		}

		public boolean isViewFromObject(View arg0, Object arg1) {
			return arg0 == ((View) arg1);
		}

		public Parcelable saveState() {
			return null;
		}
	}

}
