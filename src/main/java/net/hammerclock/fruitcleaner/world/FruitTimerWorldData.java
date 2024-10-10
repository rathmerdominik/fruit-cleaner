package net.hammerclock.fruitcleaner.world;

import net.hammerclock.fruitcleaner.FruitCleaner;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.fml.server.ServerLifecycleHooks;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class FruitTimerWorldData extends WorldSavedData {
    public static final Logger LOGGER = LogManager.getLogger(FruitCleaner.PROJECT_ID);
    private static final String IDENTIFIER = FruitCleaner.PROJECT_ID + "-timers";
    private static final int COMPOUND_TAG_TYPE = 10;

    private Map<String, Long> fruitTimerMap = new HashMap<>();
    private ArrayList<String> fruitsToPurge = new ArrayList<>();

    public FruitTimerWorldData() {
        super(IDENTIFIER);
    }

    public static Optional<FruitTimerWorldData> get() {
        if (ServerLifecycleHooks.getCurrentServer() != null) {
            return Optional.of(ServerLifecycleHooks.getCurrentServer().overworld().getDataStorage().computeIfAbsent(FruitTimerWorldData::new, IDENTIFIER));
        }
        return Optional.empty();
    }

    @Override
    public void load(CompoundNBT nbt) {
        CompoundNBT fruitTimerMapNBT = nbt.getCompound("fruitTimerMapNBT");

        this.fruitTimerMap.clear();
        fruitTimerMapNBT.getAllKeys().stream().forEach(x ->
                this.fruitTimerMap.put(x, fruitTimerMapNBT.getLong(x))
        );

        this.fruitsToPurge.clear();
        ListNBT fruitsToPurgeNBT = nbt.getList("fruitsToPurge", COMPOUND_TAG_TYPE);
        for (int i = 0; i < fruitsToPurgeNBT.size(); i++) {
            this.fruitsToPurge.add(fruitsToPurgeNBT.getCompound(i).getString("fruitName"));
        }
    }

    @Override
    public CompoundNBT save(CompoundNBT nbt) {
        CompoundNBT fruitTimerMapNBT = new CompoundNBT();
        if (!this.fruitTimerMap.isEmpty()) {
            this.fruitTimerMap.entrySet().stream().forEach(x -> fruitTimerMapNBT.putLong(x.getKey(), x.getValue()));
        }
        nbt.put("fruitTimerMapNBT", fruitTimerMapNBT);

        ListNBT fruitsToPurgeNBT = new ListNBT();
        for (int i = 0; i < this.fruitsToPurge.size(); i++) {
            CompoundNBT fruitNBT = new CompoundNBT();
            fruitNBT.putString("fruitName", this.fruitsToPurge.get(i));
            fruitsToPurgeNBT.add(i, fruitNBT);
        }
        nbt.put("fruitsToPurge", fruitsToPurgeNBT);

        return nbt;
    }

    public void addFruitTimer(String fruitName, long gameTimeRelease) {
        LOGGER.debug("Adding fruit {} with time {}", fruitName, gameTimeRelease);
        this.fruitTimerMap.put(fruitName, gameTimeRelease);
        this.setDirty();
        LOGGER.debug("Now have {} in fruitTimerMap", this.fruitTimerMap);
    }

    public void removeFruitTimer(String fruitName) {
        if (this.fruitTimerMap.containsKey(fruitName)) {
            LOGGER.debug("Removing fruit: {} from fruitTimerMap", fruitName);
            this.fruitTimerMap.remove(fruitName);
            LOGGER.debug("Now have {} in fruitTimerMap", this.fruitTimerMap);
            this.setDirty();
        }
    }

    public Optional<Long> getFruitTimer(String fruitName) {
        LOGGER.debug("Retrieving gameticks for fruit  {}", fruitName);
        LOGGER.debug("Currently have {} in fruitTimerMap", this.fruitTimerMap);

        if (!this.fruitTimerMap.containsKey(fruitName)) {
            LOGGER.debug("No fruit timer found");
            return Optional.empty();
        }

        return Optional.of(this.fruitTimerMap.get(fruitName));
    }

    public Map<String, Long> getFruitTimers() {
        return this.fruitTimerMap;
    }

    public void addFruitToPurge(String fruitName) {
        LOGGER.debug("Adding fruit {} to fruitsToPurge", fruitName);
        this.fruitsToPurge.add(fruitName);
        LOGGER.debug("Now have {} in fruitsToPurge", this.fruitTimerMap);
        this.setDirty();
    }

    public void removeFruitToPurge(String fruitName) {
        if (this.fruitsToPurge.contains(fruitName)) {
            LOGGER.debug("Removing fruit: {} from fruitsToPurge", fruitName);
            this.fruitsToPurge.remove(fruitName);
            LOGGER.debug("Now have {} in fruitsToPurge", this.fruitTimerMap);
            this.setDirty();
        }
    }

    public ArrayList<String> getFruitsToPurge() {
        return this.fruitsToPurge;
    }
}
