package com.example.webdownloader.db;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class PageDao_Impl implements PageDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<Page> __insertionAdapterOfPage;

  private final EntityDeletionOrUpdateAdapter<Page> __deletionAdapterOfPage;

  public PageDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfPage = new EntityInsertionAdapter<Page>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `pages` (`id`,`title`,`url`,`filePath`,`timestamp`,`iconPath`,`faviconUrl`,`fileSize`,`tags`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Page entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getTitle());
        statement.bindString(3, entity.getUrl());
        statement.bindString(4, entity.getFilePath());
        statement.bindLong(5, entity.getTimestamp());
        if (entity.getIconPath() == null) {
          statement.bindNull(6);
        } else {
          statement.bindString(6, entity.getIconPath());
        }
        if (entity.getFaviconUrl() == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, entity.getFaviconUrl());
        }
        statement.bindLong(8, entity.getFileSize());
        if (entity.getTags() == null) {
          statement.bindNull(9);
        } else {
          statement.bindString(9, entity.getTags());
        }
      }
    };
    this.__deletionAdapterOfPage = new EntityDeletionOrUpdateAdapter<Page>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `pages` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Page entity) {
        statement.bindLong(1, entity.getId());
      }
    };
  }

  @Override
  public Object insertPage(final Page page, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfPage.insert(page);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deletePage(final Page page, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfPage.handle(page);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object getAllPages(final Continuation<? super List<Page>> $completion) {
    final String _sql = "SELECT * FROM pages ORDER BY timestamp DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<Page>>() {
      @Override
      @NonNull
      public List<Page> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "url");
          final int _cursorIndexOfFilePath = CursorUtil.getColumnIndexOrThrow(_cursor, "filePath");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfIconPath = CursorUtil.getColumnIndexOrThrow(_cursor, "iconPath");
          final int _cursorIndexOfFaviconUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "faviconUrl");
          final int _cursorIndexOfFileSize = CursorUtil.getColumnIndexOrThrow(_cursor, "fileSize");
          final int _cursorIndexOfTags = CursorUtil.getColumnIndexOrThrow(_cursor, "tags");
          final List<Page> _result = new ArrayList<Page>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Page _item;
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpUrl;
            _tmpUrl = _cursor.getString(_cursorIndexOfUrl);
            final String _tmpFilePath;
            _tmpFilePath = _cursor.getString(_cursorIndexOfFilePath);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final String _tmpIconPath;
            if (_cursor.isNull(_cursorIndexOfIconPath)) {
              _tmpIconPath = null;
            } else {
              _tmpIconPath = _cursor.getString(_cursorIndexOfIconPath);
            }
            final String _tmpFaviconUrl;
            if (_cursor.isNull(_cursorIndexOfFaviconUrl)) {
              _tmpFaviconUrl = null;
            } else {
              _tmpFaviconUrl = _cursor.getString(_cursorIndexOfFaviconUrl);
            }
            final long _tmpFileSize;
            _tmpFileSize = _cursor.getLong(_cursorIndexOfFileSize);
            final String _tmpTags;
            if (_cursor.isNull(_cursorIndexOfTags)) {
              _tmpTags = null;
            } else {
              _tmpTags = _cursor.getString(_cursorIndexOfTags);
            }
            _item = new Page(_tmpId,_tmpTitle,_tmpUrl,_tmpFilePath,_tmpTimestamp,_tmpIconPath,_tmpFaviconUrl,_tmpFileSize,_tmpTags);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getPageByUrl(final String url, final Continuation<? super Page> $completion) {
    final String _sql = "SELECT * FROM pages WHERE url = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, url);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Page>() {
      @Override
      @Nullable
      public Page call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "url");
          final int _cursorIndexOfFilePath = CursorUtil.getColumnIndexOrThrow(_cursor, "filePath");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfIconPath = CursorUtil.getColumnIndexOrThrow(_cursor, "iconPath");
          final int _cursorIndexOfFaviconUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "faviconUrl");
          final int _cursorIndexOfFileSize = CursorUtil.getColumnIndexOrThrow(_cursor, "fileSize");
          final int _cursorIndexOfTags = CursorUtil.getColumnIndexOrThrow(_cursor, "tags");
          final Page _result;
          if (_cursor.moveToFirst()) {
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpUrl;
            _tmpUrl = _cursor.getString(_cursorIndexOfUrl);
            final String _tmpFilePath;
            _tmpFilePath = _cursor.getString(_cursorIndexOfFilePath);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final String _tmpIconPath;
            if (_cursor.isNull(_cursorIndexOfIconPath)) {
              _tmpIconPath = null;
            } else {
              _tmpIconPath = _cursor.getString(_cursorIndexOfIconPath);
            }
            final String _tmpFaviconUrl;
            if (_cursor.isNull(_cursorIndexOfFaviconUrl)) {
              _tmpFaviconUrl = null;
            } else {
              _tmpFaviconUrl = _cursor.getString(_cursorIndexOfFaviconUrl);
            }
            final long _tmpFileSize;
            _tmpFileSize = _cursor.getLong(_cursorIndexOfFileSize);
            final String _tmpTags;
            if (_cursor.isNull(_cursorIndexOfTags)) {
              _tmpTags = null;
            } else {
              _tmpTags = _cursor.getString(_cursorIndexOfTags);
            }
            _result = new Page(_tmpId,_tmpTitle,_tmpUrl,_tmpFilePath,_tmpTimestamp,_tmpIconPath,_tmpFaviconUrl,_tmpFileSize,_tmpTags);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object searchPages(final String query,
      final Continuation<? super List<Page>> $completion) {
    final String _sql = "SELECT * FROM pages WHERE title LIKE '%' || ? || '%' OR url LIKE '%' || ? || '%' ORDER BY timestamp DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindString(_argIndex, query);
    _argIndex = 2;
    _statement.bindString(_argIndex, query);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<Page>>() {
      @Override
      @NonNull
      public List<Page> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "url");
          final int _cursorIndexOfFilePath = CursorUtil.getColumnIndexOrThrow(_cursor, "filePath");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfIconPath = CursorUtil.getColumnIndexOrThrow(_cursor, "iconPath");
          final int _cursorIndexOfFaviconUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "faviconUrl");
          final int _cursorIndexOfFileSize = CursorUtil.getColumnIndexOrThrow(_cursor, "fileSize");
          final int _cursorIndexOfTags = CursorUtil.getColumnIndexOrThrow(_cursor, "tags");
          final List<Page> _result = new ArrayList<Page>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Page _item;
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpUrl;
            _tmpUrl = _cursor.getString(_cursorIndexOfUrl);
            final String _tmpFilePath;
            _tmpFilePath = _cursor.getString(_cursorIndexOfFilePath);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final String _tmpIconPath;
            if (_cursor.isNull(_cursorIndexOfIconPath)) {
              _tmpIconPath = null;
            } else {
              _tmpIconPath = _cursor.getString(_cursorIndexOfIconPath);
            }
            final String _tmpFaviconUrl;
            if (_cursor.isNull(_cursorIndexOfFaviconUrl)) {
              _tmpFaviconUrl = null;
            } else {
              _tmpFaviconUrl = _cursor.getString(_cursorIndexOfFaviconUrl);
            }
            final long _tmpFileSize;
            _tmpFileSize = _cursor.getLong(_cursorIndexOfFileSize);
            final String _tmpTags;
            if (_cursor.isNull(_cursorIndexOfTags)) {
              _tmpTags = null;
            } else {
              _tmpTags = _cursor.getString(_cursorIndexOfTags);
            }
            _item = new Page(_tmpId,_tmpTitle,_tmpUrl,_tmpFilePath,_tmpTimestamp,_tmpIconPath,_tmpFaviconUrl,_tmpFileSize,_tmpTags);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
