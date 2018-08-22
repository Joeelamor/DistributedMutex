package util;

import com.amihaiemil.camel.*;

import java.io.*;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class ConfigLoader {
    private String ConfigFileName;

    public ConfigLoader(String configFileName) {
        ConfigFileName = configFileName;
    }

    public Collection<HostConfig> loadExistingHostConfigs() throws IOException {
        File configFile = new File(this.ConfigFileName);
        YamlInput yamlInput;
        try {
            yamlInput = Yaml.createYamlInput(configFile);
        } catch (FileNotFoundException e) {
            return new ArrayList<>();
        }
        YamlSequence yamlSequence = yamlInput.readYamlSequence();

        int size = yamlSequence.size();
        Collection<HostConfig> existingNodes = new ArrayList<>();
        for (int i = 0; i < size; ++i) {
            YamlMapping y = yamlSequence.yamlMapping(i);
            HostConfig host = new HostConfig(
                Integer.parseInt(y.string("id")),
                y.string("IP"),
                Integer.parseInt(y.string("port"))
            );
            existingNodes.add(host);
        }
        return existingNodes;
    }

    public void dumpHostConfigs(Collection<HostConfig> hostConfigs) throws IOException {
        if (hostConfigs == null || hostConfigs.size() == 0) {
            try (FileChannel outChan = new FileOutputStream(this.ConfigFileName, true).getChannel()) {
                outChan.truncate(0);
            }
            return;
        }
        ArrayList<Object> hostConfigObjects = new ArrayList<>(hostConfigs.size());
        hostConfigObjects.addAll(hostConfigs);
        YamlSequence yamlSequence = new YamlCollectionDump(hostConfigObjects).represent();
        BufferedWriter writer = new BufferedWriter(new FileWriter(this.ConfigFileName));
        writer.write(yamlSequence.toString());
        writer.close();
    }
}
