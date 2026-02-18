/** 
* Copyright 2024 Brendan Robert
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
**/

package jace.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import jace.Emulator;
import jace.EmulatorUILogic;
import jace.LawlessLegends;
import jace.core.Keyboard;
import jace.core.Utility;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;
import javafx.scene.image.ImageView;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * Manages the configuration state of the emulator components.
 *
 * @author Brendan Robert (BLuRry) brendan.robert@gmail.com
 */
public class Configuration implements Reconfigurable {
    public EmulatorUILogic ui;

    public Configuration() {
        ui = Emulator.getUILogic();
    }

    static ConfigurableField getConfigurableFieldInfo(Reconfigurable subject, String settingName) {
        Field f;
        try {
            f = subject.getClass().getField(settingName);
        } catch (NoSuchFieldException | SecurityException ex) {
            return null;
        }
        ConfigurableField annotation = f.getAnnotation(ConfigurableField.class);
        return annotation;
    }

    public static String getShortName(ConfigurableField f, String longName) {
        return (f != null && !f.shortName().equals("")) ? f.shortName() : longName;
    }

    public static Optional<ImageView> getChangedIcon() {
        return Utility.loadIcon("icon_exclaim.gif").map(ImageView::new);
    }

    @Override
    public String getName() {
        return "Configuration";
    }

    @Override
    public String getShortName() {
        return "cfg";
    }

    @Override
    public void reconfigure() {
    }

    /**
     * Represents a serializable configuration node as part of a tree. The root
     * node should be a single instance (e.g. Computer) The child nodes should
     * be all object instances that stem from each object The overall goal of
     * this class is two-fold: 1) Provide a navigable manner to inspect
     * configuration 2) Provide a simple persistence mechanism to load/store
     * configuration
     */
    @SuppressWarnings("all")
    public static class ConfigNode extends TreeItem implements Serializable {

        public transient ConfigNode root;
        public transient ConfigNode parent;
        private transient ObservableList<ConfigNode> children;
        public transient Reconfigurable subject;
        private transient boolean changed = true;

        public Map<String, Serializable> settings = new TreeMap<>();
        public Map<String, String[]> hotkeys = new TreeMap<>();
        public String name;
        private String id;

        private void writeObject(java.io.ObjectOutputStream out)
                throws IOException {
            out.writeObject(id);
            out.writeObject(name);
            out.writeObject(settings);
            out.writeObject(hotkeys);
            out.writeObject(children.toArray());
        }

        private void readObject(java.io.ObjectInputStream in)
                throws IOException, ClassNotFoundException {
                // Create children list if it doesn't exist
            if (children == null) {
                children = FXCollections.observableArrayList();
                children.setAll(getChildren());
            }
            children.setAll(super.getChildren());
            id = (String) in.readObject();
            name = (String) in.readObject();
            settings = (Map<String, Serializable>) in.readObject();
            hotkeys = (Map<String, String[]>) in.readObject();
            Object[] nodeArray = (Object[]) in.readObject();
            synchronized (children) {
                for (Object child : nodeArray) {
                    children.add((ConfigNode) child);
                }
            }
        }

        private void readObjectNoData()
                throws ObjectStreamException {
            name = "Bad read";
        }

        @Override
        public String toString() {
            return name;
        }

        public ConfigNode(Reconfigurable subject) {
            this(null, subject);
            this.root = null;
            this.setExpanded(true);
        }

        public ConfigNode(ConfigNode parent, Reconfigurable subject) {
            this(parent, subject, subject.getName());
        }

        public ConfigNode(ConfigNode parent, Reconfigurable subject, String id) {
            super();
            this.id = id;
            this.name = subject.getName();
            this.subject = subject;
            this.children = getChildren();
            this.parent = parent;
            if (this.parent != null) {
                this.root = this.parent.root != null ? this.parent.root : this.parent;
            }
            setValue(toString());
        }

        public void setFieldValue(String field, Serializable value) {
            setChanged(true);
            if (value != null) {
                if (value.equals(getFieldValue(field))) {
                    return;
                }
            } else {
                if (getFieldValue(field) == null) {
                    return;
                }
            }
            setRawFieldValue(field, value);
        }

        public void setRawFieldValue(String field, Serializable value) {
            settings.put(field, value);
        }

