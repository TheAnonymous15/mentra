package com.example.mentra.core.common.permissions;

import android.content.Context;
import com.example.mentra.core.common.EventBus;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
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
public final class PermissionManager_Factory implements Factory<PermissionManager> {
  private final Provider<Context> contextProvider;

  private final Provider<EventBus> eventBusProvider;

  public PermissionManager_Factory(Provider<Context> contextProvider,
      Provider<EventBus> eventBusProvider) {
    this.contextProvider = contextProvider;
    this.eventBusProvider = eventBusProvider;
  }

  @Override
  public PermissionManager get() {
    return newInstance(contextProvider.get(), eventBusProvider.get());
  }

  public static PermissionManager_Factory create(Provider<Context> contextProvider,
      Provider<EventBus> eventBusProvider) {
    return new PermissionManager_Factory(contextProvider, eventBusProvider);
  }

  public static PermissionManager newInstance(Context context, EventBus eventBus) {
    return new PermissionManager(context, eventBus);
  }
}
