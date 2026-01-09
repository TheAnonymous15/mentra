package com.example.mentra.core.data.di;

import com.example.mentra.core.data.local.MentraDatabase;
import com.example.mentra.core.data.local.dao.HealthStatsDao;
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
public final class DatabaseModule_ProvideHealthStatsDaoFactory implements Factory<HealthStatsDao> {
  private final Provider<MentraDatabase> databaseProvider;

  public DatabaseModule_ProvideHealthStatsDaoFactory(Provider<MentraDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @Override
  public HealthStatsDao get() {
    return provideHealthStatsDao(databaseProvider.get());
  }

  public static DatabaseModule_ProvideHealthStatsDaoFactory create(
      Provider<MentraDatabase> databaseProvider) {
    return new DatabaseModule_ProvideHealthStatsDaoFactory(databaseProvider);
  }

  public static HealthStatsDao provideHealthStatsDao(MentraDatabase database) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideHealthStatsDao(database));
  }
}
