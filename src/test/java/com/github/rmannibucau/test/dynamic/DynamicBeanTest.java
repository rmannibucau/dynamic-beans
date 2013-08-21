package com.github.rmannibucau.test.dynamic;

import com.github.rmannibucau.dynamic.DynamicBean;
import org.junit.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class DynamicBeanTest {
    @Test
    public void newBean() throws IOException {
        final Dynamic d = DynamicBean.newDynamicBean(Dynamic.class, "com/github/rmannibucau/test/dynamic/DynamicImpl.groovy");
        assertEquals("dynamic", d.value());
    }

    @Test
    public void updates() throws IOException {
        final String path = "com/github/rmannibucau/test/dynamic/RefreshableDynamicImpl.groovy";
        final File updated = new File("target/test-classes/" + path);
        updated.getParentFile().mkdirs();

        final String template = "package com.github.rmannibucau.test.dynamic\n" +
            "\n" +
            "class RefreshableDynamicImpl implements DynamicBeanTest.Dynamic {\n" +
            "    @Override\n" +
            "    String value() {\n" +
            "        \"%s\"\n" +
            "    }\n" +
            "}\n";

        write(updated, String.format(template, "first"));

        final Dynamic d = DynamicBean.newDynamicBean(Dynamic.class, path, 500);
        assertEquals("first", d.value());

        write(updated, String.format(template, "second"));
        try { // wait 500ms for update
            Thread.sleep(1000);
        } catch (final InterruptedException e) {
            // no-op
        }
        assertEquals("second", d.value());
    }

    private void write(final File updated, final String value) throws IOException {
        final FileWriter writer = new FileWriter(updated);
        writer.write(value);
        writer.close();
    }

    public static interface Dynamic {
        String value();
    }
}
