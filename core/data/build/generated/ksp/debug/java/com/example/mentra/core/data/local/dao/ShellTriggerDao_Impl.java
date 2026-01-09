package com.example.mentra.core.data.local.dao;

import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.example.mentra.core.data.local.entity.ShellTriggerEntity;
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
public final class ShellTriggerDao_Impl implements ShellTriggerDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<ShellTriggerEntity> __insertionAdapterOfShellTriggerEntity;

  private final EntityDeletionOrUpdateAdapter<ShellTriggerEntity> __deletionAdapterOfShellTriggerEntity;

  private final SharedSQLiteStatement __preparedStmtOfUpdateTriggerEnabled;

  public ShellTriggerDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfShellTriggerEntity = new EntityInsertionAdapter<ShellTriggerEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `shell_triggers` (`id`,`name`,`triggerType`,`scriptId`,`enabled`,`conditions`) VALUES (nullif(?, 0),?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final ShellTriggerEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getName());
        statement.bindString(3, entity.getTriggerType());
        statement.bindLong(4, entity.getScriptId());
        final int _tmp = entity.getEnabled() ? 1 : 0;
        statement.bindLong(5, _tmp);
        if (entity.getConditions() == null) {
          statement.bindNull(6);
        } else {
          statement.bindString(6, entity.getConditions());
        }
      }
    };
    this.__deletionAdapterOfShellTriggerEntity = new EntityDeletionOrUpdateAdapter<ShellTriggerEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `shell_triggers` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final ShellTriggerEntity entity) {
        statement.bindLong(1, entity.getId());
      }
    };
    this.__preparedStmtOfUpdateTriggerEnabled = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE shell_triggers SET enabled = ? WHERE id = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insertTrigger(final ShellTriggerEntity trigger,
      final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfShellTriggerEntity.insertAndReturnId(trigger);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteTrigger(final ShellTriggerEntity trigger,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfShellTriggerEntity.handle(trigger);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateTriggerEnabled(final long triggerId, final boolean enabled,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateTriggerEnabled.acquire();
        int _argIndex = 1;
        final int _tmp = enabled ? 1 : 0;
        _stmt.bindLong(_argIndex, _tmp);
        _argIndex = 2;
        _stmt.bindLong(_argIndex, triggerId);
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
          __preparedStmtOfUpdateTriggerEnabled.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<ShellTriggerEntity>> getEnabledTriggers() {
    final String _sql = "SELECT * FROM shell_triggers WHERE enabled = 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"shell_triggers"}, new Callable<List<ShellTriggerEntity>>() {
      @Override
      @NonNull
      public List<ShellTriggerEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfTriggerType = CursorUtil.getColumnIndexOrThrow(_cursor, "triggerType");
          final int _cursorIndexOfScriptId = CursorUtil.getColumnIndexOrThrow(_cursor, "scriptId");
          final int _cursorIndexOfEnabled = CursorUtil.getColumnIndexOrThrow(_cursor, "enabled");
          final int _cursorIndexOfConditions = CursorUtil.getColumnIndexOrThrow(_cursor, "conditions");
          final List<ShellTriggerEntity> _result = new ArrayList<ShellTriggerEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ShellTriggerEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpTriggerType;
            _tmpTriggerType = _cursor.getString(_cursorIndexOfTriggerType);
            final long _tmpScriptId;
            _tmpScriptId = _cursor.getLong(_cursorIndexOfScriptId);
            final boolean _tmpEnabled;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfEnabled);
            _tmpEnabled = _tmp != 0;
            final String _tmpConditions;
            if (_cursor.isNull(_cursorIndexOfConditions)) {
              _tmpConditions = null;
            } else {
              _tmpConditions = _cursor.getString(_cursorIndexOfConditions);
            }
            _item = new ShellTriggerEntity(_tmpId,_tmpName,_tmpTriggerType,_tmpScriptId,_tmpEnabled,_tmpConditions);
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
  public Flow<List<ShellTriggerEntity>> getTriggersByType(final String type) {
    final String _sql = "SELECT * FROM shell_triggers WHERE triggerType = ? AND enabled = 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, type);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"shell_triggers"}, new Callable<List<ShellTriggerEntity>>() {
      @Override
      @NonNull
      public List<ShellTriggerEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfTriggerType = CursorUtil.getColumnIndexOrThrow(_cursor, "triggerType");
          final int _cursorIndexOfScriptId = CursorUtil.getColumnIndexOrThrow(_cursor, "scriptId");
          final int _cursorIndexOfEnabled = CursorUtil.getColumnIndexOrThrow(_cursor, "enabled");
          final int _cursorIndexOfConditions = CursorUtil.getColumnIndexOrThrow(_cursor, "conditions");
          final List<ShellTriggerEntity> _result = new ArrayList<ShellTriggerEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ShellTriggerEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpTriggerType;
            _tmpTriggerType = _cursor.getString(_cursorIndexOfTriggerType);
            final long _tmpScriptId;
            _tmpScriptId = _cursor.getLong(_cursorIndexOfScriptId);
            final boolean _tmpEnabled;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfEnabled);
            _tmpEnabled = _tmp != 0;
            final String _tmpConditions;
            if (_cursor.isNull(_cursorIndexOfConditions)) {
              _tmpConditions = null;
            } else {
              _tmpConditions = _cursor.getString(_cursorIndexOfConditions);
            }
            _item = new ShellTriggerEntity(_tmpId,_tmpName,_tmpTriggerType,_tmpScriptId,_tmpEnabled,_tmpConditions);
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

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
