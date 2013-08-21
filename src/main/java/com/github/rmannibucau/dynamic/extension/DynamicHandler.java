package com.github.rmannibucau.dynamic.extension;

import com.github.rmannibucau.cdi.configuration.ConfigurationException;
import com.github.rmannibucau.cdi.configuration.model.ConfigBean;
import com.github.rmannibucau.cdi.configuration.xml.handlers.NamespaceHandlerSupport;
import com.github.rmannibucau.dynamic.factory.DynamicBean;
import org.xml.sax.Attributes;

import java.io.Closeable;
import java.io.IOException;

public class DynamicHandler extends NamespaceHandlerSupport {
    @Override
    public String supportedUri() {
        return "dynamic";
    }

    @Override
    public ConfigBean createBean(final String localName, final Attributes attributes) {
        final String api = attributes.getValue("api");
        final ConfigBean bean = new ConfigBean(localName, api,
                                                attributes.getValue("scope"), attributes.getValue("qualifier"),
                                                DynamicFactory.class.getName(), "create", null, "destroy", false);

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

    public static class DynamicFactory {
        private String api;
        private String path;
        private int timeout = -1;
        private Object instance;

        public Object create() {
            try {
                final Class<?> clazz = Thread.currentThread().getContextClassLoader().loadClass(api);
                if (path == null || path.isEmpty()) { // default
                    path = clazz.getName().replace(".", "/") + ".groovy";
                }

                instance = DynamicBean.newDynamicBean(clazz, path, timeout, new CdiFactory());
                return instance;
            } catch (final Exception e) {
                throw new ConfigurationException(e);
            }
        }

        public void destroy() {
            try {
                Closeable.class.cast(instance).close();
            } catch (final IOException e) {
                // no-op
            }
        }
    }
}
