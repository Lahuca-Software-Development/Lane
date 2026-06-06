package com.lahuca.lane.utilities;

import com.google.gson.typeadapters.RuntimeTypeAdapterFactory;

public class GsonUtilities {

    public static <T> RuntimeTypeAdapterFactory<T> getSealedRuntimeTypeAdapterFactory(Class<T> base) {
        RuntimeTypeAdapterFactory<T> factory = RuntimeTypeAdapterFactory.of(base);
        Class<?>[] subclasses = base.getPermittedSubclasses();
        if (subclasses == null || subclasses.length == 0) {
            throw new IllegalArgumentException(base.getName() + " is not a sealed type or does not list any permitted subclasses");
        }
        for (Class<?> subclass : subclasses) {
            registerSealedSubclassesRuntimeTypeAdapterFactory(factory, subclass);
        }
        return factory;
    }

    @SuppressWarnings("unchecked")
    private static <T> void registerSealedSubclassesRuntimeTypeAdapterFactory(RuntimeTypeAdapterFactory<T> factory, Class<?> base) {
        Class<?>[] subclasses = base.getPermittedSubclasses();
        if (subclasses == null || subclasses.length == 0) {
            Class<? extends T> cast = (Class<T>) base;
            factory.registerSubtype(cast);
            return;
        }
        for (Class<?> subclass : subclasses) {
            registerSealedSubclassesRuntimeTypeAdapterFactory(factory, subclass);
        }
    }

}
