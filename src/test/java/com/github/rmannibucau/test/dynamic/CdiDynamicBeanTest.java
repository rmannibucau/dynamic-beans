package com.github.rmannibucau.test.dynamic;

import com.github.rmannibucau.cdi.configuration.LightConfigurationExtension;
import com.github.rmannibucau.cdi.configuration.xml.handlers.NamespaceHandler;
import com.github.rmannibucau.dynamic.configuration.DynamicHandler;
import org.apache.deltaspike.core.api.provider.BeanProvider;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;
import javax.inject.Named;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import static org.apache.ziplock.JarLocation.jarLocation;
import static org.junit.Assert.assertEquals;

@RunWith(Arquillian.class)
public class CdiDynamicBeanTest {
    private static final File DYNAMIC_FILE = new File("target/foo/bar/CdiDynamicBeanTest.groovy");

    @Deployment
    public static Archive<?> war() throws IOException {
        DYNAMIC_FILE.getParentFile().mkdirs();
        write(DYNAMIC_FILE, "class DynamicImpl implements com.github.rmannibucau.test.dynamic.CdiDynamicBeanTest.CdiDynamic {\n" +
            "    String init = \"dynamic\"\n" +
            "}\n");

        return ShrinkWrap.create(WebArchive.class, "config.war")
                .addPackages(true, "com.github.rmannibucau.dynamic")
                .addAsServiceProvider(NamespaceHandler.class, DynamicHandler.class)
                .addAsWebInfResource(new StringAsset("<cdi-beans xmlns:dynamic=\"cdi://dynamic\">" +
                    "  <dynamic:dynamicBean api=\"" + CdiDynamic.class.getName() + "\" timeout=\"500\" path=\"" + DYNAMIC_FILE.getAbsolutePath() + "\"/>" +
                    "</cdi-beans>"), "classes/cdi-configuration.xml")
                .addAsLibraries(jarLocation(BeanProvider.class), jarLocation(LightConfigurationExtension.class));
    }

    @Inject
    @Named("dynamicBean")
    private CdiDynamic dynamic;

    @Test
    public void dynamic() throws InterruptedException, IOException {
        assertEquals("dynamic", dynamic.getInit());

        write(DYNAMIC_FILE, "class DynamicImpl implements com.github.rmannibucau.test.dynamic.CdiDynamicBeanTest.CdiDynamic {\n" +
            "    String init\n" +
            "    \n" +
            "    @javax.annotation.PostConstruct\n" +
            "    public void init() {\n" +
            "        init = \"re-dynamic\"\n" +
            "    }\n" +
            "}\n");
        Thread.sleep(1000);
        assertEquals("re-dynamic", dynamic.getInit());
    }

    private static void write(final File updated, final String value) throws IOException {
        final FileWriter writer = new FileWriter(updated);
        writer.write(value);
        writer.close();
    }

    public static interface CdiDynamic {
        String getInit();
    }
}
