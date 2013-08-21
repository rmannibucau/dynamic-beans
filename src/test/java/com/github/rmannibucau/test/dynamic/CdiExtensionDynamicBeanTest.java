package com.github.rmannibucau.test.dynamic;

import com.github.rmannibucau.cdi.configuration.LightConfigurationExtension;
import com.github.rmannibucau.cdi.configuration.xml.handlers.NamespaceHandler;
import com.github.rmannibucau.dynamic.Dynamic;
import com.github.rmannibucau.dynamic.extension.DynamicCdiBeanExtension;
import com.github.rmannibucau.dynamic.extension.DynamicHandler;
import org.apache.deltaspike.core.api.provider.BeanProvider;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.enterprise.inject.spi.Extension;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import static org.apache.ziplock.JarLocation.jarLocation;
import static org.junit.Assert.assertEquals;

@RunWith(Arquillian.class)
public class CdiExtensionDynamicBeanTest {
    public static final String DYNAMIC_PATH = "target/foo/bar/CdiExtensionDynamicBeanTest.groovy";
    private static final File DYNAMIC_FILE = new File(DYNAMIC_PATH);

    @Deployment
    public static Archive<?> war() throws IOException {
        DYNAMIC_FILE.getParentFile().mkdirs();
        write(DYNAMIC_FILE, "class DynamicImpl implements com.github.rmannibucau.test.dynamic.CdiExtensionDynamicBeanTest.CdiDynamic {\n" +
            "    String init = \"dynamic\"\n" +
            "}\n");

        return ShrinkWrap.create(WebArchive.class, "config.war")
                .addPackages(true, "com.github.rmannibucau.dynamic")
                .addAsServiceProvider(Extension.class, DynamicCdiBeanExtension.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                .addClass(ABean.class)
                .addAsLibraries(jarLocation(BeanProvider.class), jarLocation(LightConfigurationExtension.class));
    }

    @Inject
    private CdiDynamic dynamic;

    @Test
    public void dynamic() throws InterruptedException, IOException {
        assertEquals("dynamic", dynamic.getInit());

        write(DYNAMIC_FILE, "class DynamicImpl implements com.github.rmannibucau.test.dynamic.CdiExtensionDynamicBeanTest.CdiDynamic {\n" +
            "    @javax.inject.Inject\n" +
            "    com.github.rmannibucau.test.dynamic.CdiExtensionDynamicBeanTest.ABean bean\n" +
            "    \n" +
            "    String init\n" +
            "    \n" +
            "    @javax.annotation.PostConstruct\n" +
            "    public void init() {\n" +
            "        init = bean.toString() + \"-re-dynamic\"\n" +
            "    }\n" +
            "}\n");
        Thread.sleep(1000);
        assertEquals("ABean-re-dynamic", dynamic.getInit());
    }

    private static void write(final File updated, final String value) throws IOException {
        final FileWriter writer = new FileWriter(updated);
        writer.write(value);
        writer.close();
    }

    @Dynamic(timeout = 500, path = DYNAMIC_PATH)
    public static interface CdiDynamic {
        String getInit();
    }

    public static class ABean {
        @Override
        public String toString() {
            return "ABean";
        }
    }
}
