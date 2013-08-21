package com.github.rmannibucau.dynamic.configuration;

import com.github.rmannibucau.cdi.configuration.ConfigurationException;
import com.github.rmannibucau.cdi.configuration.model.ConfigBean;
import com.github.rmannibucau.cdi.configuration.xml.handlers.NamespaceHandlerSupport;
import com.github.rmannibucau.dynamic.DynamicBean;
import org.xml.sax.Attributes;

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

    public static class DynamicFactory {
        private String api;
        private String path;
        private int timeout = -1;

        public Object create() {
            try {
                return DynamicBean.newDynamicBean(Thread.currentThread().getContextClassLoader().loadClass(api), path, timeout, new CdiFactory());
            } catch (final Exception e) {
                throw new ConfigurationException(e);
            }
        }
    }
}
