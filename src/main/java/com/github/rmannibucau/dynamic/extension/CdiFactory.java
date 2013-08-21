package com.github.rmannibucau.dynamic.extension;

import com.github.rmannibucau.dynamic.factory.InstanceFactory;
import org.apache.deltaspike.core.api.provider.BeanManagerProvider;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.InjectionTarget;

public class CdiFactory implements InstanceFactory {
    @Override
    public InstanceHolder newInstance(final Class<?> clazz) {
        final BeanManager beanManager = BeanManagerProvider.getInstance().getBeanManager();
        final AnnotatedType at = beanManager.createAnnotatedType(clazz);
        final InjectionTarget it = beanManager.createInjectionTarget(at);
        final CreationalContext cc = beanManager.createCreationalContext(null);
        final Object instance = it.produce(cc);
        it.inject(instance, cc);
        it.postConstruct(instance);
        return new CdiInstanceHolder(instance, cc, it);
    }

    @Override
    public void destroyInstance(final InstanceHolder instance) {
        CdiInstanceHolder.class.cast(instance).destroy();
    }

    private static class CdiInstanceHolder implements InstanceHolder {
        private final Object instance;
        private final CreationalContext creationalContext;
        private final InjectionTarget injectionTarget;

        public CdiInstanceHolder(final Object instance, final CreationalContext cc, final InjectionTarget it) {
            this.instance = instance;
            this.creationalContext = cc;
            this.injectionTarget = it;
        }

        @Override
        public Object getInstance() {
            return instance;
        }

        public void destroy() {
            injectionTarget.preDestroy(instance);
            creationalContext.release();
        }
    }
}
