package com.example.test;

import android.content.ContentProviderClient;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.provider.CalendarContract;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;


import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Created by luolai on 2017/5/22.
 */
public class CalendarFixerUtil {
	private static boolean DEBUG = true;
	private static final String TAG = "CalendarFixerUtil";
	private static final String ACCOUNT_TYPE_EXCHANGE = "com.htc.android.mail.eas";
	private static final String ACCOUNT_TYPE_GOOGLE = "com.google";
	private static final String ACCOUNT_TYPE_PCSYNC = "com.htc.pcsc";

	private static boolean isHtcExchangeCalendarAccountExist(ContentResolver resolver) {
		Cursor cursor = null;
		ContentProviderClient calendarsProvider = null;
		try {
			calendarsProvider = resolver.acquireUnstableContentProviderClient(CalendarContract.Calendars.CONTENT_URI);
			if (calendarsProvider != null) {
				cursor = calendarsProvider.query(CalendarContract.Calendars.CONTENT_URI, new String[]{CalendarContract.Calendars.ACCOUNT_NAME},
						CalendarContract.Calendars.ACCOUNT_TYPE + "='"+ ACCOUNT_TYPE_EXCHANGE + "'", null, null);
			}
			if (cursor != null && cursor.getCount() > 0) {
				return true;
			}
		} catch (IllegalArgumentException e) {
			cursor = null;

		} catch (RemoteException e) {
			if (!TextUtils.isEmpty(e.getMessage())) {
				Log.d(TAG, e.getMessage());
			}
			e.printStackTrace();
		} finally {
			if (null != calendarsProvider) {
				calendarsProvider.release();
				calendarsProvider = null;
			}

			if (null != cursor && !cursor.isClosed()) {
				cursor.close();
				cursor = null;
			}
		}
		return false;
	}
	
	private static byte isPCSyncStatusError(ContentResolver resolver) {
		Cursor cCalID = null;
		ContentProviderClient calendarsProvider = null;
		try {
			calendarsProvider = resolver.acquireUnstableContentProviderClient(CalendarContract.Calendars.CONTENT_URI);
			if (calendarsProvider != null) {
				cCalID = calendarsProvider.query(CalendarContract.Calendars.CONTENT_URI, new String[]{CalendarContract.Calendars.CAL_SYNC1},
						CalendarContract.Calendars.ACCOUNT_TYPE + "='"+ ACCOUNT_TYPE_PCSYNC + "'" , null, null);
			}
			if (cCalID != null && cCalID.moveToFirst()) {
				String value = cCalID.getString(0);
				if ("0".equals(value)) {
					return STATUS_MEET_ISSUE_PC_SYNC;
				}
			}
		} catch (IllegalArgumentException e) {
			cCalID = null;

		} catch (RemoteException e) {
			if (!TextUtils.isEmpty(e.getMessage())) {
				Log.d(TAG, e.getMessage());
			}
			e.printStackTrace();
		} finally {
			if (null != calendarsProvider) {
				calendarsProvider.release();
				calendarsProvider = null;
			}

			if (null != cCalID) {
				cCalID.close();
				cCalID = null;
			}
		}
		return 0;
	}

	private static ArrayList<Long> getCalendarAccountIDs(ContentResolver resolver, String where) {
		ArrayList<Long> accountIDs = new ArrayList<>();
		Cursor cCalID = null;
		ContentProviderClient calendarsProvider = null;
		try {
			calendarsProvider = resolver.acquireUnstableContentProviderClient(CalendarContract.Calendars.CONTENT_URI);
			if (calendarsProvider != null) {
				cCalID = calendarsProvider.query(CalendarContract.Calendars.CONTENT_URI, new String[]{CalendarContract.Calendars._ID},
						where , null, null);
			}
			if (cCalID != null && cCalID.moveToFirst()) {
				do {
					long accID = cCalID.getLong(0);
					if (!accountIDs.contains(accID)) {
						accountIDs.add(accID);
					}
				} while (cCalID.moveToNext());
			}
		} catch (IllegalArgumentException e) {
			cCalID = null;

		} catch (RemoteException e) {
			if (!TextUtils.isEmpty(e.getMessage())) {
				Log.d(TAG, e.getMessage());
			}
			e.printStackTrace();
		} finally {
			if (null != calendarsProvider) {
				calendarsProvider.release();
				calendarsProvider = null;
			}

			if (null != cCalID) {
				cCalID.close();
				cCalID = null;
			}
		}
		return accountIDs;
	}
	
	private static ArrayList<Long> getExistenceGoogleCalendarAccountIDs(ContentResolver resolver) {
		return getCalendarAccountIDs(resolver, CalendarContract.Calendars.ACCOUNT_TYPE + "='" + ACCOUNT_TYPE_GOOGLE + "'" + " AND " + CalendarContract.Calendars.IS_PRIMARY + " IS 1");
	}

