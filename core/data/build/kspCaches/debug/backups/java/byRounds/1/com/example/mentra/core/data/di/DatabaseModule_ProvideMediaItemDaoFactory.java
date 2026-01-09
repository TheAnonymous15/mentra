package com.example.mentra.core.data.di;

import com.example.mentra.core.data.local.MentraDatabase;
import com.example.mentra.core.data.local.dao.MediaItemDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast",
    "deprecation"
})
public final class DatabaseModule_ProvideMediaItemDaoFactory implements Factory<MediaItemDao> {
  private final Provider<MentraDatabase> databaseProvider;

  public DatabaseModule_ProvideMediaItemDaoFactory(Provider<MentraDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @Override
  public MediaItemDao get() {
    return provideMediaItemDao(databaseProvider.get());
  }

  public static DatabaseModule_ProvideMediaItemDaoFactory create(
      Provider<MentraDatabase> databaseProvider) {
    return new DatabaseModule_ProvideMediaItemDaoFactory(databaseProvider);
  }

  public static MediaItemDao provideMediaItemDao(MentraDatabase database) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideMediaItemDao(database));
  }
}
