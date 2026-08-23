package ca.favro.vega.client;

import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;
import net.fabricmc.loader.impl.FabricLoaderImpl;
import net.fabricmc.loader.impl.launch.FabricLauncherBase;
import org.slf4j.Logger;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Paths;

public class VegaFabricPL implements PreLaunchEntrypoint {
    private static VegaFabricPL INSTANCE;
    public static final Logger LOGGER = new VegaLogger(VegaFabricPL.class);

    public VegaFabricPL() {
        if (INSTANCE != null) {
            throw new IllegalStateException("Constructor called twice");
        } else {
            INSTANCE = this;
        }
    }

    public static VegaFabricPL getInstance() {
        return INSTANCE;
    }

    @Override
    public void onPreLaunch() {
        // Teehee :)
        /* The goal is to do everything here (before loading the Vega impl class)
         * before mixins are applied. Then, instead of having mixins in the api jar, have them in the impl jar.
         * Then, hook into mixin loading somehow and force it to load the mixins that are in the impl jar, which won't be present in
         * fabric.mod.json. This will require that the api presents a method which will just blindly load all the mixins that the
         * impl jar provides. This method will be called at mixin time somehow.
         */
        try {
            File gameDirectory = FabricLoaderImpl.INSTANCE.getGameDir().toFile();
            File jarDir = new File(gameDirectory, "/vega/");
            if (!jarDir.isDirectory())
                jarDir.mkdir();

            BufferedInputStream bufferedInputStream = null;
            try {
                File jarFile = new File(jarDir.getPath() + "/vega-client.jar");
                if (jarFile.exists()) {
                    jarFile.delete();
                }
                String s = System.getProperty("vegaDebug");
                if (s != null) {
                    LOGGER.info("Loading Vega jar locally due to vegaDebug arg set");
                    jarFile = new File("/home/jeremy/Documents/Vega api-impl split/Vega/common/build/libs/common.jar");
                } else {
                    URL url = URI.create("https://wss.ve3jfo.ca/jars/common.jar").toURL();
                    LOGGER.info("Downloading Vega client JAR");
                    URLConnection urlConnection = url.openConnection();
                    bufferedInputStream = new BufferedInputStream(urlConnection.getInputStream());

                    FileOutputStream fileOutputStream = new FileOutputStream(jarFile);
                    bufferedInputStream.transferTo(fileOutputStream);
                    fileOutputStream.flush();
                    fileOutputStream.close();
                }
                FabricLauncherBase.getLauncher().addToClassPath(Paths.get(jarFile.getAbsolutePath()));
                LOGGER.info("Vega client JAR on classpath");

                IVegaBootstrap vegaBootstrap = (IVegaBootstrap) this.getClass().getClassLoader().loadClass("ca.favro.vega.common.VegaBootstrap")
                        .getDeclaredConstructor()
                        .newInstance();
                vegaBootstrap.loadMixins();
                LOGGER.info("Vega client mixin config loaded");
            } finally {
                if (bufferedInputStream != null) {
                    bufferedInputStream.close();
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