        public Serializable getFieldValue(String field) {
            return settings.get(field);
        }

        public Set<String> getAllSettingNames() {
            return settings.keySet();
        }

        @Override
        final public ObservableList<ConfigNode> getChildren() {
            return super.getChildren();
        }

        private boolean removeChild(String childName) {
            ConfigNode child = findChild(childName);
            synchronized (children) {
                return children.remove(child);
            }
        }

        /**
         * Locate a child by its internal id (field-name) or its display name.
         * Older configuration files used the display name (e.g. "Jace User Interface")
         * as the id, while newer builds use the Java field name (e.g. "ui").
         * To remain backward-compatible we look for a match on either.
         */
        private ConfigNode findChild(String key) {
            if (key == null) return null;
            synchronized (children) {
                for (ConfigNode node : children) {
                    if (key.equalsIgnoreCase(node.id) || key.equalsIgnoreCase(node.name)) {
                        return node;
                    }
                }
            }
            return null;
        }

        private void putChild(String id, ConfigNode newChild) {
            removeChild(id);
            int index = 0;
            for (ConfigNode node : children) {
                int compare = node.toString().compareToIgnoreCase(id);
                if (compare >= 0) {
                    break;
                } else {
                    index++;
                }
            }
            synchronized (children) {
                children.add(index, newChild);
            }
        }

        private void setChanged(boolean b) {
            changed = b;
            if (!changed) {
                setGraphic(null);
            } else {
                getChangedIcon().ifPresent(this::setGraphic);
            }
        }

        public Stream<ConfigNode> getTreeAsStream() {
            synchronized (children) {
                return Stream.concat(
                        Stream.of(this),
                        children.stream().flatMap(ConfigNode::getTreeAsStream));
            }
        }
    }
    public static ConfigNode BASE;
    @ConfigurableField(name = "Autosave Changes", description = "If unchecked, changes are only saved when the Save button is pressed.")
    public static boolean saveAutomatically = false;

    // Track whether shutdown hook has been registered (GH-9 fix)
    private static final AtomicBoolean shutdownHookRegistered = new AtomicBoolean(false);

    // Track shutdown hook execution for testing (package-private for test access)
    static final AtomicBoolean shutdownHookExecuted = new AtomicBoolean(false);

    public static void initializeBaseConfiguration() {
        if (BASE == null) {
            BASE = new ConfigNode(new Configuration());
            registerShutdownHook();
        }
    }

