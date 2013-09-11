package com.screeninteraction.delayedjob;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Handler;

public class DelayedJob {

	private static Context mApplicationContext;
	private static Handler mHandler;

	/**
	 * 
	 * @param context
	 *            Delayed job will grab the applciation context from the context
	 *            parameter so you can send in any context teid to your
	 *            application here.
	 */
	public static void init(Context context) {
		if (mApplicationContext != null) {
			throw new IllegalStateException("You cannot init DelayedJob twice");
		}
		mApplicationContext = context.getApplicationContext();
		mHandler = new Handler();
		performReadyJobs();
	}

	/**
	 * 
	 * @param handler
	 *            The handler the job is connected to
	 * @param jobId
	 *            The id of the job
	 */
	public static void removeJobs(Class<? extends DelayedJobHandler> handler,
			long jobId) {
		if (mApplicationContext == null) {
			throw new IllegalStateException(
					"DelayedJob does not have a context. Please call DelayedJob.init() att the start of your application");
		}
		DelayedJobDB.deleteJobsWithHandlerAndId(mApplicationContext,
				handler.getName(), jobId);
	}

	/**
	 * Used as a parameter to withTimeOut() too indicate that the job should
	 * never time out.
	 */
	public static final long NO_TIMEOUT = -1;

	static final String EXTRA_HANDLER_CLASS = "extra_handler_class";
	static final String EXTRA_JOB_ID = "extra_job_id";
	static final String EXTRA_START_DELAY = "extra_start_delay";
	static final String EXTRA_RETRY_COUNT = "extra_retry_count";
	static final String EXTRA_RETRY_DELAY = "extra_retry_delay";
	static final String EXTRA_TIMEOUT = "extra_timeout";

	private Intent mData;
	private boolean mReplacePrevious;

	/**
	 * 
	 * @param handler
	 *            The class that should do the performing of the job.
	 * @param job
	 *            The job.
	 */
	public DelayedJob(Class<? extends DelayedJobHandler> handler, Intent job) {
		mData = job;
		mData.putExtra(EXTRA_HANDLER_CLASS, handler.getName());

		// set default values
		withId(-1);
		withDelay(0);
		withRetryCount(0);
		withRetryDelay(0);
		withTimeOut(NO_TIMEOUT);
		replacePrevious(false);
	}

	/**
	 * 
	 * @param id
	 *            The id of the job. The id is tied to the handler, this means
	 *            that you can use the same id for different handlers without
	 *            them effecting each other.
	 * 
	 * @return The current DelayedJob instance.
	 */
	public DelayedJob withId(long id) {
		mData.putExtra(EXTRA_JOB_ID, id);
		return this;
	}

	/**
	 * 
	 * @param delayInMillis
	 *            The amount to delay the first perform call of this job. A
	 *            parameter of 0 will perform the job immediately.
	 * 
	 * @return The current DelayedJob instance.
	 */
	public DelayedJob withDelay(long delayInMillis) {
		if (delayInMillis < 0) {
			throw new IllegalArgumentException(
					"A delay less than 0 makes not sense, try again!");
		}
		mData.putExtra(EXTRA_START_DELAY, delayInMillis);
		return this;
	}

	/**
	 * 
	 * @param timeOut
	 *            The time in milliseconds at which this job is no longer valid.
	 *            System.currentTimeMillis() + 24 * 60 * 60 * 10000 will
	 *            invalidate the job one day from now.
	 * 
	 * 
	 * @return The current DelayedJob instance.
	 */
	public DelayedJob withTimeOut(long timeOut) {
		if (timeOut != NO_TIMEOUT && timeOut < System.currentTimeMillis()) {
			throw new IllegalArgumentException(
					"A timeOut less than this moment (System.currentTimeMillis) makes not sense, try again! If you don't want a timeout, set it to DelayedJob.NO_TIMEOUT");
		}
		mData.putExtra(EXTRA_TIMEOUT, timeOut);
		return this;
	}

