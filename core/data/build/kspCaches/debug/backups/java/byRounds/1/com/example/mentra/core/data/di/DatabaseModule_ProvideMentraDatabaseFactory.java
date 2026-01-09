package com.example.mentra.core.data.di;

import android.content.Context;
import com.example.mentra.core.data.local.MentraDatabase;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class DatabaseModule_ProvideMentraDatabaseFactory implements Factory<MentraDatabase> {
  private final Provider<Context> contextProvider;

  public DatabaseModule_ProvideMentraDatabaseFactory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public MentraDatabase get() {
    return provideMentraDatabase(contextProvider.get());
  }

  public static DatabaseModule_ProvideMentraDatabaseFactory create(
      Provider<Context> contextProvider) {
    return new DatabaseModule_ProvideMentraDatabaseFactory(contextProvider);
  }

  public static MentraDatabase provideMentraDatabase(Context context) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideMentraDatabase(context));
  }
}
