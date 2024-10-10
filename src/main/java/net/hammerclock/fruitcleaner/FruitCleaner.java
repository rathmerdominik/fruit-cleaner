package net.hammerclock.fruitcleaner;

import net.hammerclock.fruitcleaner.config.CommonConfig;
import net.hammerclock.fruitcleaner.events.FruitCleanerEvent;
import net.minecraftforge.common.MinecraftForge;

import net.minecraftforge.fml.ExtensionPoint;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.VersionChecker;
import net.minecraftforge.fml.VersionChecker.Status;
import net.minecraftforge.fml.VersionChecker.CheckResult;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.server.FMLServerStartedEvent;
import net.minecraftforge.fml.loading.FMLEnvironment;

import net.minecraftforge.fml.network.FMLNetworkConstants;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(FruitCleaner.PROJECT_ID)
public class FruitCleaner
{
    public static final Logger LOGGER = LogManager.getLogger(FruitCleaner.PROJECT_ID);

    public static final String CONFIG_NAME = "mmnm-fruit-cleaner.toml";

    public static final String PROJECT_ID = "fruitcleaner";


    public FruitCleaner() {
        ModLoadingContext.get().registerExtensionPoint(ExtensionPoint.DISPLAYTEST,
                () -> Pair.of(() -> FMLNetworkConstants.IGNORESERVERONLY, (a, b) -> true));

        ModLoadingContext context = ModLoadingContext.get();

        context.registerConfig(ModConfig.Type.COMMON, CommonConfig.CONFIG, CONFIG_NAME);

        if(FMLEnvironment.dist.isDedicatedServer()){
            this.initServer();
        }
    }

    private static void onServerStarted(FMLServerStartedEvent event) {
        CheckResult result = VersionChecker.getResult(ModList.get().getModContainerById(PROJECT_ID).orElseThrow(IllegalArgumentException::new).getModInfo());
        if(result.status == Status.OUTDATED) {
            LOGGER.warn("YOUR MOD IS OUTDATED. The latest version is {}. Please get the latest version here: {}", result.target, result.url);
        }
        LOGGER.info("Fruit Cleaner Started!");
    }

    private void initServer() {
        MinecraftForge.EVENT_BUS.register(FruitCleanerEvent.class);
        MinecraftForge.EVENT_BUS.addListener(FruitCleaner::onServerStarted);
    }
}
