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
import com.example.mentra.core.data.local.entity.SavedRouteEntity;
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
public final class SavedRouteDao_Impl implements SavedRouteDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<SavedRouteEntity> __insertionAdapterOfSavedRouteEntity;

  private final EntityDeletionOrUpdateAdapter<SavedRouteEntity> __deletionAdapterOfSavedRouteEntity;

  private final SharedSQLiteStatement __preparedStmtOfUpdateLastUsed;

  public SavedRouteDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfSavedRouteEntity = new EntityInsertionAdapter<SavedRouteEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `saved_routes` (`id`,`name`,`startLatitude`,`startLongitude`,`endLatitude`,`endLongitude`,`distance`,`estimatedTime`,`createdAt`,`lastUsed`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final SavedRouteEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getName());
        statement.bindDouble(3, entity.getStartLatitude());
        statement.bindDouble(4, entity.getStartLongitude());
        statement.bindDouble(5, entity.getEndLatitude());
        statement.bindDouble(6, entity.getEndLongitude());
        statement.bindDouble(7, entity.getDistance());
        statement.bindLong(8, entity.getEstimatedTime());
        statement.bindLong(9, entity.getCreatedAt());
        statement.bindLong(10, entity.getLastUsed());
      }
    };
    this.__deletionAdapterOfSavedRouteEntity = new EntityDeletionOrUpdateAdapter<SavedRouteEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `saved_routes` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final SavedRouteEntity entity) {
        statement.bindLong(1, entity.getId());
      }
    };
    this.__preparedStmtOfUpdateLastUsed = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE saved_routes SET lastUsed = ? WHERE id = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insertRoute(final SavedRouteEntity route,
      final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfSavedRouteEntity.insertAndReturnId(route);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteRoute(final SavedRouteEntity route,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfSavedRouteEntity.handle(route);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateLastUsed(final long routeId, final long timestamp,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateLastUsed.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, timestamp);
        _argIndex = 2;
        _stmt.bindLong(_argIndex, routeId);
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
          __preparedStmtOfUpdateLastUsed.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<SavedRouteEntity>> getAllRoutes() {
    final String _sql = "SELECT * FROM saved_routes ORDER BY lastUsed DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"saved_routes"}, new Callable<List<SavedRouteEntity>>() {
      @Override
      @NonNull
      public List<SavedRouteEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfStartLatitude = CursorUtil.getColumnIndexOrThrow(_cursor, "startLatitude");
          final int _cursorIndexOfStartLongitude = CursorUtil.getColumnIndexOrThrow(_cursor, "startLongitude");
          final int _cursorIndexOfEndLatitude = CursorUtil.getColumnIndexOrThrow(_cursor, "endLatitude");
          final int _cursorIndexOfEndLongitude = CursorUtil.getColumnIndexOrThrow(_cursor, "endLongitude");
          final int _cursorIndexOfDistance = CursorUtil.getColumnIndexOrThrow(_cursor, "distance");
          final int _cursorIndexOfEstimatedTime = CursorUtil.getColumnIndexOrThrow(_cursor, "estimatedTime");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfLastUsed = CursorUtil.getColumnIndexOrThrow(_cursor, "lastUsed");
          final List<SavedRouteEntity> _result = new ArrayList<SavedRouteEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final SavedRouteEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final double _tmpStartLatitude;
            _tmpStartLatitude = _cursor.getDouble(_cursorIndexOfStartLatitude);
            final double _tmpStartLongitude;
            _tmpStartLongitude = _cursor.getDouble(_cursorIndexOfStartLongitude);
            final double _tmpEndLatitude;
            _tmpEndLatitude = _cursor.getDouble(_cursorIndexOfEndLatitude);
            final double _tmpEndLongitude;
            _tmpEndLongitude = _cursor.getDouble(_cursorIndexOfEndLongitude);
            final double _tmpDistance;
            _tmpDistance = _cursor.getDouble(_cursorIndexOfDistance);
            final long _tmpEstimatedTime;
            _tmpEstimatedTime = _cursor.getLong(_cursorIndexOfEstimatedTime);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpLastUsed;
            _tmpLastUsed = _cursor.getLong(_cursorIndexOfLastUsed);
            _item = new SavedRouteEntity(_tmpId,_tmpName,_tmpStartLatitude,_tmpStartLongitude,_tmpEndLatitude,_tmpEndLongitude,_tmpDistance,_tmpEstimatedTime,_tmpCreatedAt,_tmpLastUsed);
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
  public Object getRouteById(final long routeId,
      final Continuation<? super SavedRouteEntity> $completion) {
    final String _sql = "SELECT * FROM saved_routes WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, routeId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<SavedRouteEntity>() {
      @Override
      @Nullable
      public SavedRouteEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfStartLatitude = CursorUtil.getColumnIndexOrThrow(_cursor, "startLatitude");
          final int _cursorIndexOfStartLongitude = CursorUtil.getColumnIndexOrThrow(_cursor, "startLongitude");
          final int _cursorIndexOfEndLatitude = CursorUtil.getColumnIndexOrThrow(_cursor, "endLatitude");
          final int _cursorIndexOfEndLongitude = CursorUtil.getColumnIndexOrThrow(_cursor, "endLongitude");
          final int _cursorIndexOfDistance = CursorUtil.getColumnIndexOrThrow(_cursor, "distance");
          final int _cursorIndexOfEstimatedTime = CursorUtil.getColumnIndexOrThrow(_cursor, "estimatedTime");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfLastUsed = CursorUtil.getColumnIndexOrThrow(_cursor, "lastUsed");
          final SavedRouteEntity _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final double _tmpStartLatitude;
            _tmpStartLatitude = _cursor.getDouble(_cursorIndexOfStartLatitude);
            final double _tmpStartLongitude;
            _tmpStartLongitude = _cursor.getDouble(_cursorIndexOfStartLongitude);
            final double _tmpEndLatitude;
            _tmpEndLatitude = _cursor.getDouble(_cursorIndexOfEndLatitude);
            final double _tmpEndLongitude;
            _tmpEndLongitude = _cursor.getDouble(_cursorIndexOfEndLongitude);
            final double _tmpDistance;
            _tmpDistance = _cursor.getDouble(_cursorIndexOfDistance);
            final long _tmpEstimatedTime;
            _tmpEstimatedTime = _cursor.getLong(_cursorIndexOfEstimatedTime);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpLastUsed;
            _tmpLastUsed = _cursor.getLong(_cursorIndexOfLastUsed);
            _result = new SavedRouteEntity(_tmpId,_tmpName,_tmpStartLatitude,_tmpStartLongitude,_tmpEndLatitude,_tmpEndLongitude,_tmpDistance,_tmpEstimatedTime,_tmpCreatedAt,_tmpLastUsed);
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
