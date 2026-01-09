package com.example.mentra.core.data.di;

import com.example.mentra.core.data.local.MentraDatabase;
import com.example.mentra.core.data.local.dao.ActivityDao;
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
public final class DatabaseModule_ProvideActivityDaoFactory implements Factory<ActivityDao> {
  private final Provider<MentraDatabase> databaseProvider;

  public DatabaseModule_ProvideActivityDaoFactory(Provider<MentraDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @Override
  public ActivityDao get() {
    return provideActivityDao(databaseProvider.get());
  }

  public static DatabaseModule_ProvideActivityDaoFactory create(
      Provider<MentraDatabase> databaseProvider) {
    return new DatabaseModule_ProvideActivityDaoFactory(databaseProvider);
  }

  public static ActivityDao provideActivityDao(MentraDatabase database) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideActivityDao(database));
  }
}
