package net.hammerclock.fruitcleaner.config;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.io.WritingMode;
import net.hammerclock.fruitcleaner.FruitCleaner;
import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

import java.nio.file.Path;
import java.nio.file.Paths;

public class CommonConfig {
    public static final Path CONFIG_PATH = Paths.get("config", FruitCleaner.CONFIG_NAME);
    public static final CommonConfig INSTANCE;
    public static final ForgeConfigSpec CONFIG;

    static {
        Pair<CommonConfig, ForgeConfigSpec> pair = new ForgeConfigSpec.Builder().configure(CommonConfig::new);

        CONFIG = pair.getRight();
        INSTANCE = pair.getLeft();

        CommentedFileConfig file = CommentedFileConfig
                .builder(CONFIG_PATH)
                .sync()
                .autoreload()
                .writingMode(WritingMode.REPLACE)
                .build();

        file.load();
        file.save();

        CONFIG.setConfig(file);
    }

    private final ForgeConfigSpec.EnumValue<FruitDeleteMode> fruitDeleteMode;
    private final ForgeConfigSpec.IntValue fruitDeleteTimer;

    CommonConfig(ForgeConfigSpec.Builder builder) {
        this.fruitDeleteMode = builder
                .comment("ON_CHUNK_UNLOAD: The fruit will be deleted when the chunk is unloaded and marked as LOST")
                .comment("TIMER_BASED: The fruit will be deleted after a certain amount of time")
                .defineEnum("fruitDeleteMode", FruitDeleteMode.ON_CHUNK_UNLOAD, FruitDeleteMode.TIMER_BASED, FruitDeleteMode.ON_CHUNK_UNLOAD);

        this.fruitDeleteTimer = builder
                .comment("The amount of time in seconds before the fruit is deleted. Default value is 300 seconds (5 minutes) which is the same as the default item despawn time.")
                .defineInRange("fruitDeleteTimer", 300, 0, Integer.MAX_VALUE);
    }

    public FruitDeleteMode getFruitDeleteMode() {
        return this.fruitDeleteMode.get();
    }


    public int getFruitDeleteTimer() {
        return this.fruitDeleteTimer.get();
    }

    public enum FruitDeleteMode {
        ON_CHUNK_UNLOAD,
        TIMER_BASED
    }
}