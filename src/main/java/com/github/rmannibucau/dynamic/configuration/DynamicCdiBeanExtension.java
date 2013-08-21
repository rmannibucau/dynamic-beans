package com.github.rmannibucau.dynamic.configuration;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.BeforeShutdown;
import javax.enterprise.inject.spi.Extension;

public class DynamicCdiBeanExtension implements Extension {
    void cleanup(final @Observes BeforeShutdown shutdown) {
        DynamicHandler.cleanup();
    }
}
