package com.example.mentra.core.data.di;

import com.example.mentra.core.data.local.MentraDatabase;
import com.example.mentra.core.data.local.dao.RoutePointDao;
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
public final class DatabaseModule_ProvideRoutePointDaoFactory implements Factory<RoutePointDao> {
  private final Provider<MentraDatabase> databaseProvider;

  public DatabaseModule_ProvideRoutePointDaoFactory(Provider<MentraDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @Override
  public RoutePointDao get() {
    return provideRoutePointDao(databaseProvider.get());
  }

  public static DatabaseModule_ProvideRoutePointDaoFactory create(
      Provider<MentraDatabase> databaseProvider) {
    return new DatabaseModule_ProvideRoutePointDaoFactory(databaseProvider);
  }

  public static RoutePointDao provideRoutePointDao(MentraDatabase database) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideRoutePointDao(database));
  }
}
