package com.github.rmannibucau.dynamic.extension;

import com.github.rmannibucau.dynamic.Dynamic;
import com.github.rmannibucau.dynamic.factory.DynamicBean;
import org.apache.deltaspike.core.util.bean.BeanBuilder;
import org.apache.deltaspike.core.util.metadata.builder.ContextualLifecycle;

import javax.enterprise.context.Dependent;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import java.io.Closeable;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;

public class DynamicCdiBeanExtension implements Extension {
    private final Map<Class<?>, Dynamic> dynamics = new HashMap<Class<?>, Dynamic>();

    <T> void findDynamicBeans(final @Observes ProcessAnnotatedType<T> pat) {
        final Class<T> javaClass = pat.getAnnotatedType().getJavaClass();
        if (javaClass.isInterface()) {
            final Dynamic dynamic = javaClass.getAnnotation(Dynamic.class);
            if (dynamic != null) {
                dynamics.put(javaClass, dynamic);
            }
        }
    }

    void addDynamicBeans(final @Observes AfterBeanDiscovery abd, final BeanManager beanManager) {
        for (final Map.Entry<Class<?>, Dynamic> entry : dynamics.entrySet()) {
            final Class<?> api = entry.getKey();

            abd.addBean(new BeanBuilder<Object>(beanManager)
                .passivationCapable(true)
                .beanClass(api)
                .types(api, Object.class)
                .scope(findScope(beanManager, api))
                .beanLifecycle(new DynamicBeanFactory(api, entry.getValue().path(), entry.getValue().timeout()))
                .create());
        }
        dynamics.clear();
    }

    private static Class<? extends Annotation> findScope(final BeanManager bm, final Class<?> api) {
        final Annotation[] annotations = api.getAnnotations();
        for (final Annotation a : annotations) {
            final Class<? extends Annotation> annotationType = a.annotationType();
            if (bm.isNormalScope(annotationType)) {
                return annotationType;
            }
        }
        for (final Annotation a : annotations) {
            final Class<? extends Annotation> annotationType = a.annotationType();
            if (bm.isScope(annotationType)) {
                return annotationType;
            }
        }
        return Dependent.class;
    }

    private static class DynamicBeanFactory implements ContextualLifecycle<Object> {
        private final Class<?> api;
        private final String path;
        private final long timeout;

        public DynamicBeanFactory(final Class<?> api, final String path, final long timeout) {
            this.api = api;
            if (path == null || path.isEmpty()) { // default
                this.path = api.getName().replace(".", "/") + ".groovy";
            } else {
                this.path = path;
            }
            this.timeout = timeout;
        }

        @Override
        public Object create(final Bean<Object> bean, final CreationalContext<Object> creationalContext) {
            try {
                return DynamicBean.newDynamicBean(api, path, timeout, new CdiFactory());
            } catch (final IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void destroy(final Bean<Object> bean, final Object instance, final CreationalContext<Object> creationalContext) {
            try {
                Closeable.class.cast(instance).close();
            } catch (final IOException e) {
                // no-op
            }
        }
    }
}
