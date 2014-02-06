package com.deadrooster.slate.android.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;

import com.deadrooster.slate.android.model.Model;

public class DatabaseHelper extends SQLiteOpenHelper {

	private static final String DATABASE_NAME = "slate.db";
	private static final int DATABASE_VERSION = 1;
	
	public DatabaseHelper(Context context) {
		this(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	public DatabaseHelper(Context context, String name, CursorFactory factory, int version) {
		super(context, name, factory, version);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		createSchema(db);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// Delete all tables
		db.execSQL(DatabaseSchema.DROP_TABLE + Model.Entries.TABLE_NAME);
		// Create schema
		createSchema(db);
	}

	private void createSchema(SQLiteDatabase db) {
		db.execSQL(DatabaseSchema.CREATE_TABLE_ENTRIES);
	}
}
