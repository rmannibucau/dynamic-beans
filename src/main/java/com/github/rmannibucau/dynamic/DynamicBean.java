package com.github.rmannibucau.dynamic;

import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyCodeSource;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.MalformedURLException;
import java.net.URL;

public abstract class DynamicBean {
    public static <T> T newDynamicBean(final Class<T> clazz, final String path) throws IOException {
        return newDynamicBean(clazz, path, -1);
    }

    public static <T> T newDynamicBean(final Class<T> clazz, final String path, final long refreshTimeout) throws IOException {
        return newDynamicBean(clazz, path, refreshTimeout, null);
    }

    public static <T> T newDynamicBean(final Class<T> clazz, final String path, final long refreshTimeout, final InstanceFactory factory) throws IOException {
        final ClassLoader loader = loader(clazz.getClassLoader());
        return clazz.cast(Proxy.newProxyInstance(loader, new Class<?>[] { Closeable.class, clazz }, new DynamicHandler(loader, path, refreshTimeout, factory)));
    }

    private static ClassLoader loader(final ClassLoader defaultLoader) {
        final ClassLoader current = Thread.currentThread().getContextClassLoader();
        if (current != null) {
            return current;
        }
        return defaultLoader;
    }

    private static class DynamicHandler implements InvocationHandler {
        private final GroovyClassLoader loader;
        private final long timeout;
        private final String path;
        private final InstanceFactory factory;

        private volatile Class<?> clazz = null;
        private volatile long lastUpdate = -1;
        private InstanceFactory.InstanceHolder delegate;

        public DynamicHandler(final ClassLoader parent, final String path, final long refreshTimeout, final InstanceFactory factory) throws IOException {
            this.path = path;
            this.loader = new GroovyClassLoader(parent);
            this.timeout = refreshTimeout;
            this.factory = factory;
        }

        @Override
        public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
            if (Closeable.class.equals(method.getDeclaringClass())) {
                if (delegate != null) {
                    factory.destroyInstance(delegate);
                }
                return null;
            }

            if (clazz == null || (timeout > 0 && System.currentTimeMillis() - lastUpdate > timeout)) {
                synchronized (this) {
                    final long now = System.currentTimeMillis();
                    if (now - lastUpdate > timeout) {
                        clazz = loader.parseClass(new GroovyCodeSource(findResource()), timeout <= 0);
                        lastUpdate = now;

                        if (factory != null) {
                            if (delegate != null) {
                                factory.destroyInstance(delegate);
                            }
                            delegate = factory.newInstance(clazz);
                        } else {
                            final Object instance = clazz.newInstance();
                            delegate = new InstanceFactory.InstanceHolder() {
                                @Override
                                public Object getInstance() {
                                    return instance;
                                }
                            };
                        }
                    }
                }
            }

            return method.invoke(delegate.getInstance(), args);
        }

        private URL findResource() {
            final File file = new File(path);
            if (file.exists()) {
                try {
                    return file.toURI().toURL();
                } catch (final MalformedURLException e) {
                    // no-op
                }
            }
            return loader.getResource(path);
        }
    }
}
