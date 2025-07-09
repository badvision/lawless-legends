package jace.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import org.junit.Test;

public class DumpConfigTest {
    @Test
    public void dumpConfig() throws Exception {
        // Initialize JavaFX first
        new javafx.embed.swing.JFXPanel();
        
        // Build tree first to see baseline children
        Configuration.BASE = null;
        Configuration.initializeBaseConfiguration();
        Configuration.buildTree();
        System.out.println("Base children before load:");
        for(Configuration.ConfigNode child : Configuration.BASE.getChildren()) {
            java.lang.reflect.Field fidc = child.getClass().getDeclaredField("id");fidc.setAccessible(true);
            java.lang.reflect.Field fnamec = child.getClass().getDeclaredField("name");fnamec.setAccessible(true);
            System.out.println("child id="+fidc.get(child)+" name="+fnamec.get(child));
        }

        // Try to load from JSON first (new format)
        File jsonFile = new File(System.getProperty("user.home"), ".jace.json");
        if (jsonFile.exists()) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                AppSettingsDTO dto = mapper.readValue(jsonFile, AppSettingsDTO.class);
                System.out.println("JSON Config dump loaded:");
                System.out.println("  version: " + dto.version);
                if (dto.ui != null) {
                    System.out.println("  UI settings:");
                    System.out.println("    windowWidth: " + dto.ui.windowWidth);
                    System.out.println("    windowHeight: " + dto.ui.windowHeight);
                    System.out.println("    windowSizeIndex: " + dto.ui.windowSizeIndex);
                    System.out.println("    fullscreen: " + dto.ui.fullscreen);
                    System.out.println("    videoMode: " + dto.ui.videoMode);
                    System.out.println("    musicVolume: " + dto.ui.musicVolume);
                    System.out.println("    sfxVolume: " + dto.ui.sfxVolume);
                    System.out.println("    soundtrackSelection: " + dto.ui.soundtrackSelection);
                    System.out.println("    aspectRatio: " + dto.ui.aspectRatio);
                }
            } catch (Exception e) {
                System.out.println("Failed to parse JSON config: " + e.getMessage());
            }
        } else {
            System.out.println("No JSON config file found at: " + jsonFile.getAbsolutePath());
        }

        // Also try legacy binary format as fallback
        File legacyFile = new File(System.getProperty("user.home"), ".jace.conf");
        if (legacyFile.exists()) {
            try(ObjectInputStream ois = new ObjectInputStream(new FileInputStream(legacyFile))) {
                Object obj = ois.readObject();
                if (obj instanceof Configuration.ConfigNode root) {
                    System.out.println("Legacy binary config dump loaded root children:");
                    for(Configuration.ConfigNode child : root.getChildren()) {
                        java.lang.reflect.Field fid = child.getClass().getDeclaredField("id");
                        fid.setAccessible(true);
                        java.lang.reflect.Field fname = child.getClass().getDeclaredField("name");
                        fname.setAccessible(true);
                        System.out.println("child id="+fid.get(child)+" name="+fname.get(child));
                        System.out.println(" settings="+child.settings.keySet());
                        for(Configuration.ConfigNode grand: child.getChildren()) {
                            java.lang.reflect.Field gfid = grand.getClass().getDeclaredField("id");
                            gfid.setAccessible(true);
                            java.lang.reflect.Field gfname = grand.getClass().getDeclaredField("name");
                            gfname.setAccessible(true);
                            System.out.println("  grandchild id="+gfid.get(grand)+" name="+gfname.get(grand));
                        }
                    }
                } else {
                    System.out.println("Not ConfigNode root??"+obj);
                }
            } catch (Exception e) {
                System.out.println("Failed to read legacy config: " + e.getMessage());
            }
        } else {
            System.out.println("No legacy config file found at: " + legacyFile.getAbsolutePath());
        }

        // Dump current in-memory configuration tree structure
        System.out.println("Current in-memory configuration tree:");
        dumpConfigTree(Configuration.BASE, 0);
    }

    private void dumpConfigTree(Configuration.ConfigNode node, int depth) {
        if (node == null) return;
        
        String indent = "  ".repeat(depth);
        try {
            java.lang.reflect.Field fid = node.getClass().getDeclaredField("id");
            fid.setAccessible(true);
            java.lang.reflect.Field fname = node.getClass().getDeclaredField("name");
            fname.setAccessible(true);
            
            System.out.println(indent + "id=" + fid.get(node) + " name=" + fname.get(node));
            System.out.println(indent + "settings=" + node.settings.keySet());
            
            for (Configuration.ConfigNode child : node.getChildren()) {
                dumpConfigTree(child, depth + 1);
            }
        } catch (Exception e) {
            System.out.println(indent + "Error accessing node: " + e.getMessage());
        }
    }
} 