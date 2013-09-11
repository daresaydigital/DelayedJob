package com.screeninteraction.delayedjob;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.widget.Toast;

public class ToastJobHandler extends DelayedJobHandler {

	public static final String EXTRA_CHECKED = "extra_checked";

	@Override
	public boolean performJob(final Context context, Intent job) {
		final boolean checked = job.getBooleanExtra(EXTRA_CHECKED, false);
		
		Handler mainHandler = new Handler(context.getMainLooper());
		mainHandler.post(new Runnable() {
			
			@Override
			public void run() {
				Toast.makeText(context, "checked = "+checked, Toast.LENGTH_SHORT).show();
			}
		});
		
		//always return true, this job cannot fails
		return true;
	}

}
