package util;

import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import static org.testng.Assert.*;

public class ConfigLoaderTest {

    @Test
    public void testLoadExistingHostConfigs() throws IOException {
        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource("testLoadHostConfig.yml").getFile());

        ConfigLoader configLoader = new ConfigLoader(file.getAbsolutePath());
        Collection<HostConfig> hostConfigs = configLoader.loadExistingHostConfigs();
        assertEquals(hostConfigs.size(), 3);

        ConfigLoader configLoaderEmpty = new ConfigLoader("file.not.exist");
        Collection<HostConfig> emptyHostConfigs = configLoaderEmpty.loadExistingHostConfigs();
        assertEquals(emptyHostConfigs.size(), 0);
    }

    @Test
    public void testDumpHostConfigs() throws IOException {
        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource("testDumpHostConfig.yml").getFile());

        ConfigLoader configLoader = new ConfigLoader(file.getAbsolutePath());

        Collection<HostConfig> hostConfigs = new ArrayList<>();
        hostConfigs.add(new HostConfig(1, "127.0.0.1", 8090));

        configLoader.dumpHostConfigs(hostConfigs);

        Collection<HostConfig> dumpedHostConfigs = configLoader.loadExistingHostConfigs();
        assertEquals(hostConfigs.size(), 1);

        configLoader.dumpHostConfigs(null);
    }
}