    /**
     * Register a JVM shutdown hook to ensure configuration is saved even if
     * the application exits during the debounce window (GH-9 fix).
     *
     * This prevents settings loss when the application shuts down within
     * 2 seconds of the last setting change.
     */
    private static void registerShutdownHook() {
        if (shutdownHookRegistered.compareAndSet(false, true)) {
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                Logger logger = Logger.getLogger(Configuration.class.getName());
                logger.log(Level.INFO, "Shutdown hook triggered - flushing configuration to disk");

                try {
                    // Mark that shutdown hook executed (for testing validation)
                    shutdownHookExecuted.set(true);

                    // Save immediately on shutdown, bypassing any debounce
                    saveSettingsImmediate();
                    logger.log(Level.INFO, "Configuration saved successfully during shutdown");
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "Failed to save configuration during shutdown", e);
                }
            }, "Configuration-Shutdown-Hook"));

            Logger.getLogger(Configuration.class.getName()).log(Level.FINE,
                "Shutdown hook registered for configuration persistence (GH-9 fix)");
        }
    }
    
    public static void buildTree() {
        if (BASE == null) {
            BASE = new ConfigNode(new Configuration());
        }
        Set<ConfigNode> visited = new LinkedHashSet<>();
        buildTree(BASE, visited);
        Emulator.withComputer(c->{
            ConfigNode computer = new ConfigNode(BASE, c);
            BASE.putChild(c.getName(), computer);
            buildTree(computer, visited);
        });
    }

    @SuppressWarnings("all")
    private static void buildTree(ConfigNode node, Set visited) {
        if (node.subject == null) {
            return;
        }

        InvokableActionRegistry registry = InvokableActionRegistry.getInstance();
        registry.getStaticMethodNames(node.subject.getClass()).stream().forEach((name) -> 
            node.hotkeys.put(name, registry.getStaticMethodInfo(name).defaultKeyMapping())
        );
        registry.getInstanceMethodNames(node.subject.getClass()).stream().forEach((name) -> 
            node.hotkeys.put(name, registry.getInstanceMethodInfo(name).defaultKeyMapping())
        );

        for (Field f : node.subject.getClass().getFields()) {
//            System.out.println("Evaluating field " + f.getName());
            try {
                Object o = f.get(node.subject);
                if (o == null || !f.getType().isPrimitive() && f.getType() != String.class && visited.contains(o)) {
                    continue;
                }
                visited.add(o);
//                System.out.println(o.getClass().getName());
                // If the object in question is not reconfigurable,
                // skip over it and investigate its fields instead
//                if (o.getClass().isAssignableFrom(Reconfigurable.class)) {
//                if (Reconfigurable.class.isAssignableFrom(o.getClass())) {
                if (f.isAnnotationPresent(ConfigurableField.class)) {
                    if (ISelection.class.isAssignableFrom(o.getClass())) {
                        ISelection selection = (ISelection) o;
                        node.setRawFieldValue(f.getName(), (Serializable) selection.getSelections().get(selection.getValue()));
                    } else {
                        node.setRawFieldValue(f.getName(), (Serializable) o);
                    }
                    continue;
                }

                if (o instanceof Reconfigurable r) {
                    ConfigNode child = node.findChild(f.getName());
                    if (child == null || !child.subject.equals(o)) {
                        // Use display name for id to maintain compatibility with existing settings files
                        child = new ConfigNode(node, r);
                        node.putChild(f.getName(), child);
                    }
                    buildTree(child, visited);
                } else if (o.getClass().isArray()) {
                    String fieldName = f.getName();
                    Class type = o.getClass().getComponentType();
//                    System.out.println("Evaluating " + node.subject.getShortName() + "." + fieldName + "; type is " + type.toGenericString());
                    List<Reconfigurable> children = new ArrayList<>();
                    if (!Reconfigurable.class.isAssignableFrom(type)) {
//                        System.out.println("Looking at type " + type.getName() + " to see if optional");
                        if (Optional.class.isAssignableFrom(type)) {
                            Type genericTypes = f.getGenericType();
//                            System.out.println("Looking at generic parmeters " + genericTypes.getTypeName() + " for reconfigurable class, type " + genericTypes.getClass().getName());
                            if (genericTypes instanceof GenericArrayType aType) {
                                ParameterizedType pType = (ParameterizedType) aType.getGenericComponentType();
                                if (pType.getActualTypeArguments().length != 1) {
                                    continue;
                                }
                                Type genericType = pType.getActualTypeArguments()[0];
//                                System.out.println("Looking at type " + genericType.getTypeName() + " to see if reconfigurable");
                                if (!Reconfigurable.class.isAssignableFrom((Class) genericType)) {
                                    continue;
                                }
                            } else {
                                continue;
                            }

                            synchronized (children) {
                                for (Optional<Reconfigurable> child : (Optional<Reconfigurable>[]) o) {
                                    if (child.isPresent()) {
                                        children.add(child.get());
                                    } else {
                                        children.add(null);
                                    }
                                }
                            }
                        }
                    } else {
                        children = Arrays.asList((Reconfigurable[]) o);
                    }
                    for (int i = 0; i < children.size(); i++) {
                        Reconfigurable child = children.get(i);
                        String childId = fieldName + i;
                        if (child == null) {
                            node.removeChild(childId);
                            continue;
                        }
                        ConfigNode grandchild = node.findChild(childId);
                        if (grandchild == null || !grandchild.subject.equals(child)) {
                            grandchild = new ConfigNode(node, child, childId);
                            node.putChild(childId, grandchild);
                        }
                        buildTree(grandchild, visited);
                    }
                }
            } catch (IllegalArgumentException | IllegalAccessException ex) {
                Logger.getLogger(Configuration.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    @InvokableAction(
            name = "Save settings",
            description = "Save all configuration settings as defaults",
            category = "general",
            alternatives = "save preferences;save defaults",
            defaultKeyMapping = "meta+ctrl+s"
    )
    public static void saveSettings() {
        Logger.getLogger(Configuration.class.getName()).log(Level.INFO, "Saving configuration (full) to {0}", getSettingsFile().getAbsolutePath());
        {
            ObjectOutputStream oos = null;
            try {
                applySettings(BASE);
                oos = new ObjectOutputStream(new FileOutputStream(getSettingsFile()));
                oos.writeObject(BASE);
            } catch (FileNotFoundException ex) {
                Logger.getLogger(Configuration.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(Configuration.class.getName()).log(Level.SEVERE, null, ex);
            } finally {
                try {
                    if (oos != null) {
                        oos.close();
                    }
                } catch (IOException ex) {
                    Logger.getLogger(Configuration.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    /**
     * Save settings without applying them first - useful for UI settings that don't need computer suspension.
     *
     * This method ensures durability by:
     * 1. Writing to FileOutputStream directly for control over flush behavior
     * 2. Calling flush() to ensure data leaves application buffers
     * 3. Calling getFD().sync() to force OS to write to physical storage
     * 4. Verifying the file was written successfully
     */
    public static void saveSettingsImmediate() {
        Logger logger = Logger.getLogger(Configuration.class.getName());
        try {
            buildTree(); // make sure BASE reflects latest in-memory values

            // construct DTO from live objects
            AppSettingsDTO dto = new AppSettingsDTO();
            if (BASE.subject instanceof Configuration cfg && cfg.ui != null) {
                EmulatorUILogic ui = cfg.ui;
                dto.ui.windowWidth = ui.windowWidth;
                dto.ui.windowHeight = ui.windowHeight;
                dto.ui.windowSizeIndex = ui.windowSizeIndex;
                dto.ui.fullscreen = ui.fullscreen;
                dto.ui.videoMode = ui.videoMode;
                dto.ui.musicVolume = ui.musicVolume;
                dto.ui.sfxVolume = ui.sfxVolume;
                dto.ui.soundtrackSelection = ui.soundtrackSelection;
                dto.ui.aspectRatio = ui.aspectRatioCorrection;
            }

            ObjectMapper mapper = JsonMapper.builder()
                .enable(SerializationFeature.INDENT_OUTPUT)
                .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
                .build();

            File json = getJsonSettingsFile();
            logger.log(Level.FINE, "Saving configuration (JSON) to {0}", json.getAbsolutePath());

            // Write with explicit flush and sync for durability (GH-9 fix)
            try (FileOutputStream fos = new FileOutputStream(json)) {
                mapper.writeValue(fos, dto);
                fos.flush(); // Ensure data leaves application buffers

                // Attempt fsync - may fail on some filesystems (e.g., tmpfs on macOS)
                // but that's acceptable for test environments
                try {
                    fos.getFD().sync(); // Force OS to write to physical storage
                } catch (java.io.SyncFailedException e) {
                    // Sync not supported on this filesystem - acceptable in test/temp directories
                    logger.log(Level.FINE, "fsync not supported on this filesystem (acceptable for temp directories)");
                }
            }

            // Verify file was written successfully
            if (!json.exists() || json.length() == 0) {
                throw new IOException("Configuration file not written or is empty");
            }

            logger.log(Level.INFO, "Configuration saved successfully: {0} bytes at {1}",
                new Object[]{json.length(), System.currentTimeMillis()});

        } catch (IOException ex) {
            Logger.getLogger(Configuration.class.getName()).log(Level.SEVERE,
                "Failed to save configuration", ex);
        }
    }

    @InvokableAction(
            name = "Load settings",
            description = "Load all configuration settings previously saved",
            category = "general",
            alternatives = "load preferences;revert settings;revert preferences",
            defaultKeyMapping = "meta+ctrl+r"
    )
    public static void loadSettings() {
        File json = getJsonSettingsFile();
        if (json.exists()) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                AppSettingsDTO dto = mapper.readValue(json, AppSettingsDTO.class);
                Logger.getLogger(Configuration.class.getName()).log(Level.FINE, "Loading configuration from {0}", json.getAbsolutePath());
                applyDto(dto);
                return;
            } catch (IOException e) {
                Logger.getLogger(Configuration.class.getName()).log(Level.WARNING, "Failed to parse JSON settings, falling back to legacy format", e);
            }
        }

        // fallback to legacy binary format
        Logger.getLogger(Configuration.class.getName()).log(Level.FINE, "Loading configuration (legacy) from {0}", getSettingsFile().getAbsolutePath());
        boolean successful = false;
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(getSettingsFile()))) {
            ConfigNode newRoot = (ConfigNode) ois.readObject();
            applyConfigTree(newRoot, BASE);
            successful = true;
        } catch (FileNotFoundException ignored) {
        } catch (InvalidClassException | NullPointerException ex) {
            Logger.getLogger(Configuration.class.getName()).log(Level.WARNING, "Legacy settings incompatible with current version.");
        } catch (ClassNotFoundException | IOException ex) {
            Logger.getLogger(Configuration.class.getName()).log(Level.SEVERE, null, ex);
        }

        if (successful) {
            // immediately migrate to JSON for future runs
            saveSettingsImmediate();
            // optional: backup old file once
            try {
                Files.move(getSettingsFile().toPath(), getSettingsFile().toPath().resolveSibling(".jace.conf.bak"), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException ignore) {}
        }
    }

    public static void resetToDefaults() {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public static File getSettingsFile() {
        return new File(System.getProperty("user.home"), ".jace.conf");
    }

    public static void registerKeyHandlers() {
        registerKeyHandlers(BASE, true);
    }

    public static void registerKeyHandlers(ConfigNode node, boolean recursive) {
        Keyboard.unregisterAllHandlers(node.subject);
        InvokableActionRegistry registry = InvokableActionRegistry.getInstance();
        node.hotkeys.keySet().stream().forEach((name) -> {
            InvokableAction action = registry.getStaticMethodInfo(name);
            if (action != null) {
                for (String code : node.hotkeys.get(name)) {
                    Keyboard.registerInvokableAction(action, node.subject, registry.getStaticFunction(name), code);
                }
            }
            action = registry.getInstanceMethodInfo(name);
            if (action != null) {
                for (String code : node.hotkeys.get(name)) {
                    Keyboard.registerInvokableAction(action, node.subject, registry.getInstanceFunction(name), code);
                }
            }
        });
        if (recursive) {
            node.getChildren().stream().forEach((child) -> {
                registerKeyHandlers(child, true);
            });
        }
    }

    /**
     * Apply settings from node tree to the object model This also calls
     * "reconfigure" on objects in sequence
     *
     * @param node
     * @return True if any settings have changed in the node or any of its
     * descendants
     */
    public static boolean applySettings(ConfigNode node) {
        AtomicBoolean hasChanged = new AtomicBoolean(false);

        Emulator.whileSuspended(c-> {
            if (node.changed) {
                doApply(node);
                hasChanged.set(true);
            }

            // Now that the object structure reflects the current configuration,
            // process reconfiguration from the children, etc.
            for (ConfigNode child : node.getChildren()) {
                if (applySettings(child)) hasChanged.set(true);
            }
        });

        if (node.equals(BASE) && hasChanged.get()) {
            buildTree();
        }

        return hasChanged.get();
    }

    @SuppressWarnings("all")
    private static void applyConfigTree(ConfigNode newRoot, ConfigNode oldRoot) {
        if (oldRoot == null || newRoot == null) {
            return;
        }
        oldRoot.settings = newRoot.settings;
        oldRoot.hotkeys = newRoot.hotkeys;
        if (oldRoot.subject != null) {
            doApply(oldRoot);
            buildTree(oldRoot, new HashSet());
        }
        newRoot.getChildren().stream().forEach((child) -> {
            // First try by id (field name), then by human-readable name
            ConfigNode oldChild = oldRoot.findChild(child.id);
            if (oldChild == null) {
                oldChild = oldRoot.findChild(child.toString());
            }
            applyConfigTree(child, oldChild);
        });
    }

    @SuppressWarnings("all")
    private static void doApply(ConfigNode node) {
        List<String> removeList = new ArrayList<>();
        registerKeyHandlers(node, false);

        for (String f : node.settings.keySet()) {
            try {
                Field ff = node.subject.getClass().getField(f);
//                System.out.println("Setting " + f + " to " + node.settings.get(f));
                Object val = node.settings.get(f);
                Class valType = (val != null ? val.getClass() : null);
                Class fieldType = ff.getType();
                if (ISelection.class.isAssignableFrom(fieldType)) {
                    ISelection selection = (ISelection) ff.get(node.subject);
                    try {
                        selection.setValue(val);
                    } catch (ClassCastException c) {
                        selection.setValueByMatch(String.valueOf(val));
                    }
                    continue;
                }
                if (val == null || valType.equals(fieldType)) {
                    ff.set(node.subject, val);
                    continue;
                }
//                System.out.println(fieldType);
                val = Utility.deserializeString(String.valueOf(val), fieldType, false);
//                System.out.println("Setting "+node.subject.getName()+" property "+ff.getName()+" with value "+String.valueOf(val));
                ff.set(node.subject, val);
            } catch (NoSuchFieldException ex) {
                System.out.println("Setting " + f + " no longer exists, skipping.");
                removeList.add(f);
            } catch (SecurityException | IllegalArgumentException | IllegalAccessException ex) {
                Logger.getLogger(Configuration.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        removeList.stream().forEach((f) -> {
            node.settings.remove(f);
        });

        try {
            // When settings are applied, this could very well change the object structure
            // For example, if cards or other pieces of emulation are changed around
//            System.out.println("Reconfiguring "+node.subject.getName());
            node.subject.reconfigure();
        } catch (Exception ex) {
            Logger.getLogger(Configuration.class.getName()).log(Level.SEVERE, null, ex);
        }

        node.setChanged(false);
    }

    public static void applySettings(Map<String, String> settings) {
        for (Map.Entry<String, String> setting : settings.entrySet()) {
            Map<String, ConfigNode> shortNames = new HashMap<>();
            buildNodeMap(BASE, shortNames);

            String settingName = setting.getKey();
            String value = setting.getValue();
            String[] parts = settingName.split("\\.");
            if (parts.length != 2) {
                System.err.println("Unable to parse settting, should be in the form of DEVICE.PROPERTYNAME " + settingName);
                continue;
            }
            String deviceName = parts[0];
            String fieldName = parts[1];
            ConfigNode n = shortNames.get(deviceName.toLowerCase());
            if (n == null) {
                System.err.println("Unable to find device named " + deviceName + ", try one of these: " + String.join(", ", shortNames.keySet()));
                continue;
            }

            boolean found = false;
            List<String> shortFieldNames = new ArrayList<>();
            for (String longName : n.getAllSettingNames()) {
                ConfigurableField f = getConfigurableFieldInfo(n.subject, longName);
                String shortName = getShortName(f, longName);
                shortFieldNames.add(shortName);

                if (fieldName.equalsIgnoreCase(longName) || fieldName.equalsIgnoreCase(shortName)) {
                    found = true;
                    n.setFieldValue(longName, value);
                    applySettings(n);
//                    n.subject.reconfigure();
                    buildTree();
                    System.out.println("Set property " + n.subject.getName() + "." + longName + " to " + value);
                    break;
                }
            }
            if (!found) {
                System.err.println("Unable to find property " + fieldName + " for device " + deviceName + ".  Try one of these: " + String.join(", ", shortFieldNames));
            }
        }
    }

    private static void buildNodeMap(ConfigNode n, Map<String, ConfigNode> shortNames) {
//        System.out.println("Encountered " + n.subject.getShortName().toLowerCase());
        shortNames.put(n.subject.getShortName().toLowerCase(), n);
        synchronized (n.getChildren()) {
            n.getChildren().stream().forEach((c) -> {
                buildNodeMap(c, shortNames);
            });
        }
    }

    private static File getJsonSettingsFile() {
        return new File(System.getProperty("user.home"), ".jace.json");
    }

    private static void applyDto(AppSettingsDTO dto) {
        if (dto == null) return;
        EmulatorUILogic ui = ((Configuration) BASE.subject).ui;
        AppSettingsDTO.UiSettings s = dto.ui;
        if (s != null) {
            ui.windowWidth = s.windowWidth;
            ui.windowHeight = s.windowHeight;
            ui.windowSizeIndex = s.windowSizeIndex;
            ui.fullscreen = s.fullscreen;
            ui.videoMode = s.videoMode;
            ui.musicVolume = s.musicVolume;
            ui.sfxVolume = s.sfxVolume;
            ui.soundtrackSelection = s.soundtrackSelection;
            ui.aspectRatioCorrection = s.aspectRatio;
        }
        // ensure tree reflects settings
        buildTree();
    }
}