	/**
	 * 
	 * @param retryCount
	 *            The number of times to retry this job.
	 * 
	 * @return The current DelayedJob instance.
	 */
	public DelayedJob withRetryCount(int retryCount) {
		if (retryCount < 0) {
			throw new IllegalArgumentException(
					"A retryCount less than 0 makes not sense, try again!");
		}
		mData.putExtra(EXTRA_RETRY_COUNT, retryCount);
		return this;
	}

	/**
	 * 
	 * @param delayInMillis
	 *            The amount of time to delay retries of the job.
	 * 
	 * @return The current DelayedJob instance.
	 */
	public DelayedJob withRetryDelay(long delayInMillis) {
		if (delayInMillis < 0) {
			throw new IllegalArgumentException(
					"A delay less than 0 makes not sense, try again!");
		}
		mData.putExtra(EXTRA_RETRY_DELAY, delayInMillis);
		return this;
	}

	/**
	 * 
	 * @param replacePrevious
	 *            A boolean indicating if all the pending jobs with the same id
	 *            and handler should be removed.
	 * 
	 * @return The current DelayedJob instance.
	 */
	public DelayedJob replacePrevious(boolean replacePrevious) {
		mReplacePrevious = replacePrevious;
		return this;
	}

	/**
	 * Perform the job at the specified point in time with the given settings.
	 */
	public void perform() {
		if (mApplicationContext == null) {
			throw new IllegalStateException(
					"DelayedJob does not have a context. Please call DelayedJob.init() att the start of your application");
		}
		if (mReplacePrevious) {
			DelayedJobDB.deleteJobsWithHandlerAndId(mApplicationContext, mData
					.getExtras().getString(EXTRA_HANDLER_CLASS), mData
					.getExtras().getLong(EXTRA_JOB_ID));
			// TODO also remove timer for this job
		}
		if (mData.getExtras().getLong(EXTRA_START_DELAY) == 0) {
			new JobExecutor().execute(mData);
		} else {
			DelayedJobDB.insertJob(mApplicationContext, mData);
			mHandler.postDelayed(new Runnable() {

				@Override
				public void run() {
					performReadyJobs();
				}
			}, mData.getExtras().getLong(EXTRA_START_DELAY));
		}
	}

	private static class JobExecutor extends AsyncTask<Intent, Void, Void> {
		@Override
		protected Void doInBackground(Intent... jobs) {
			for (Intent job : jobs) {
				performJob(job);
			}
			return null;
		}

		private static void performJob(Intent job) {
			try {
				boolean completed = ((DelayedJobHandler) Class.forName(
						job.getStringExtra(EXTRA_HANDLER_CLASS)).newInstance())
						.performJob(mApplicationContext, job);
				if (!completed) {
					int retryCount = job.getExtras().getInt(EXTRA_RETRY_COUNT);
					if (retryCount > 0) {
						long retryDelay = job.getExtras().getLong(
								EXTRA_RETRY_DELAY);
						job.putExtra(EXTRA_RETRY_COUNT, retryCount - 1);
						job.putExtra(EXTRA_START_DELAY, retryDelay);
						// post it to try again
						mHandler.postDelayed(new Runnable() {

							@Override
							public void run() {
								performReadyJobs();
							}
						}, retryDelay);
						DelayedJobDB.insertJob(mApplicationContext, job);
					}
				}
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
		}
	}

	private static void performReadyJobs() {
		long currTime = System.currentTimeMillis();
		Cursor c = DelayedJobDB.getJobsExecutedBefore(mApplicationContext,
				currTime);
		List<Intent> jobsToPerform = new ArrayList<Intent>();
		while (c.moveToNext()) {
			Intent job = DelayedJobDB.getJobFromCursor(c);
			long jobTimeout = job.getExtras().getLong(EXTRA_TIMEOUT);
			if (job != null
					&& (jobTimeout == NO_TIMEOUT || jobTimeout > System
							.currentTimeMillis())) {
				jobsToPerform.add(job);
			}
		}
		new JobExecutor().execute(jobsToPerform
				.toArray(new Intent[jobsToPerform.size()]));
		c.close();
		DelayedJobDB.deleteJobsExecutedBefore(mApplicationContext, currTime);
	}

}
