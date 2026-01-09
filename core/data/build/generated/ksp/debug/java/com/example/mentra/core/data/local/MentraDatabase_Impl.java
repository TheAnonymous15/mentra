package com.example.mentra.core.data.local;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import com.example.mentra.core.data.local.dao.ActivityDao;
import com.example.mentra.core.data.local.dao.ActivityDao_Impl;
import com.example.mentra.core.data.local.dao.HealthStatsDao;
import com.example.mentra.core.data.local.dao.HealthStatsDao_Impl;
import com.example.mentra.core.data.local.dao.MediaItemDao;
import com.example.mentra.core.data.local.dao.MediaItemDao_Impl;
import com.example.mentra.core.data.local.dao.POIDao;
import com.example.mentra.core.data.local.dao.POIDao_Impl;
import com.example.mentra.core.data.local.dao.PlaylistDao;
import com.example.mentra.core.data.local.dao.PlaylistDao_Impl;
import com.example.mentra.core.data.local.dao.PlaylistItemDao;
import com.example.mentra.core.data.local.dao.PlaylistItemDao_Impl;
import com.example.mentra.core.data.local.dao.RoutePointDao;
import com.example.mentra.core.data.local.dao.RoutePointDao_Impl;
import com.example.mentra.core.data.local.dao.SavedRouteDao;
import com.example.mentra.core.data.local.dao.SavedRouteDao_Impl;
import com.example.mentra.core.data.local.dao.ShellAliasDao;
import com.example.mentra.core.data.local.dao.ShellAliasDao_Impl;
import com.example.mentra.core.data.local.dao.ShellHistoryDao;
import com.example.mentra.core.data.local.dao.ShellHistoryDao_Impl;
import com.example.mentra.core.data.local.dao.ShellScriptDao;
import com.example.mentra.core.data.local.dao.ShellScriptDao_Impl;
import com.example.mentra.core.data.local.dao.ShellTriggerDao;
import com.example.mentra.core.data.local.dao.ShellTriggerDao_Impl;
import com.example.mentra.core.data.local.dao.SleepDao;
import com.example.mentra.core.data.local.dao.SleepDao_Impl;
import com.example.mentra.core.data.local.dao.UserProfileDao;
import com.example.mentra.core.data.local.dao.UserProfileDao_Impl;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class MentraDatabase_Impl extends MentraDatabase {
  private volatile ActivityDao _activityDao;

  private volatile HealthStatsDao _healthStatsDao;

  private volatile SleepDao _sleepDao;

  private volatile SavedRouteDao _savedRouteDao;

  private volatile RoutePointDao _routePointDao;

  private volatile POIDao _pOIDao;

  private volatile MediaItemDao _mediaItemDao;

  private volatile PlaylistDao _playlistDao;

  private volatile PlaylistItemDao _playlistItemDao;

  private volatile ShellHistoryDao _shellHistoryDao;

  private volatile ShellAliasDao _shellAliasDao;

  private volatile ShellScriptDao _shellScriptDao;

  private volatile ShellTriggerDao _shellTriggerDao;

  private volatile UserProfileDao _userProfileDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(1) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `activity_records` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `timestamp` INTEGER NOT NULL, `activityType` TEXT NOT NULL, `steps` INTEGER NOT NULL, `distance` REAL NOT NULL, `calories` REAL NOT NULL, `duration` INTEGER NOT NULL, `confidence` REAL NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `health_stats` (`date` TEXT NOT NULL, `totalSteps` INTEGER NOT NULL, `totalDistance` REAL NOT NULL, `totalCalories` REAL NOT NULL, `activeMinutes` INTEGER NOT NULL, `walkingMinutes` INTEGER NOT NULL, `runningMinutes` INTEGER NOT NULL, `cyclingMinutes` INTEGER NOT NULL, PRIMARY KEY(`date`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `sleep_data` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `date` TEXT NOT NULL, `sleepStart` INTEGER NOT NULL, `sleepEnd` INTEGER NOT NULL, `duration` INTEGER NOT NULL, `quality` INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `saved_routes` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `startLatitude` REAL NOT NULL, `startLongitude` REAL NOT NULL, `endLatitude` REAL NOT NULL, `endLongitude` REAL NOT NULL, `distance` REAL NOT NULL, `estimatedTime` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `lastUsed` INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `route_points` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `routeId` INTEGER NOT NULL, `latitude` REAL NOT NULL, `longitude` REAL NOT NULL, `sequence` INTEGER NOT NULL, `instruction` TEXT)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `poi` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `category` TEXT NOT NULL, `latitude` REAL NOT NULL, `longitude` REAL NOT NULL, `address` TEXT, `rating` REAL, `isFavorite` INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `media_items` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, `artist` TEXT, `album` TEXT, `genre` TEXT, `duration` INTEGER NOT NULL, `filePath` TEXT NOT NULL, `mimeType` TEXT NOT NULL, `size` INTEGER NOT NULL, `dateAdded` INTEGER NOT NULL, `dateModified` INTEGER NOT NULL, `albumArtPath` TEXT, `playCount` INTEGER NOT NULL, `lastPlayed` INTEGER, `isFavorite` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `playlists` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `description` TEXT, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `trackCount` INTEGER NOT NULL, `coverArtPath` TEXT)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `playlist_items` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `playlistId` INTEGER NOT NULL, `mediaId` TEXT NOT NULL, `position` INTEGER NOT NULL, `addedAt` INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `shell_history` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `command` TEXT NOT NULL, `originalLanguage` TEXT, `translatedCommand` TEXT, `result` TEXT NOT NULL, `success` INTEGER NOT NULL, `timestamp` INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `shell_aliases` (`alias` TEXT NOT NULL, `target` TEXT NOT NULL, `description` TEXT, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`alias`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `shell_scripts` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `content` TEXT NOT NULL, `description` TEXT, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `executionCount` INTEGER NOT NULL, `lastExecuted` INTEGER)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `shell_triggers` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `triggerType` TEXT NOT NULL, `scriptId` INTEGER NOT NULL, `enabled` INTEGER NOT NULL, `conditions` TEXT)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `user_profile` (`id` INTEGER NOT NULL, `name` TEXT, `height` REAL, `weight` REAL, `age` INTEGER, `gender` TEXT, `dailyStepGoal` INTEGER NOT NULL, `useMetricSystem` INTEGER NOT NULL, `theme` TEXT NOT NULL, `language` TEXT NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'c038110da0aee065a29d0518de716b8a')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `activity_records`");
        db.execSQL("DROP TABLE IF EXISTS `health_stats`");
        db.execSQL("DROP TABLE IF EXISTS `sleep_data`");
        db.execSQL("DROP TABLE IF EXISTS `saved_routes`");
        db.execSQL("DROP TABLE IF EXISTS `route_points`");
        db.execSQL("DROP TABLE IF EXISTS `poi`");
        db.execSQL("DROP TABLE IF EXISTS `media_items`");
        db.execSQL("DROP TABLE IF EXISTS `playlists`");
        db.execSQL("DROP TABLE IF EXISTS `playlist_items`");
        db.execSQL("DROP TABLE IF EXISTS `shell_history`");
        db.execSQL("DROP TABLE IF EXISTS `shell_aliases`");
        db.execSQL("DROP TABLE IF EXISTS `shell_scripts`");
        db.execSQL("DROP TABLE IF EXISTS `shell_triggers`");
        db.execSQL("DROP TABLE IF EXISTS `user_profile`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsActivityRecords = new HashMap<String, TableInfo.Column>(8);
        _columnsActivityRecords.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsActivityRecords.put("timestamp", new TableInfo.Column("timestamp", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsActivityRecords.put("activityType", new TableInfo.Column("activityType", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsActivityRecords.put("steps", new TableInfo.Column("steps", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsActivityRecords.put("distance", new TableInfo.Column("distance", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsActivityRecords.put("calories", new TableInfo.Column("calories", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsActivityRecords.put("duration", new TableInfo.Column("duration", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsActivityRecords.put("confidence", new TableInfo.Column("confidence", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysActivityRecords = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesActivityRecords = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoActivityRecords = new TableInfo("activity_records", _columnsActivityRecords, _foreignKeysActivityRecords, _indicesActivityRecords);
        final TableInfo _existingActivityRecords = TableInfo.read(db, "activity_records");
        if (!_infoActivityRecords.equals(_existingActivityRecords)) {
          return new RoomOpenHelper.ValidationResult(false, "activity_records(com.example.mentra.core.data.local.entity.ActivityRecordEntity).\n"
                  + " Expected:\n" + _infoActivityRecords + "\n"
                  + " Found:\n" + _existingActivityRecords);
        }
        final HashMap<String, TableInfo.Column> _columnsHealthStats = new HashMap<String, TableInfo.Column>(8);
        _columnsHealthStats.put("date", new TableInfo.Column("date", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHealthStats.put("totalSteps", new TableInfo.Column("totalSteps", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHealthStats.put("totalDistance", new TableInfo.Column("totalDistance", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHealthStats.put("totalCalories", new TableInfo.Column("totalCalories", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHealthStats.put("activeMinutes", new TableInfo.Column("activeMinutes", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHealthStats.put("walkingMinutes", new TableInfo.Column("walkingMinutes", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHealthStats.put("runningMinutes", new TableInfo.Column("runningMinutes", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHealthStats.put("cyclingMinutes", new TableInfo.Column("cyclingMinutes", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysHealthStats = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesHealthStats = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoHealthStats = new TableInfo("health_stats", _columnsHealthStats, _foreignKeysHealthStats, _indicesHealthStats);
        final TableInfo _existingHealthStats = TableInfo.read(db, "health_stats");
        if (!_infoHealthStats.equals(_existingHealthStats)) {
          return new RoomOpenHelper.ValidationResult(false, "health_stats(com.example.mentra.core.data.local.entity.HealthStatsEntity).\n"
                  + " Expected:\n" + _infoHealthStats + "\n"
                  + " Found:\n" + _existingHealthStats);
        }
        final HashMap<String, TableInfo.Column> _columnsSleepData = new HashMap<String, TableInfo.Column>(6);
        _columnsSleepData.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSleepData.put("date", new TableInfo.Column("date", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSleepData.put("sleepStart", new TableInfo.Column("sleepStart", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSleepData.put("sleepEnd", new TableInfo.Column("sleepEnd", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSleepData.put("duration", new TableInfo.Column("duration", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSleepData.put("quality", new TableInfo.Column("quality", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysSleepData = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesSleepData = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoSleepData = new TableInfo("sleep_data", _columnsSleepData, _foreignKeysSleepData, _indicesSleepData);
        final TableInfo _existingSleepData = TableInfo.read(db, "sleep_data");
        if (!_infoSleepData.equals(_existingSleepData)) {
          return new RoomOpenHelper.ValidationResult(false, "sleep_data(com.example.mentra.core.data.local.entity.SleepDataEntity).\n"
                  + " Expected:\n" + _infoSleepData + "\n"
                  + " Found:\n" + _existingSleepData);
        }
        final HashMap<String, TableInfo.Column> _columnsSavedRoutes = new HashMap<String, TableInfo.Column>(10);
        _columnsSavedRoutes.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSavedRoutes.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSavedRoutes.put("startLatitude", new TableInfo.Column("startLatitude", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSavedRoutes.put("startLongitude", new TableInfo.Column("startLongitude", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSavedRoutes.put("endLatitude", new TableInfo.Column("endLatitude", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSavedRoutes.put("endLongitude", new TableInfo.Column("endLongitude", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSavedRoutes.put("distance", new TableInfo.Column("distance", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSavedRoutes.put("estimatedTime", new TableInfo.Column("estimatedTime", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSavedRoutes.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSavedRoutes.put("lastUsed", new TableInfo.Column("lastUsed", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysSavedRoutes = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesSavedRoutes = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoSavedRoutes = new TableInfo("saved_routes", _columnsSavedRoutes, _foreignKeysSavedRoutes, _indicesSavedRoutes);
        final TableInfo _existingSavedRoutes = TableInfo.read(db, "saved_routes");
        if (!_infoSavedRoutes.equals(_existingSavedRoutes)) {
          return new RoomOpenHelper.ValidationResult(false, "saved_routes(com.example.mentra.core.data.local.entity.SavedRouteEntity).\n"
                  + " Expected:\n" + _infoSavedRoutes + "\n"
                  + " Found:\n" + _existingSavedRoutes);
        }
        final HashMap<String, TableInfo.Column> _columnsRoutePoints = new HashMap<String, TableInfo.Column>(6);
        _columnsRoutePoints.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRoutePoints.put("routeId", new TableInfo.Column("routeId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRoutePoints.put("latitude", new TableInfo.Column("latitude", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRoutePoints.put("longitude", new TableInfo.Column("longitude", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRoutePoints.put("sequence", new TableInfo.Column("sequence", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRoutePoints.put("instruction", new TableInfo.Column("instruction", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysRoutePoints = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesRoutePoints = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoRoutePoints = new TableInfo("route_points", _columnsRoutePoints, _foreignKeysRoutePoints, _indicesRoutePoints);
        final TableInfo _existingRoutePoints = TableInfo.read(db, "route_points");
        if (!_infoRoutePoints.equals(_existingRoutePoints)) {
          return new RoomOpenHelper.ValidationResult(false, "route_points(com.example.mentra.core.data.local.entity.RoutePointEntity).\n"
                  + " Expected:\n" + _infoRoutePoints + "\n"
                  + " Found:\n" + _existingRoutePoints);
        }
        final HashMap<String, TableInfo.Column> _columnsPoi = new HashMap<String, TableInfo.Column>(8);
        _columnsPoi.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPoi.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPoi.put("category", new TableInfo.Column("category", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPoi.put("latitude", new TableInfo.Column("latitude", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPoi.put("longitude", new TableInfo.Column("longitude", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPoi.put("address", new TableInfo.Column("address", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPoi.put("rating", new TableInfo.Column("rating", "REAL", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPoi.put("isFavorite", new TableInfo.Column("isFavorite", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysPoi = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesPoi = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoPoi = new TableInfo("poi", _columnsPoi, _foreignKeysPoi, _indicesPoi);
        final TableInfo _existingPoi = TableInfo.read(db, "poi");
        if (!_infoPoi.equals(_existingPoi)) {
          return new RoomOpenHelper.ValidationResult(false, "poi(com.example.mentra.core.data.local.entity.POIEntity).\n"
                  + " Expected:\n" + _infoPoi + "\n"
                  + " Found:\n" + _existingPoi);
        }
        final HashMap<String, TableInfo.Column> _columnsMediaItems = new HashMap<String, TableInfo.Column>(15);
        _columnsMediaItems.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMediaItems.put("title", new TableInfo.Column("title", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMediaItems.put("artist", new TableInfo.Column("artist", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMediaItems.put("album", new TableInfo.Column("album", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMediaItems.put("genre", new TableInfo.Column("genre", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMediaItems.put("duration", new TableInfo.Column("duration", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMediaItems.put("filePath", new TableInfo.Column("filePath", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMediaItems.put("mimeType", new TableInfo.Column("mimeType", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMediaItems.put("size", new TableInfo.Column("size", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMediaItems.put("dateAdded", new TableInfo.Column("dateAdded", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMediaItems.put("dateModified", new TableInfo.Column("dateModified", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMediaItems.put("albumArtPath", new TableInfo.Column("albumArtPath", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMediaItems.put("playCount", new TableInfo.Column("playCount", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMediaItems.put("lastPlayed", new TableInfo.Column("lastPlayed", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMediaItems.put("isFavorite", new TableInfo.Column("isFavorite", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysMediaItems = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesMediaItems = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoMediaItems = new TableInfo("media_items", _columnsMediaItems, _foreignKeysMediaItems, _indicesMediaItems);
        final TableInfo _existingMediaItems = TableInfo.read(db, "media_items");
        if (!_infoMediaItems.equals(_existingMediaItems)) {
          return new RoomOpenHelper.ValidationResult(false, "media_items(com.example.mentra.core.data.local.entity.MediaItemEntity).\n"
                  + " Expected:\n" + _infoMediaItems + "\n"
                  + " Found:\n" + _existingMediaItems);
        }
        final HashMap<String, TableInfo.Column> _columnsPlaylists = new HashMap<String, TableInfo.Column>(7);
        _columnsPlaylists.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPlaylists.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPlaylists.put("description", new TableInfo.Column("description", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPlaylists.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPlaylists.put("updatedAt", new TableInfo.Column("updatedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPlaylists.put("trackCount", new TableInfo.Column("trackCount", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPlaylists.put("coverArtPath", new TableInfo.Column("coverArtPath", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysPlaylists = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesPlaylists = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoPlaylists = new TableInfo("playlists", _columnsPlaylists, _foreignKeysPlaylists, _indicesPlaylists);
        final TableInfo _existingPlaylists = TableInfo.read(db, "playlists");
        if (!_infoPlaylists.equals(_existingPlaylists)) {
          return new RoomOpenHelper.ValidationResult(false, "playlists(com.example.mentra.core.data.local.entity.PlaylistEntity).\n"
                  + " Expected:\n" + _infoPlaylists + "\n"
                  + " Found:\n" + _existingPlaylists);
        }
        final HashMap<String, TableInfo.Column> _columnsPlaylistItems = new HashMap<String, TableInfo.Column>(5);
        _columnsPlaylistItems.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPlaylistItems.put("playlistId", new TableInfo.Column("playlistId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPlaylistItems.put("mediaId", new TableInfo.Column("mediaId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPlaylistItems.put("position", new TableInfo.Column("position", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPlaylistItems.put("addedAt", new TableInfo.Column("addedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysPlaylistItems = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesPlaylistItems = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoPlaylistItems = new TableInfo("playlist_items", _columnsPlaylistItems, _foreignKeysPlaylistItems, _indicesPlaylistItems);
        final TableInfo _existingPlaylistItems = TableInfo.read(db, "playlist_items");
        if (!_infoPlaylistItems.equals(_existingPlaylistItems)) {
          return new RoomOpenHelper.ValidationResult(false, "playlist_items(com.example.mentra.core.data.local.entity.PlaylistItemEntity).\n"
                  + " Expected:\n" + _infoPlaylistItems + "\n"
                  + " Found:\n" + _existingPlaylistItems);
        }
        final HashMap<String, TableInfo.Column> _columnsShellHistory = new HashMap<String, TableInfo.Column>(7);
        _columnsShellHistory.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsShellHistory.put("command", new TableInfo.Column("command", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsShellHistory.put("originalLanguage", new TableInfo.Column("originalLanguage", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsShellHistory.put("translatedCommand", new TableInfo.Column("translatedCommand", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsShellHistory.put("result", new TableInfo.Column("result", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsShellHistory.put("success", new TableInfo.Column("success", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsShellHistory.put("timestamp", new TableInfo.Column("timestamp", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysShellHistory = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesShellHistory = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoShellHistory = new TableInfo("shell_history", _columnsShellHistory, _foreignKeysShellHistory, _indicesShellHistory);
        final TableInfo _existingShellHistory = TableInfo.read(db, "shell_history");
        if (!_infoShellHistory.equals(_existingShellHistory)) {
          return new RoomOpenHelper.ValidationResult(false, "shell_history(com.example.mentra.core.data.local.entity.ShellHistoryEntity).\n"
                  + " Expected:\n" + _infoShellHistory + "\n"
                  + " Found:\n" + _existingShellHistory);
        }
        final HashMap<String, TableInfo.Column> _columnsShellAliases = new HashMap<String, TableInfo.Column>(4);
        _columnsShellAliases.put("alias", new TableInfo.Column("alias", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsShellAliases.put("target", new TableInfo.Column("target", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsShellAliases.put("description", new TableInfo.Column("description", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsShellAliases.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysShellAliases = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesShellAliases = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoShellAliases = new TableInfo("shell_aliases", _columnsShellAliases, _foreignKeysShellAliases, _indicesShellAliases);
        final TableInfo _existingShellAliases = TableInfo.read(db, "shell_aliases");
        if (!_infoShellAliases.equals(_existingShellAliases)) {
          return new RoomOpenHelper.ValidationResult(false, "shell_aliases(com.example.mentra.core.data.local.entity.ShellAliasEntity).\n"
                  + " Expected:\n" + _infoShellAliases + "\n"
                  + " Found:\n" + _existingShellAliases);
        }
        final HashMap<String, TableInfo.Column> _columnsShellScripts = new HashMap<String, TableInfo.Column>(8);
        _columnsShellScripts.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsShellScripts.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsShellScripts.put("content", new TableInfo.Column("content", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsShellScripts.put("description", new TableInfo.Column("description", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsShellScripts.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsShellScripts.put("updatedAt", new TableInfo.Column("updatedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsShellScripts.put("executionCount", new TableInfo.Column("executionCount", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsShellScripts.put("lastExecuted", new TableInfo.Column("lastExecuted", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysShellScripts = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesShellScripts = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoShellScripts = new TableInfo("shell_scripts", _columnsShellScripts, _foreignKeysShellScripts, _indicesShellScripts);
        final TableInfo _existingShellScripts = TableInfo.read(db, "shell_scripts");
        if (!_infoShellScripts.equals(_existingShellScripts)) {
          return new RoomOpenHelper.ValidationResult(false, "shell_scripts(com.example.mentra.core.data.local.entity.ShellScriptEntity).\n"
                  + " Expected:\n" + _infoShellScripts + "\n"
                  + " Found:\n" + _existingShellScripts);
        }
        final HashMap<String, TableInfo.Column> _columnsShellTriggers = new HashMap<String, TableInfo.Column>(6);
        _columnsShellTriggers.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsShellTriggers.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsShellTriggers.put("triggerType", new TableInfo.Column("triggerType", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsShellTriggers.put("scriptId", new TableInfo.Column("scriptId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsShellTriggers.put("enabled", new TableInfo.Column("enabled", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsShellTriggers.put("conditions", new TableInfo.Column("conditions", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysShellTriggers = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesShellTriggers = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoShellTriggers = new TableInfo("shell_triggers", _columnsShellTriggers, _foreignKeysShellTriggers, _indicesShellTriggers);
        final TableInfo _existingShellTriggers = TableInfo.read(db, "shell_triggers");
        if (!_infoShellTriggers.equals(_existingShellTriggers)) {
          return new RoomOpenHelper.ValidationResult(false, "shell_triggers(com.example.mentra.core.data.local.entity.ShellTriggerEntity).\n"
                  + " Expected:\n" + _infoShellTriggers + "\n"
                  + " Found:\n" + _existingShellTriggers);
        }
        final HashMap<String, TableInfo.Column> _columnsUserProfile = new HashMap<String, TableInfo.Column>(10);
        _columnsUserProfile.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUserProfile.put("name", new TableInfo.Column("name", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUserProfile.put("height", new TableInfo.Column("height", "REAL", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUserProfile.put("weight", new TableInfo.Column("weight", "REAL", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUserProfile.put("age", new TableInfo.Column("age", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUserProfile.put("gender", new TableInfo.Column("gender", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUserProfile.put("dailyStepGoal", new TableInfo.Column("dailyStepGoal", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUserProfile.put("useMetricSystem", new TableInfo.Column("useMetricSystem", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUserProfile.put("theme", new TableInfo.Column("theme", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUserProfile.put("language", new TableInfo.Column("language", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysUserProfile = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesUserProfile = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoUserProfile = new TableInfo("user_profile", _columnsUserProfile, _foreignKeysUserProfile, _indicesUserProfile);
        final TableInfo _existingUserProfile = TableInfo.read(db, "user_profile");
        if (!_infoUserProfile.equals(_existingUserProfile)) {
          return new RoomOpenHelper.ValidationResult(false, "user_profile(com.example.mentra.core.data.local.entity.UserProfileEntity).\n"
                  + " Expected:\n" + _infoUserProfile + "\n"
                  + " Found:\n" + _existingUserProfile);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "c038110da0aee065a29d0518de716b8a", "241d51cd97df6cad8e4b14a48bf101ed");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "activity_records","health_stats","sleep_data","saved_routes","route_points","poi","media_items","playlists","playlist_items","shell_history","shell_aliases","shell_scripts","shell_triggers","user_profile");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    try {
      super.beginTransaction();
      _db.execSQL("DELETE FROM `activity_records`");
      _db.execSQL("DELETE FROM `health_stats`");
      _db.execSQL("DELETE FROM `sleep_data`");
      _db.execSQL("DELETE FROM `saved_routes`");
      _db.execSQL("DELETE FROM `route_points`");
      _db.execSQL("DELETE FROM `poi`");
      _db.execSQL("DELETE FROM `media_items`");
      _db.execSQL("DELETE FROM `playlists`");
      _db.execSQL("DELETE FROM `playlist_items`");
      _db.execSQL("DELETE FROM `shell_history`");
      _db.execSQL("DELETE FROM `shell_aliases`");
      _db.execSQL("DELETE FROM `shell_scripts`");
      _db.execSQL("DELETE FROM `shell_triggers`");
      _db.execSQL("DELETE FROM `user_profile`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(ActivityDao.class, ActivityDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(HealthStatsDao.class, HealthStatsDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(SleepDao.class, SleepDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(SavedRouteDao.class, SavedRouteDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(RoutePointDao.class, RoutePointDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(POIDao.class, POIDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(MediaItemDao.class, MediaItemDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(PlaylistDao.class, PlaylistDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(PlaylistItemDao.class, PlaylistItemDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(ShellHistoryDao.class, ShellHistoryDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(ShellAliasDao.class, ShellAliasDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(ShellScriptDao.class, ShellScriptDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(ShellTriggerDao.class, ShellTriggerDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(UserProfileDao.class, UserProfileDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public ActivityDao activityDao() {
    if (_activityDao != null) {
      return _activityDao;
    } else {
      synchronized(this) {
        if(_activityDao == null) {
          _activityDao = new ActivityDao_Impl(this);
        }
        return _activityDao;
      }
    }
  }

  @Override
  public HealthStatsDao healthStatsDao() {
    if (_healthStatsDao != null) {
      return _healthStatsDao;
    } else {
      synchronized(this) {
        if(_healthStatsDao == null) {
          _healthStatsDao = new HealthStatsDao_Impl(this);
        }
        return _healthStatsDao;
      }
    }
  }

  @Override
  public SleepDao sleepDao() {
    if (_sleepDao != null) {
      return _sleepDao;
    } else {
      synchronized(this) {
        if(_sleepDao == null) {
          _sleepDao = new SleepDao_Impl(this);
        }
        return _sleepDao;
      }
    }
  }

  @Override
  public SavedRouteDao savedRouteDao() {
    if (_savedRouteDao != null) {
      return _savedRouteDao;
    } else {
      synchronized(this) {
        if(_savedRouteDao == null) {
          _savedRouteDao = new SavedRouteDao_Impl(this);
        }
        return _savedRouteDao;
      }
    }
  }

  @Override
  public RoutePointDao routePointDao() {
    if (_routePointDao != null) {
      return _routePointDao;
    } else {
      synchronized(this) {
        if(_routePointDao == null) {
          _routePointDao = new RoutePointDao_Impl(this);
        }
        return _routePointDao;
      }
    }
  }

  @Override
  public POIDao poiDao() {
    if (_pOIDao != null) {
      return _pOIDao;
    } else {
      synchronized(this) {
        if(_pOIDao == null) {
          _pOIDao = new POIDao_Impl(this);
        }
        return _pOIDao;
      }
    }
  }

  @Override
  public MediaItemDao mediaItemDao() {
    if (_mediaItemDao != null) {
      return _mediaItemDao;
    } else {
      synchronized(this) {
        if(_mediaItemDao == null) {
          _mediaItemDao = new MediaItemDao_Impl(this);
        }
        return _mediaItemDao;
      }
    }
  }

  @Override
  public PlaylistDao playlistDao() {
    if (_playlistDao != null) {
      return _playlistDao;
    } else {
      synchronized(this) {
        if(_playlistDao == null) {
          _playlistDao = new PlaylistDao_Impl(this);
        }
        return _playlistDao;
      }
    }
  }

  @Override
  public PlaylistItemDao playlistItemDao() {
    if (_playlistItemDao != null) {
      return _playlistItemDao;
    } else {
      synchronized(this) {
        if(_playlistItemDao == null) {
          _playlistItemDao = new PlaylistItemDao_Impl(this);
        }
        return _playlistItemDao;
      }
    }
  }

  @Override
  public ShellHistoryDao shellHistoryDao() {
    if (_shellHistoryDao != null) {
      return _shellHistoryDao;
    } else {
      synchronized(this) {
        if(_shellHistoryDao == null) {
          _shellHistoryDao = new ShellHistoryDao_Impl(this);
        }
        return _shellHistoryDao;
      }
    }
  }

  @Override
  public ShellAliasDao shellAliasDao() {
    if (_shellAliasDao != null) {
      return _shellAliasDao;
    } else {
      synchronized(this) {
        if(_shellAliasDao == null) {
          _shellAliasDao = new ShellAliasDao_Impl(this);
        }
        return _shellAliasDao;
      }
    }
  }

  @Override
  public ShellScriptDao shellScriptDao() {
    if (_shellScriptDao != null) {
      return _shellScriptDao;
    } else {
      synchronized(this) {
        if(_shellScriptDao == null) {
          _shellScriptDao = new ShellScriptDao_Impl(this);
        }
        return _shellScriptDao;
      }
    }
  }

  @Override
  public ShellTriggerDao shellTriggerDao() {
    if (_shellTriggerDao != null) {
      return _shellTriggerDao;
    } else {
      synchronized(this) {
        if(_shellTriggerDao == null) {
          _shellTriggerDao = new ShellTriggerDao_Impl(this);
        }
        return _shellTriggerDao;
      }
    }
  }

  @Override
  public UserProfileDao userProfileDao() {
    if (_userProfileDao != null) {
      return _userProfileDao;
    } else {
      synchronized(this) {
        if(_userProfileDao == null) {
          _userProfileDao = new UserProfileDao_Impl(this);
        }
        return _userProfileDao;
      }
    }
  }
}
