package com.example.mentra.core.data.local.dao;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.example.mentra.core.data.local.entity.ShellScriptEntity;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Long;
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
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class ShellScriptDao_Impl implements ShellScriptDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<ShellScriptEntity> __insertionAdapterOfShellScriptEntity;

  private final EntityDeletionOrUpdateAdapter<ShellScriptEntity> __deletionAdapterOfShellScriptEntity;

  private final EntityDeletionOrUpdateAdapter<ShellScriptEntity> __updateAdapterOfShellScriptEntity;

  private final SharedSQLiteStatement __preparedStmtOfIncrementExecutionCount;

  public ShellScriptDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfShellScriptEntity = new EntityInsertionAdapter<ShellScriptEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `shell_scripts` (`id`,`name`,`content`,`description`,`createdAt`,`updatedAt`,`executionCount`,`lastExecuted`) VALUES (nullif(?, 0),?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final ShellScriptEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getName());
        statement.bindString(3, entity.getContent());
        if (entity.getDescription() == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.getDescription());
        }
        statement.bindLong(5, entity.getCreatedAt());
        statement.bindLong(6, entity.getUpdatedAt());
        statement.bindLong(7, entity.getExecutionCount());
        if (entity.getLastExecuted() == null) {
          statement.bindNull(8);
        } else {
          statement.bindLong(8, entity.getLastExecuted());
        }
      }
    };
    this.__deletionAdapterOfShellScriptEntity = new EntityDeletionOrUpdateAdapter<ShellScriptEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `shell_scripts` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final ShellScriptEntity entity) {
        statement.bindLong(1, entity.getId());
      }
    };
    this.__updateAdapterOfShellScriptEntity = new EntityDeletionOrUpdateAdapter<ShellScriptEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `shell_scripts` SET `id` = ?,`name` = ?,`content` = ?,`description` = ?,`createdAt` = ?,`updatedAt` = ?,`executionCount` = ?,`lastExecuted` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final ShellScriptEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getName());
        statement.bindString(3, entity.getContent());
        if (entity.getDescription() == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.getDescription());
        }
        statement.bindLong(5, entity.getCreatedAt());
        statement.bindLong(6, entity.getUpdatedAt());
        statement.bindLong(7, entity.getExecutionCount());
        if (entity.getLastExecuted() == null) {
          statement.bindNull(8);
        } else {
          statement.bindLong(8, entity.getLastExecuted());
        }
        statement.bindLong(9, entity.getId());
      }
    };
    this.__preparedStmtOfIncrementExecutionCount = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE shell_scripts SET executionCount = executionCount + 1, lastExecuted = ? WHERE id = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insertScript(final ShellScriptEntity script,
      final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfShellScriptEntity.insertAndReturnId(script);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteScript(final ShellScriptEntity script,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfShellScriptEntity.handle(script);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateScript(final ShellScriptEntity script,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfShellScriptEntity.handle(script);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object incrementExecutionCount(final long scriptId, final long timestamp,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfIncrementExecutionCount.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, timestamp);
        _argIndex = 2;
        _stmt.bindLong(_argIndex, scriptId);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfIncrementExecutionCount.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<ShellScriptEntity>> getAllScripts() {
    final String _sql = "SELECT * FROM shell_scripts ORDER BY name ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"shell_scripts"}, new Callable<List<ShellScriptEntity>>() {
      @Override
      @NonNull
      public List<ShellScriptEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfContent = CursorUtil.getColumnIndexOrThrow(_cursor, "content");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final int _cursorIndexOfExecutionCount = CursorUtil.getColumnIndexOrThrow(_cursor, "executionCount");
          final int _cursorIndexOfLastExecuted = CursorUtil.getColumnIndexOrThrow(_cursor, "lastExecuted");
          final List<ShellScriptEntity> _result = new ArrayList<ShellScriptEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ShellScriptEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpContent;
            _tmpContent = _cursor.getString(_cursorIndexOfContent);
            final String _tmpDescription;
            if (_cursor.isNull(_cursorIndexOfDescription)) {
              _tmpDescription = null;
            } else {
              _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            final int _tmpExecutionCount;
            _tmpExecutionCount = _cursor.getInt(_cursorIndexOfExecutionCount);
            final Long _tmpLastExecuted;
            if (_cursor.isNull(_cursorIndexOfLastExecuted)) {
              _tmpLastExecuted = null;
            } else {
              _tmpLastExecuted = _cursor.getLong(_cursorIndexOfLastExecuted);
            }
            _item = new ShellScriptEntity(_tmpId,_tmpName,_tmpContent,_tmpDescription,_tmpCreatedAt,_tmpUpdatedAt,_tmpExecutionCount,_tmpLastExecuted);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getScriptById(final long scriptId,
      final Continuation<? super ShellScriptEntity> $completion) {
    final String _sql = "SELECT * FROM shell_scripts WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, scriptId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<ShellScriptEntity>() {
      @Override
      @Nullable
      public ShellScriptEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfContent = CursorUtil.getColumnIndexOrThrow(_cursor, "content");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final int _cursorIndexOfExecutionCount = CursorUtil.getColumnIndexOrThrow(_cursor, "executionCount");
          final int _cursorIndexOfLastExecuted = CursorUtil.getColumnIndexOrThrow(_cursor, "lastExecuted");
          final ShellScriptEntity _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpContent;
            _tmpContent = _cursor.getString(_cursorIndexOfContent);
            final String _tmpDescription;
            if (_cursor.isNull(_cursorIndexOfDescription)) {
              _tmpDescription = null;
            } else {
              _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            final int _tmpExecutionCount;
            _tmpExecutionCount = _cursor.getInt(_cursorIndexOfExecutionCount);
            final Long _tmpLastExecuted;
            if (_cursor.isNull(_cursorIndexOfLastExecuted)) {
              _tmpLastExecuted = null;
            } else {
              _tmpLastExecuted = _cursor.getLong(_cursorIndexOfLastExecuted);
            }
            _result = new ShellScriptEntity(_tmpId,_tmpName,_tmpContent,_tmpDescription,_tmpCreatedAt,_tmpUpdatedAt,_tmpExecutionCount,_tmpLastExecuted);
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

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