	private static String getWhereClauseForGoogleAccountCalendars(ContentResolver resolver) {
		
		
		ArrayList<Long> accountIDs = getExistenceGoogleCalendarAccountIDs(resolver);
		if (accountIDs == null || accountIDs.size() <= 0) {
			Log.w(TAG, "No Google account's calendar found");
			return null;
		}

		String inCase = "";
		StringBuilder inCaseTemp = new StringBuilder(inCase);
		inCaseTemp.append(CalendarContract.Events.CALENDAR_ID).append(" IN (");
		for(Long id : accountIDs) {
			inCaseTemp.append("'").append(id).append("',");
		}
		if (inCaseTemp.length() > 0) {
			inCase = inCaseTemp.substring(0, inCaseTemp.length() - 1);
			inCase = inCase + ")";
		} else {
			return null;
		}
		return inCase;
	}

	private static long getMailDeployTime() {
		Calendar calendar = Calendar.getInstance();
		calendar.set(2017, Calendar.APRIL, 23);
		return calendar.getTimeInMillis();
	}

	public static final byte STATUS_MEET_ISSUE_NO_EVENT = 0x01;
	public static final byte STATUS_MEET_ISSUE_PC_SYNC = 0x02;
	public static final byte STATUS_NO_EXCHANGE = 0x04;
	public static final byte STATUS_NO_GOOGLE = 0x08;
	public static final byte STATUS_HAVE_UNSYNC_EVENT = 0x10;
	public static final byte STATUS_HAVE_NEW_EVENT = 0x20;

	private static byte doCheckGoogleAccountCalendarEventStatus(ContentResolver resolver) {
		Cursor cursor = null;
		ContentProviderClient calendarsProvider = null;
		ArrayList<Long> accountIDs = getExistenceGoogleCalendarAccountIDs(resolver);
		if (accountIDs == null || accountIDs.size() <= 0) {
			Log.w(TAG, "No Google account's calendar found");
			return 0;
		}

		String inCase = getWhereClauseForGoogleAccountCalendars(resolver);

		if (DEBUG) Log.d(TAG, "isGoogleAccountCalendarEventExist, inCase = " + inCase);
		if (TextUtils.isEmpty(inCase)) {
			return 0;
		}
		try {
			calendarsProvider = resolver.acquireUnstableContentProviderClient(CalendarContract.Events.CONTENT_URI);
			if (calendarsProvider != null) {
				cursor = calendarsProvider.query(CalendarContract.Events.CONTENT_URI, new String[]{CalendarContract.Events.SYNC_DATA5}, inCase, null, null);
			}
			
			if (cursor == null || cursor.getCount() <= 0 || !cursor.moveToFirst()) {
				return STATUS_MEET_ISSUE_NO_EVENT;
				
			} else {

				long mailDeployTime = getMailDeployTime();
				byte result = 0;
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
				sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
				do {
					String createDate = cursor.getString(0);

					if (TextUtils.isEmpty(createDate)) {
						result |= STATUS_HAVE_UNSYNC_EVENT;
					} else {

						try {
							Date date = sdf.parse(createDate);
							if (date != null) {
								if (mailDeployTime > date.getTime()) {
									if (DEBUG)
										Log.d(TAG, "Found an event's create time is older than Mail's deploy time, " + createDate);
									return 0;
								} else {
									result |= STATUS_HAVE_NEW_EVENT;
								}
							}
						} catch (java.text.ParseException e) {
							if (DEBUG && e != null) Log.d(TAG, e.toString());
						}
					}
				} while (cursor.moveToNext());
				if (DEBUG) Log.d(TAG, "isGoogleAccountCalendarEventExist, check completed, no matched Google calendar event found" + inCase);
				return (byte)(STATUS_MEET_ISSUE_NO_EVENT | result);
			}
		} catch (IllegalArgumentException e) {
			cursor = null;

		} catch (RemoteException e) {
			if (!TextUtils.isEmpty(e.getMessage())) {
				Log.d(TAG, e.getMessage());
			}
			e.printStackTrace();
		} finally {
			if (null != calendarsProvider) {
				calendarsProvider.release();
				calendarsProvider = null;
			}

			if (null != cursor && !cursor.isClosed()) {
				cursor.close();
				cursor = null;
			}
		}
		return 0;
	}

	private static boolean isGoogleAccountCalendarExist(ContentResolver resolver) {
		ArrayList<Long> accountIDs = getExistenceGoogleCalendarAccountIDs(resolver);
		return accountIDs != null && accountIDs.size() > 0;
	}

	public static byte checkGoogleAccountCalendarEventStatus(ContentResolver resolver, boolean byEvent) {
		boolean hasExchange = isHtcExchangeCalendarAccountExist(resolver);
		boolean hasGoogle = isGoogleAccountCalendarExist(resolver);
		if (hasExchange && hasGoogle) {
			if (byEvent) {
				return doCheckGoogleAccountCalendarEventStatus(resolver);
			} else {
				return isPCSyncStatusError(resolver);
			}
			
		} else {
			return (byte) ((hasExchange ? 0 : STATUS_NO_EXCHANGE) | (hasGoogle ? 0 : STATUS_NO_GOOGLE));
		}
	}

