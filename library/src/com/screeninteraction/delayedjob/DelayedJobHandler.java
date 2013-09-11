package com.screeninteraction.delayedjob;

import android.content.Context;
import android.content.Intent;

public abstract class DelayedJobHandler {

	public DelayedJobHandler() {
	}

	/**
	 * 
	 * @param context
	 *            The application context
	 * 
	 * @param job
	 *            The job to perform.
	 * 
	 * @return true if the job has been successfully performed, this will not
	 *         retry the job. false otherwise, this will trigger a retry.
	 */
	public abstract boolean performJob(Context context, Intent job);

}
