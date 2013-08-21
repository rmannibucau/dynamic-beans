package com.github.rmannibucau.dynamic.configuration;

import com.github.rmannibucau.cdi.configuration.ConfigurationException;
import com.github.rmannibucau.cdi.configuration.model.ConfigBean;
import com.github.rmannibucau.cdi.configuration.xml.handlers.NamespaceHandlerSupport;
import com.github.rmannibucau.dynamic.DynamicBean;
import org.xml.sax.Attributes;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

public class DynamicHandler extends NamespaceHandlerSupport {
    private static final Collection<Object> INSTANCES = new ArrayList<Object>();

    @Override
    public String supportedUri() {
        return "dynamic";
    }

    @Override
    public ConfigBean createBean(final String localName, final Attributes attributes) {
        final String api = attributes.getValue("api");
        final ConfigBean bean = new ConfigBean(localName, api,
                                                attributes.getValue("scope"), attributes.getValue("qualifier"),
                                                DynamicFactory.class.getName(), "create", null, null, false);

        bean.getDirectAttributes().put("api", api);

        final String timeout = attributes.getValue("timeout");
        if (timeout != null) {
            bean.getDirectAttributes().put("timeout", timeout);
        }

        final String path = attributes.getValue("path");
        if (path != null) {
            bean.getDirectAttributes().put("path", path);
        }

        return bean;
    }

    public static void cleanup() {
        for (final Object o : INSTANCES) {
            try {
                Closeable.class.cast(o).close();
            } catch (final IOException e) {
                // no-op
            }
        }
    }

    public static class DynamicFactory {
        private String api;
        private String path;
        private int timeout = -1;

        public Object create() {
            try {
                final Class<?> clazz = Thread.currentThread().getContextClassLoader().loadClass(api);
                if (path == null) { // default
                    path = clazz.getName().replace(".", "/") + ".groovy";
                }

                final Object o = DynamicBean.newDynamicBean(clazz, path, timeout, new CdiFactory());

                synchronized (INSTANCES) { // TODO: will only work for app scoped beans
                    INSTANCES.add(o);
                }

                return o;
            } catch (final Exception e) {
                throw new ConfigurationException(e);
            }
        }
    }
}
