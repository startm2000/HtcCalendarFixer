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
import java.util.HashMap;
import java.util.Locale;
import java.util.Set;
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

	private static HashMap<Long, Long> getCalendarAccountIDAndSync8(ContentResolver resolver) {
		HashMap<Long, Long> accountIDs = new HashMap<Long, Long>();
		Cursor cCalID = null;
		ContentProviderClient calendarsProvider = null;
		String where = CalendarContract.Calendars.ACCOUNT_TYPE + "='" + ACCOUNT_TYPE_GOOGLE + "'" + " AND " + CalendarContract.Calendars.IS_PRIMARY + " IS 1";
		try {
			calendarsProvider = resolver.acquireUnstableContentProviderClient(CalendarContract.Calendars.CONTENT_URI);
			if (calendarsProvider != null) {
				cCalID = calendarsProvider.query(CalendarContract.Calendars.CONTENT_URI, new String[]{CalendarContract.Calendars._ID, CalendarContract.Calendars.CAL_SYNC8},
						where , null, null);
			}
			if (cCalID != null && cCalID.moveToFirst()) {
				int idIndex = cCalID.getColumnIndex(CalendarContract.Calendars._ID);
				int sync8Index = cCalID.getColumnIndex(CalendarContract.Calendars.CAL_SYNC8);
				do {
					long accID = cCalID.getLong(idIndex);
					if (!accountIDs.containsKey(accID)) {
						accountIDs.put(accID, cCalID.getLong(sync8Index));
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
	
	/*private static ArrayList<Long> getExistenceGoogleCalendarAccountIDs(ContentResolver resolver) {
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
	}*/

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
	public static final byte STATUS_MEET_ISSUE_CAL_SYNC8 = 0x04;

	private static byte doCheckGoogleAccountCalendarEventStatus(ContentResolver resolver, long accountID) {
		Cursor cursor = null;
		ContentProviderClient calendarsProvider = null;

		String inCase = String.format("%s='%d'", CalendarContract.Events.CALENDAR_ID, accountID, Locale.US);

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

	/*private static boolean isGoogleAccountCalendarExist(ContentResolver resolver) {
		ArrayList<Long> accountIDs = getExistenceGoogleCalendarAccountIDs(resolver);
		return accountIDs != null && accountIDs.size() > 0;
	}*/

	public static HashMap<Long, String> getExistenceGoogleCalendarAccountIDAndNames(ContentResolver resolver) {
		HashMap<Long, String> accountNames = new HashMap<Long, String>();
		Cursor cCalID = null;
		ContentProviderClient calendarsProvider = null;
		try {
			calendarsProvider = resolver.acquireUnstableContentProviderClient(CalendarContract.Calendars.CONTENT_URI);
			if (calendarsProvider != null) {
				cCalID = calendarsProvider.query(CalendarContract.Calendars.CONTENT_URI, new String[]{CalendarContract.Calendars.ACCOUNT_NAME, CalendarContract.Calendars._ID},
						CalendarContract.Calendars.ACCOUNT_TYPE + "='" + ACCOUNT_TYPE_GOOGLE + "'", null, null);
			}
			if (cCalID != null && cCalID.moveToFirst()) {
				int nameIndex = cCalID.getColumnIndex(CalendarContract.Calendars.ACCOUNT_NAME);
				int idIndex = cCalID.getColumnIndex(CalendarContract.Calendars._ID);
				do {
					String accName = cCalID.getString(nameIndex);
					long accID = cCalID.getLong(idIndex);
					if (!accountNames.containsKey(accID)) {
						accountNames.put(accID, accName);
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

	public static void eraseGoogleCalendarSyncStatus(Context context, String accountName, long accId) {

		ContentProviderClient calendarsProvider = null;
		Cursor cursor = null;
		try {
			
			//Delete google account's calendar sync status
			ArrayList<ContentProviderOperation> calendarOperationList = new ArrayList<ContentProviderOperation>();
			ContentProviderOperation.Builder builder = null;
			
			

			calendarsProvider = context.getContentResolver().acquireUnstableContentProviderClient(CalendarContract.SyncState.CONTENT_URI);
			if (calendarsProvider != null) {
				String where = String.format("%1$s='%2$s' AND %3$s='%4$s'", CalendarContract.Calendars.ACCOUNT_TYPE, ACCOUNT_TYPE_GOOGLE, CalendarContract.Calendars.ACCOUNT_NAME, accountName, Locale.US);
				cursor = calendarsProvider.query(CalendarContract.SyncState.CONTENT_URI, 
						new String[] {CalendarContract.SyncState.DATA},
						where, null, null);
			}
			
			if (cursor == null || cursor.getCount() <= 0 || !cursor.moveToFirst()) {
				Log.w(TAG, "Can't find sync_status!");
				return;
			}

			String blob = new String(cursor.getBlob(0));
			if (blob == null || blob.length() <= 0) {
				Log.w(TAG, "Find nothing inside sync_status!");
				return;
			}
			
			String [] sep = blob.split("\"");
			if (sep == null || sep.length <= 0) {
				Log.w(TAG, "Find nothing inside sync_status!");
				return;
			}
			
			StringBuilder stringBuilder = new StringBuilder();
			
			for (int i = 0; i < sep.length; i++) {
				if (!sep[i].contains("@")) {
					stringBuilder.append(sep[i]);
				}
				if (i < sep.length - 1) {
					stringBuilder.append("\"");
				}
			}
			
			String valueString = stringBuilder.toString();
			byte [] value = valueString.getBytes();

			Uri.Builder builderu =  CalendarContract.SyncState.CONTENT_URI.buildUpon();
			builderu.appendQueryParameter(CalendarContract.Calendars.ACCOUNT_TYPE, ACCOUNT_TYPE_GOOGLE);
			builderu.appendQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME, accountName);
			builderu.appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true");

			builder = ContentProviderOperation.newUpdate(builderu.build());
			builder.withValue(CalendarContract.SyncState.DATA, value);
			
			builder.withSelection(CalendarContract.SyncState.ACCOUNT_TYPE + "=?", new String [] {ACCOUNT_TYPE_GOOGLE});
			calendarOperationList.add(builder.build());
			context.getContentResolver().applyBatch(CalendarContract.AUTHORITY, calendarOperationList);

			//Trigger resync
			Bundle extras = new Bundle();
			extras.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
			
			android.accounts.Account account = new android.accounts.Account(accountName, ACCOUNT_TYPE_GOOGLE);
			ContentResolver.requestSync(account, CalendarContract.AUTHORITY, extras);
			
			Log.w(TAG, String.format("Erase sync status for account : %d", accId, Locale.US));
			
		} catch (Exception e) {
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
	}

	public static String detectGoogleAccountCalendarEventMissing(Context context) {
		
		String toastMessage = "Confirm that";
		HashMap<Long, Long> accountIDs = getCalendarAccountIDAndSync8(context.getContentResolver());
		boolean hasGoogle = accountIDs.size() > 0;
		boolean hasExchange = isHtcExchangeCalendarAccountExist(context.getContentResolver());
		byte pcSyncStatus = isPCSyncStatusError(context.getContentResolver());
		
		if (!hasExchange) {
			toastMessage += "\n->There is no HtcExchange's calendar account!";
		} else {
			toastMessage += "\n->HtcExchange's calendar account found!";
		}
		
		if (!hasGoogle) {
			toastMessage += "\n->There is no Google's calendar account!";
		} else {
			toastMessage += "\n->Google's calendar account found!";
		}
		
		if ((pcSyncStatus & CalendarFixerUtil.STATUS_MEET_ISSUE_PC_SYNC) != 0) {
			toastMessage += "\n->PC sync's cal_sync1 is 0";
		} else {
			toastMessage += "\n->PC sync's cal_sync1 is not 0";
		}
		
		
		if (accountIDs == null || accountIDs.size() <= 0) {
			Log.w(TAG, "No Google account's calendar found");
			Toast.makeText(context, toastMessage, Toast.LENGTH_LONG).show();
			return toastMessage;
		}
		
		Set<Long> ids = accountIDs.keySet();
		long mailDeploy = getMailDeployTime();
		HashMap<Long, String> idNamePair = getExistenceGoogleCalendarAccountIDAndNames(context.getContentResolver());
		
		for (Long id : ids) {
			toastMessage += "\n\n->For account " + idNamePair.get(id);
			byte status = CalendarFixerUtil.doCheckGoogleAccountCalendarEventStatus(context.getContentResolver(), id);
			long cal_sync8 = accountIDs.get(id);
			String dateString = getDateString(cal_sync8);
			if (cal_sync8 > mailDeploy) {
				toastMessage += "\n->CAL_SYNC8 is " + dateString + ", could meet issue";
			} else {
				toastMessage += "\n->CAL_SYNC8 is " + dateString + ", should be safe";
			}
			
			if ((status & CalendarFixerUtil.STATUS_MEET_ISSUE_NO_EVENT) != 0) {
				
				toastMessage += "\n->Google account's Calendar events missing!";
				
				if ((status & CalendarFixerUtil.STATUS_HAVE_UNSYNC_EVENT) != 0) {
					toastMessage += "\n->There are also unsynced event(s).";
				} 
	
				if ((status & CalendarFixerUtil.STATUS_HAVE_NEW_EVENT) != 0) {
					toastMessage += "\n->There are also new event(s) after issue happened.";
				}
			} else {
				toastMessage += "\n->Google account's Calendar events are still there!";
			}
		}
		Toast.makeText(context, toastMessage, Toast.LENGTH_LONG).show();
		
		return toastMessage;
	}
	
	private static String getDateString(long tims) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
		return sdf.format(new Date(tims));
	}
	
	public static String fixGoogleAccountCalendarEventMissing(Context context) {
		String toastMessage = "Processing";
		HashMap<Long, Long> accountIDs = getCalendarAccountIDAndSync8(context.getContentResolver());
		if (accountIDs == null || accountIDs.size() <= 0) {
			Log.w(TAG, "No Google account's calendar found");
			toastMessage += "\n->No Google account's calendar found";
			return toastMessage;
		} else {
			toastMessage += "\n->Find Google account";
		}
		
		byte pcSyncStatus = isPCSyncStatusError(context.getContentResolver());
		if ((pcSyncStatus & CalendarFixerUtil.STATUS_MEET_ISSUE_PC_SYNC) == 0) {
			Log.w(TAG, "PcSync Status is not 0");
			toastMessage += "\n->PcSync Status is not 0";
			return toastMessage;
		} else {
			toastMessage += "\n->PcSync Status is 0";
		}
		
		long mailDeploy = getMailDeployTime();
		Set<Long> ids = accountIDs.keySet();
		
		HashMap<Long, String> idNamePair = getExistenceGoogleCalendarAccountIDAndNames(context.getContentResolver());
		
		
		for (Long id : ids) {
			toastMessage += "\n\n->For account " + idNamePair.get(id);
			long cal_sync8 = accountIDs.get(id);
			String dateString = getDateString(cal_sync8);
			if (cal_sync8 < mailDeploy) {
				String temp = String.format("CAL_SYNC8 is %s, should be safe", dateString, Locale.US);
				Log.w(TAG, temp);
				toastMessage += "\n->" + temp;
				continue;
			} else {
				String temp = String.format("CAL_SYNC8 is %s, which is not ok", dateString, Locale.US);
				Log.w(TAG, temp);
				toastMessage += "\n->" + temp;
			}
			
			byte status = CalendarFixerUtil.doCheckGoogleAccountCalendarEventStatus(context.getContentResolver(), id);
			if ((status & CalendarFixerUtil.STATUS_MEET_ISSUE_NO_EVENT) != 0) {
				Log.w(TAG, String.format("Account %d meet issue, start fixing", id, Locale.US));
				toastMessage += "\n->Event is missing, trigger fix";
				eraseGoogleCalendarSyncStatus(context, idNamePair.get(id), id);
			}
		}
		
		Toast.makeText(context, toastMessage, Toast.LENGTH_LONG).show();
		return toastMessage;
	}
}
