package com.example.mentra.core.data.local.dao;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.example.mentra.core.data.local.entity.RoutePointEntity;
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

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class RoutePointDao_Impl implements RoutePointDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<RoutePointEntity> __insertionAdapterOfRoutePointEntity;

  private final SharedSQLiteStatement __preparedStmtOfDeletePointsForRoute;

  public RoutePointDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfRoutePointEntity = new EntityInsertionAdapter<RoutePointEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `route_points` (`id`,`routeId`,`latitude`,`longitude`,`sequence`,`instruction`) VALUES (nullif(?, 0),?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final RoutePointEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getRouteId());
        statement.bindDouble(3, entity.getLatitude());
        statement.bindDouble(4, entity.getLongitude());
        statement.bindLong(5, entity.getSequence());
        if (entity.getInstruction() == null) {
          statement.bindNull(6);
        } else {
          statement.bindString(6, entity.getInstruction());
        }
      }
    };
    this.__preparedStmtOfDeletePointsForRoute = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM route_points WHERE routeId = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insertPoint(final RoutePointEntity point,
      final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfRoutePointEntity.insertAndReturnId(point);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertPoints(final List<RoutePointEntity> points,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfRoutePointEntity.insert(points);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deletePointsForRoute(final long routeId,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeletePointsForRoute.acquire();
        int _argIndex = 1;
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
          __preparedStmtOfDeletePointsForRoute.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object getPointsForRoute(final long routeId,
      final Continuation<? super List<RoutePointEntity>> $completion) {
    final String _sql = "SELECT * FROM route_points WHERE routeId = ? ORDER BY sequence ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, routeId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<RoutePointEntity>>() {
      @Override
      @NonNull
      public List<RoutePointEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfRouteId = CursorUtil.getColumnIndexOrThrow(_cursor, "routeId");
          final int _cursorIndexOfLatitude = CursorUtil.getColumnIndexOrThrow(_cursor, "latitude");
          final int _cursorIndexOfLongitude = CursorUtil.getColumnIndexOrThrow(_cursor, "longitude");
          final int _cursorIndexOfSequence = CursorUtil.getColumnIndexOrThrow(_cursor, "sequence");
          final int _cursorIndexOfInstruction = CursorUtil.getColumnIndexOrThrow(_cursor, "instruction");
          final List<RoutePointEntity> _result = new ArrayList<RoutePointEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final RoutePointEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpRouteId;
            _tmpRouteId = _cursor.getLong(_cursorIndexOfRouteId);
            final double _tmpLatitude;
            _tmpLatitude = _cursor.getDouble(_cursorIndexOfLatitude);
            final double _tmpLongitude;
            _tmpLongitude = _cursor.getDouble(_cursorIndexOfLongitude);
            final int _tmpSequence;
            _tmpSequence = _cursor.getInt(_cursorIndexOfSequence);
            final String _tmpInstruction;
            if (_cursor.isNull(_cursorIndexOfInstruction)) {
              _tmpInstruction = null;
            } else {
              _tmpInstruction = _cursor.getString(_cursorIndexOfInstruction);
            }
            _item = new RoutePointEntity(_tmpId,_tmpRouteId,_tmpLatitude,_tmpLongitude,_tmpSequence,_tmpInstruction);
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
