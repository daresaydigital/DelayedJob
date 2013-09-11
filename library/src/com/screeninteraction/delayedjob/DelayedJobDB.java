package com.screeninteraction.delayedjob;

import java.net.URISyntaxException;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

class DelayedJobDB {

	private static final String DATABASE_NAME = "DelayedJobs.db";
	private static final int DATABASE_VERSION = 1;

	private static final String TABLE_JOBS = "Jobs";
	private static final String COLUMN_JOB_ID = "job_id";
	private static final String COLUMN_JOB_TIME = "job_time";
	private static final String COLUMN_JOB_HANDLER = "job_handler";
	private static final String COLUMN_JOB = "job";

	static class DelayedJobDBOpenHelper extends SQLiteOpenHelper {

		public DelayedJobDBOpenHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION,
					new DatabaseErrorHandler() {

						public void onCorruption(SQLiteDatabase dbObj) {
							Log.e(DATABASE_NAME, dbObj.toString());
						}
					});
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL("CREATE TABLE Jobs (job_id integer, job_time integer, job_handler text, job text)");
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			db.execSQL("drop table if exists Jobs");
			onCreate(db);
		}
	}

	static SQLiteDatabase mSQLiteDb;

	static synchronized SQLiteDatabase getDbInstance(Context c) {
		if (mSQLiteDb == null) {
			mSQLiteDb = new DelayedJobDBOpenHelper(c).getWritableDatabase();
		}
		return mSQLiteDb;
	}

	static void insertJob(Context c, Intent job) {
		ContentValues values = new ContentValues();
		values.put(COLUMN_JOB_ID,
				job.getExtras().getLong(DelayedJob.EXTRA_JOB_ID));
		values.put(COLUMN_JOB_TIME, System.currentTimeMillis()
				+ job.getExtras().getLong(DelayedJob.EXTRA_START_DELAY));
		values.put(COLUMN_JOB_HANDLER,
				job.getExtras().getString(DelayedJob.EXTRA_HANDLER_CLASS));
		values.put(COLUMN_JOB, job.toUri(0));
		getDbInstance(c).insert(TABLE_JOBS, null, values);
	}

	static Cursor getJobsExecutedBefore(Context c, long time) {
		return getDbInstance(c).query(TABLE_JOBS, null,
				COLUMN_JOB_TIME + "<" + time, null, null, null,
				COLUMN_JOB_TIME + " ASC");
	}

	static void deleteJobsWithHandlerAndId(Context c, String handler, long jobId) {
		getDbInstance(c).delete(
				TABLE_JOBS,
				COLUMN_JOB_ID + "=" + jobId + " AND " + COLUMN_JOB_HANDLER
						+ "=?", new String[] { handler });
	}

	static void deleteJobsExecutedBefore(Context c, long time) {
		getDbInstance(c).delete(TABLE_JOBS, COLUMN_JOB_TIME + "<" + time, null);
	}

	static Intent getJobFromCursor(Cursor c) {
		try {
			return Intent.parseUri(
					c.getString(c.getColumnIndexOrThrow(COLUMN_JOB)), 0);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		return null;
	}

}
