package com.example.mentra.core.common;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

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
public final class EventBus_Factory implements Factory<EventBus> {
  @Override
  public EventBus get() {
    return newInstance();
  }

  public static EventBus_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static EventBus newInstance() {
    return new EventBus();
  }

  private static final class InstanceHolder {
    private static final EventBus_Factory INSTANCE = new EventBus_Factory();
  }
}
