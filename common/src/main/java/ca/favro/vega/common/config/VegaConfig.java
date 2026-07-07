package ca.favro.vega.common.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

public class VegaConfig {
    private String wssURL = "wss://wss.ve3jfo.ca";
    private boolean renderEnabled = true;
    private boolean sendInfo = true;
    private boolean receiveInfo = true;
    private boolean showOnlyFocused = false;
    private boolean showBeams = true;
    private boolean showNamePlates = true;
    private boolean showLocatorBar = true;

    private float waypointKeepAge = 1;

    private final File configFile;

    public VegaConfig(File configFile) {
        this.configFile = configFile;
    }

    public void save() {
        Config config = new Config();

        config.wssURL = this.wssURL;
        config.renderEnabled = this.renderEnabled;
        config.sendInfo = this.sendInfo;
        config.receiveInfo = this.receiveInfo;
        config.showOnlyFocused = this.showOnlyFocused;
        config.showBeams = this.showBeams;
        config.showNamePlates = this.showLocatorBar;
        config.showLocatorBar = this.showLocatorBar;
        config.waypointKeepAge = this.waypointKeepAge;

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json = gson.toJson(config);

        try (FileWriter writer = new FileWriter(configFile)) {
            writer.write(json);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean load() {
        Config config = null;

        try (FileReader reader = new FileReader(this.configFile)) {
            config = new Gson().fromJson(reader, Config.class);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (config == null) return false;

        this.setWssURL(config.wssURL);
        this.setRenderEnabled(config.renderEnabled);
        this.setSendInfo(config.sendInfo);
        this.setReceiveInfo(config.receiveInfo);
        this.setShowOnlyFocused(config.showOnlyFocused);
        this.setShowBeams(config.showBeams);
        this.setShowNamePlates(config.showNamePlates);
        this.setShowLocatorBar(config.showLocatorBar);
        this.setWaypointKeepAge(Math.clamp(config.waypointKeepAge, 0, 8));

        return true;
    }

    public String getWssURL() {
        return wssURL;
    }

    public boolean setWssURL(String wssURL) {
        if (wssURL.equals(this.wssURL)) return false;

        this.wssURL = wssURL;
        return true;
    }

    public boolean isRenderEnabled() {
        return renderEnabled;
    }

    public boolean setRenderEnabled(boolean renderEnabled) {
        if (this.renderEnabled == renderEnabled) return false;

        this.renderEnabled = renderEnabled;
        return true;
    }

    public boolean isSendInfo() {
        return sendInfo;
    }

    public boolean setSendInfo(boolean sendInfo) {
        if (this.sendInfo == sendInfo) return false;

        this.sendInfo = sendInfo;
        return true;
    }

    public boolean isReceiveInfo() {
        return receiveInfo;
    }

    public boolean setReceiveInfo(boolean receiveInfo) {
        if (this.receiveInfo == receiveInfo) return false;

        this.receiveInfo = receiveInfo;
        return true;
    }

    public boolean isShowOnlyFocused() {
        return showOnlyFocused;
    }

    public boolean setShowOnlyFocused(boolean showOnlyFocused) {
        if (this.showOnlyFocused == showOnlyFocused) return false;

        this.showOnlyFocused = showOnlyFocused;
        return true;
    }

    public boolean isShowBeams() {
        return showBeams;
    }

    public boolean setShowBeams(boolean showBeams) {
        if (this.showBeams == showBeams) return false;

        this.showBeams = showBeams;
        return true;
    }

    public boolean isShowLocatorBar() {
        return showLocatorBar;
    }

    public boolean setShowLocatorBar(boolean showLocatorBar) {
        if (this.showLocatorBar == showLocatorBar) return false;

        this.showLocatorBar = showLocatorBar;
        return true;
    }

    public boolean isShowNamePlates() {
        return showNamePlates;
    }

    public boolean setShowNamePlates(boolean showNamePlates) {
        if (this.showNamePlates == showNamePlates) return false;

        this.showNamePlates = showNamePlates;
        return true;
    }

    public float getWaypointKeepAge() {
        return waypointKeepAge;
    }

    public boolean setWaypointKeepAge(float waypointKeepAge) {
        if (this.waypointKeepAge == Math.clamp(waypointKeepAge, 0F, 8F)) return false;

        this.waypointKeepAge = waypointKeepAge;
        return true;
    }

    public static class Config {
        public String wssURL;
        public String jarSource;
        public boolean renderEnabled;
        public boolean sendInfo;
        public boolean receiveInfo;
        public boolean showOnlyFocused;
        public boolean showBeams;
        public boolean showLocatorBar;
        public boolean showNamePlates;
        public float waypointKeepAge;
    }
}