	public static ArrayList<String> getExistenceGoogleCalendarAccountNames(ContentResolver resolver) {
		ArrayList<String> accountNames = new ArrayList<>();
		Cursor cCalID = null;
		ContentProviderClient calendarsProvider = null;
		try {
			calendarsProvider = resolver.acquireUnstableContentProviderClient(CalendarContract.Calendars.CONTENT_URI);
			if (calendarsProvider != null) {
				cCalID = calendarsProvider.query(CalendarContract.Calendars.CONTENT_URI, new String[]{CalendarContract.Calendars.ACCOUNT_NAME},
						CalendarContract.Calendars.ACCOUNT_TYPE + "='" + ACCOUNT_TYPE_GOOGLE + "'", null, null);
			}
			if (cCalID != null && cCalID.moveToFirst()) {
				do {
					String accName = cCalID.getString(0);
					if (!accountNames.contains(accName)) {
						accountNames.add(accName);
					}
				} while (cCalID.moveToNext());
			}
		} catch (IllegalArgumentException e) {
			cCalID = null;

		} catch (RemoteException e) {
			if (!TextUtils.isEmpty(e.getMessage())) {
				Log.d(TAG, e.getMessage());
			}
			e.printStackTrace();
		} finally {
			if (null != calendarsProvider) {
				calendarsProvider.release();
				calendarsProvider = null;
			}

			if (null != cCalID) {
				cCalID.close();
				cCalID = null;
			}
		}
		return accountNames;
	}

	public static void eraseGoogleCalendarSyncStatus(Context context) {

		ArrayList<String> accountNames = getExistenceGoogleCalendarAccountNames(context.getContentResolver());
		if (accountNames == null || accountNames.size() <= 0) {
			Log.w(TAG, "No Google account's calendar found");
			return;
		}

		try {
			//Delete google account's calendar sync status
			ArrayList<ContentProviderOperation> calendarOperationList = new ArrayList<ContentProviderOperation>();
			ContentProviderOperation.Builder builder = null;

			Uri.Builder builderu =  CalendarContract.SyncState.CONTENT_URI.buildUpon();
			builderu.appendQueryParameter(CalendarContract.Calendars.ACCOUNT_TYPE, ACCOUNT_TYPE_GOOGLE);
			builderu.appendQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME, accountNames.get(0));
			builderu.appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true");

			builder = ContentProviderOperation.newUpdate(builderu.build());
			builder.withSelection(CalendarContract.SyncState.ACCOUNT_TYPE + "=?", new String [] {ACCOUNT_TYPE_GOOGLE});
			builder.withValue(CalendarContract.SyncState.DATA, null);
			calendarOperationList.add(builder.build());

			context.getContentResolver().applyBatch(CalendarContract.AUTHORITY, calendarOperationList);

			//Trigger resync
			Bundle extras = new Bundle();
			extras.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
			for (String accName : accountNames) {
				android.accounts.Account account = new android.accounts.Account(accName, ACCOUNT_TYPE_GOOGLE);
				ContentResolver.requestSync(account, CalendarContract.AUTHORITY, extras);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static boolean detectGoogleAccountCalendarEventMissing(Context context, boolean byEvent) {
		byte status = CalendarFixerUtil.checkGoogleAccountCalendarEventStatus(context.getContentResolver(), byEvent);
		if ((status & CalendarFixerUtil.STATUS_MEET_ISSUE_NO_EVENT) != 0) {
			String toastMessage = "Confirm that\n1.There is HtcExchange's calendar account\n2.Google account's Calendar events missing!";
			if ((status & CalendarFixerUtil.STATUS_HAVE_UNSYNC_EVENT) != 0) {
				toastMessage += "\nThere are also unsynced event(s).";
			} 

			if ((status & CalendarFixerUtil.STATUS_HAVE_NEW_EVENT) != 0) {
				toastMessage += "\nThere are also new event(s) after issue happened.";
			}
			Toast.makeText(context, toastMessage, Toast.LENGTH_SHORT).show();
			return true;
		} else if ((status & CalendarFixerUtil.STATUS_MEET_ISSUE_PC_SYNC) != 0) {

			Toast.makeText(context, "Confirm that\n1.There is HtcExchange's calendar account\n2.PC sync's cal_sync1 is 0", Toast.LENGTH_SHORT).show();
		}
		return false;
	}
	
	public static boolean fixGoogleAccountCalendarEventMissing(Context context) {
		CalendarFixerUtil.eraseGoogleCalendarSyncStatus(context);
		Toast.makeText(context, "Fix and sync triggered", Toast.LENGTH_SHORT).show();
		return true;
	}
}
