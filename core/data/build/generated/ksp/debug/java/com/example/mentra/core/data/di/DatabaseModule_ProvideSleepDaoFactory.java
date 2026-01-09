package com.example.mentra.core.data.di;

import com.example.mentra.core.data.local.MentraDatabase;
import com.example.mentra.core.data.local.dao.SleepDao;
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
public final class DatabaseModule_ProvideSleepDaoFactory implements Factory<SleepDao> {
  private final Provider<MentraDatabase> databaseProvider;

  public DatabaseModule_ProvideSleepDaoFactory(Provider<MentraDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @Override
  public SleepDao get() {
    return provideSleepDao(databaseProvider.get());
  }

  public static DatabaseModule_ProvideSleepDaoFactory create(
      Provider<MentraDatabase> databaseProvider) {
    return new DatabaseModule_ProvideSleepDaoFactory(databaseProvider);
  }

  public static SleepDao provideSleepDao(MentraDatabase database) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideSleepDao(database));
  }
}
