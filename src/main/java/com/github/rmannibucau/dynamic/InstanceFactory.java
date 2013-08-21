package com.github.rmannibucau.dynamic;

public interface InstanceFactory {
    InstanceHolder newInstance(Class<?> clazz);
    void destroyInstance(InstanceHolder instance);

    public static interface InstanceHolder {
        Object getInstance();
    }
}
