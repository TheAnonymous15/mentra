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
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.example.mentra.core.data.local.entity.HealthStatsEntity;
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
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class HealthStatsDao_Impl implements HealthStatsDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<HealthStatsEntity> __insertionAdapterOfHealthStatsEntity;

  private final EntityDeletionOrUpdateAdapter<HealthStatsEntity> __deletionAdapterOfHealthStatsEntity;

  public HealthStatsDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfHealthStatsEntity = new EntityInsertionAdapter<HealthStatsEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `health_stats` (`date`,`totalSteps`,`totalDistance`,`totalCalories`,`activeMinutes`,`walkingMinutes`,`runningMinutes`,`cyclingMinutes`) VALUES (?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final HealthStatsEntity entity) {
        statement.bindString(1, entity.getDate());
        statement.bindLong(2, entity.getTotalSteps());
        statement.bindDouble(3, entity.getTotalDistance());
        statement.bindDouble(4, entity.getTotalCalories());
        statement.bindLong(5, entity.getActiveMinutes());
        statement.bindLong(6, entity.getWalkingMinutes());
        statement.bindLong(7, entity.getRunningMinutes());
        statement.bindLong(8, entity.getCyclingMinutes());
      }
    };
    this.__deletionAdapterOfHealthStatsEntity = new EntityDeletionOrUpdateAdapter<HealthStatsEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `health_stats` WHERE `date` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final HealthStatsEntity entity) {
        statement.bindString(1, entity.getDate());
      }
    };
  }

  @Override
  public Object insertStats(final HealthStatsEntity stats,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfHealthStatsEntity.insert(stats);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteStats(final HealthStatsEntity stats,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfHealthStatsEntity.handle(stats);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object getStatsForDate(final String date,
      final Continuation<? super HealthStatsEntity> $completion) {
    final String _sql = "SELECT * FROM health_stats WHERE date = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, date);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<HealthStatsEntity>() {
      @Override
      @Nullable
      public HealthStatsEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfTotalSteps = CursorUtil.getColumnIndexOrThrow(_cursor, "totalSteps");
          final int _cursorIndexOfTotalDistance = CursorUtil.getColumnIndexOrThrow(_cursor, "totalDistance");
          final int _cursorIndexOfTotalCalories = CursorUtil.getColumnIndexOrThrow(_cursor, "totalCalories");
          final int _cursorIndexOfActiveMinutes = CursorUtil.getColumnIndexOrThrow(_cursor, "activeMinutes");
          final int _cursorIndexOfWalkingMinutes = CursorUtil.getColumnIndexOrThrow(_cursor, "walkingMinutes");
          final int _cursorIndexOfRunningMinutes = CursorUtil.getColumnIndexOrThrow(_cursor, "runningMinutes");
          final int _cursorIndexOfCyclingMinutes = CursorUtil.getColumnIndexOrThrow(_cursor, "cyclingMinutes");
          final HealthStatsEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpDate;
            _tmpDate = _cursor.getString(_cursorIndexOfDate);
            final int _tmpTotalSteps;
            _tmpTotalSteps = _cursor.getInt(_cursorIndexOfTotalSteps);
            final double _tmpTotalDistance;
            _tmpTotalDistance = _cursor.getDouble(_cursorIndexOfTotalDistance);
            final double _tmpTotalCalories;
            _tmpTotalCalories = _cursor.getDouble(_cursorIndexOfTotalCalories);
            final int _tmpActiveMinutes;
            _tmpActiveMinutes = _cursor.getInt(_cursorIndexOfActiveMinutes);
            final int _tmpWalkingMinutes;
            _tmpWalkingMinutes = _cursor.getInt(_cursorIndexOfWalkingMinutes);
            final int _tmpRunningMinutes;
            _tmpRunningMinutes = _cursor.getInt(_cursorIndexOfRunningMinutes);
            final int _tmpCyclingMinutes;
            _tmpCyclingMinutes = _cursor.getInt(_cursorIndexOfCyclingMinutes);
            _result = new HealthStatsEntity(_tmpDate,_tmpTotalSteps,_tmpTotalDistance,_tmpTotalCalories,_tmpActiveMinutes,_tmpWalkingMinutes,_tmpRunningMinutes,_tmpCyclingMinutes);
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
  public Flow<HealthStatsEntity> getStatsForDateFlow(final String date) {
    final String _sql = "SELECT * FROM health_stats WHERE date = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, date);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"health_stats"}, new Callable<HealthStatsEntity>() {
      @Override
      @Nullable
      public HealthStatsEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfTotalSteps = CursorUtil.getColumnIndexOrThrow(_cursor, "totalSteps");
          final int _cursorIndexOfTotalDistance = CursorUtil.getColumnIndexOrThrow(_cursor, "totalDistance");
          final int _cursorIndexOfTotalCalories = CursorUtil.getColumnIndexOrThrow(_cursor, "totalCalories");
          final int _cursorIndexOfActiveMinutes = CursorUtil.getColumnIndexOrThrow(_cursor, "activeMinutes");
          final int _cursorIndexOfWalkingMinutes = CursorUtil.getColumnIndexOrThrow(_cursor, "walkingMinutes");
          final int _cursorIndexOfRunningMinutes = CursorUtil.getColumnIndexOrThrow(_cursor, "runningMinutes");
          final int _cursorIndexOfCyclingMinutes = CursorUtil.getColumnIndexOrThrow(_cursor, "cyclingMinutes");
          final HealthStatsEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpDate;
            _tmpDate = _cursor.getString(_cursorIndexOfDate);
            final int _tmpTotalSteps;
            _tmpTotalSteps = _cursor.getInt(_cursorIndexOfTotalSteps);
            final double _tmpTotalDistance;
            _tmpTotalDistance = _cursor.getDouble(_cursorIndexOfTotalDistance);
            final double _tmpTotalCalories;
            _tmpTotalCalories = _cursor.getDouble(_cursorIndexOfTotalCalories);
            final int _tmpActiveMinutes;
            _tmpActiveMinutes = _cursor.getInt(_cursorIndexOfActiveMinutes);
            final int _tmpWalkingMinutes;
            _tmpWalkingMinutes = _cursor.getInt(_cursorIndexOfWalkingMinutes);
            final int _tmpRunningMinutes;
            _tmpRunningMinutes = _cursor.getInt(_cursorIndexOfRunningMinutes);
            final int _tmpCyclingMinutes;
            _tmpCyclingMinutes = _cursor.getInt(_cursorIndexOfCyclingMinutes);
            _result = new HealthStatsEntity(_tmpDate,_tmpTotalSteps,_tmpTotalDistance,_tmpTotalCalories,_tmpActiveMinutes,_tmpWalkingMinutes,_tmpRunningMinutes,_tmpCyclingMinutes);
          } else {
            _result = null;
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
  public Flow<List<HealthStatsEntity>> getRecentStats(final int limit) {
    final String _sql = "SELECT * FROM health_stats ORDER BY date DESC LIMIT ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, limit);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"health_stats"}, new Callable<List<HealthStatsEntity>>() {
      @Override
      @NonNull
      public List<HealthStatsEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfTotalSteps = CursorUtil.getColumnIndexOrThrow(_cursor, "totalSteps");
          final int _cursorIndexOfTotalDistance = CursorUtil.getColumnIndexOrThrow(_cursor, "totalDistance");
          final int _cursorIndexOfTotalCalories = CursorUtil.getColumnIndexOrThrow(_cursor, "totalCalories");
          final int _cursorIndexOfActiveMinutes = CursorUtil.getColumnIndexOrThrow(_cursor, "activeMinutes");
          final int _cursorIndexOfWalkingMinutes = CursorUtil.getColumnIndexOrThrow(_cursor, "walkingMinutes");
          final int _cursorIndexOfRunningMinutes = CursorUtil.getColumnIndexOrThrow(_cursor, "runningMinutes");
          final int _cursorIndexOfCyclingMinutes = CursorUtil.getColumnIndexOrThrow(_cursor, "cyclingMinutes");
          final List<HealthStatsEntity> _result = new ArrayList<HealthStatsEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final HealthStatsEntity _item;
            final String _tmpDate;
            _tmpDate = _cursor.getString(_cursorIndexOfDate);
            final int _tmpTotalSteps;
            _tmpTotalSteps = _cursor.getInt(_cursorIndexOfTotalSteps);
            final double _tmpTotalDistance;
            _tmpTotalDistance = _cursor.getDouble(_cursorIndexOfTotalDistance);
            final double _tmpTotalCalories;
            _tmpTotalCalories = _cursor.getDouble(_cursorIndexOfTotalCalories);
            final int _tmpActiveMinutes;
            _tmpActiveMinutes = _cursor.getInt(_cursorIndexOfActiveMinutes);
            final int _tmpWalkingMinutes;
            _tmpWalkingMinutes = _cursor.getInt(_cursorIndexOfWalkingMinutes);
            final int _tmpRunningMinutes;
            _tmpRunningMinutes = _cursor.getInt(_cursorIndexOfRunningMinutes);
            final int _tmpCyclingMinutes;
            _tmpCyclingMinutes = _cursor.getInt(_cursorIndexOfCyclingMinutes);
            _item = new HealthStatsEntity(_tmpDate,_tmpTotalSteps,_tmpTotalDistance,_tmpTotalCalories,_tmpActiveMinutes,_tmpWalkingMinutes,_tmpRunningMinutes,_tmpCyclingMinutes);
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
  public Flow<List<HealthStatsEntity>> getStatsInRange(final String startDate,
      final String endDate) {
    final String _sql = "SELECT * FROM health_stats WHERE date >= ? AND date <= ? ORDER BY date ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindString(_argIndex, startDate);
    _argIndex = 2;
    _statement.bindString(_argIndex, endDate);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"health_stats"}, new Callable<List<HealthStatsEntity>>() {
      @Override
      @NonNull
      public List<HealthStatsEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfTotalSteps = CursorUtil.getColumnIndexOrThrow(_cursor, "totalSteps");
          final int _cursorIndexOfTotalDistance = CursorUtil.getColumnIndexOrThrow(_cursor, "totalDistance");
          final int _cursorIndexOfTotalCalories = CursorUtil.getColumnIndexOrThrow(_cursor, "totalCalories");
          final int _cursorIndexOfActiveMinutes = CursorUtil.getColumnIndexOrThrow(_cursor, "activeMinutes");
          final int _cursorIndexOfWalkingMinutes = CursorUtil.getColumnIndexOrThrow(_cursor, "walkingMinutes");
          final int _cursorIndexOfRunningMinutes = CursorUtil.getColumnIndexOrThrow(_cursor, "runningMinutes");
          final int _cursorIndexOfCyclingMinutes = CursorUtil.getColumnIndexOrThrow(_cursor, "cyclingMinutes");
          final List<HealthStatsEntity> _result = new ArrayList<HealthStatsEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final HealthStatsEntity _item;
            final String _tmpDate;
            _tmpDate = _cursor.getString(_cursorIndexOfDate);
            final int _tmpTotalSteps;
            _tmpTotalSteps = _cursor.getInt(_cursorIndexOfTotalSteps);
            final double _tmpTotalDistance;
            _tmpTotalDistance = _cursor.getDouble(_cursorIndexOfTotalDistance);
            final double _tmpTotalCalories;
            _tmpTotalCalories = _cursor.getDouble(_cursorIndexOfTotalCalories);
            final int _tmpActiveMinutes;
            _tmpActiveMinutes = _cursor.getInt(_cursorIndexOfActiveMinutes);
            final int _tmpWalkingMinutes;
            _tmpWalkingMinutes = _cursor.getInt(_cursorIndexOfWalkingMinutes);
            final int _tmpRunningMinutes;
            _tmpRunningMinutes = _cursor.getInt(_cursorIndexOfRunningMinutes);
            final int _tmpCyclingMinutes;
            _tmpCyclingMinutes = _cursor.getInt(_cursorIndexOfCyclingMinutes);
            _item = new HealthStatsEntity(_tmpDate,_tmpTotalSteps,_tmpTotalDistance,_tmpTotalCalories,_tmpActiveMinutes,_tmpWalkingMinutes,_tmpRunningMinutes,_tmpCyclingMinutes);
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
