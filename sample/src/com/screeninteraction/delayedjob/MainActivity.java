package com.screeninteraction.delayedjob;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ToggleButton;

public class MainActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// initialize DelayedJob
		DelayedJob.init(this);

		ToggleButton toggle = (ToggleButton) findViewById(R.id.make_toast);
		toggle.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				Intent i = new Intent();
				i.putExtra(ToastJobHandler.EXTRA_CHECKED, isChecked);
				// Run this job after 2 seconds replacing any previous job
				new DelayedJob(ToastJobHandler.class, i).replacePrevious(true)
						.withDelay(2 * 1000).withId(1).perform();
			}
		});
	}

}
