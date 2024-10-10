package net.hammerclock.fruitcleaner.events;

import net.hammerclock.fruitcleaner.FruitCleaner;
import net.hammerclock.fruitcleaner.config.CommonConfig;
import net.hammerclock.fruitcleaner.world.FruitTimerWorldData;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.world.ChunkDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import xyz.pixelatedw.mineminenomi.api.events.onefruit.DroppedDevilFruitEvent;
import xyz.pixelatedw.mineminenomi.api.events.onefruit.InventoryDevilFruitEvent;
import xyz.pixelatedw.mineminenomi.api.events.onefruit.LostDevilFruitEvent;
import xyz.pixelatedw.mineminenomi.api.helpers.DevilFruitHelper;
import xyz.pixelatedw.mineminenomi.data.world.OFPWWorldData;
import xyz.pixelatedw.mineminenomi.entities.DFItemEntity;
import xyz.pixelatedw.mineminenomi.items.AkumaNoMiItem;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

// TODO too much duplication in the code, refactor it
public class FruitCleanerEvent {
    @SubscribeEvent
    public static void chunkSave(ChunkDataEvent.Save event) {
        if (CommonConfig.INSTANCE.getFruitDeleteMode() != CommonConfig.FruitDeleteMode.ON_CHUNK_UNLOAD) return;
        if (!(event.getWorld() instanceof ServerWorld)) return;
        if (
                !(xyz.pixelatedw.mineminenomi.config.CommonConfig.INSTANCE.hasOneFruitPerWorldSimpleLogic() ||
                        xyz.pixelatedw.mineminenomi.config.CommonConfig.INSTANCE.hasOneFruitPerWorldExtendedLogic())
        ) return;
        if (event.getWorld().isAreaLoaded(event.getChunk().getPos().getWorldPosition(), 0)) return;

        CompoundNBT level = (CompoundNBT) event.getData().get("Level");
        if (level == null) return;
        ListNBT entityData = (ListNBT) level.get("Entities");
        if (entityData == null) return;

        for (int index = 0; index < entityData.size(); index++) {
            CompoundNBT entityNBT = (CompoundNBT) entityData.get(index);
            if (entityNBT.contains("id")) {
                String id = entityNBT.getString("id");
                if (id.equals("mineminenomi:devil_fruit")) {
                    CompoundNBT itemData = entityNBT.getCompound("Item");
                    Item devilFruitItem = DevilFruitHelper.getDevilFruitItem(new ResourceLocation(itemData.getString("id")));
                    if (devilFruitItem == null) {
                        FruitCleaner.LOGGER.warn("Devil Fruit item not found for {}", itemData.getString("id"));
                        continue;
                    }
                    if (devilFruitItem.getItem().getItem() instanceof AkumaNoMiItem) {
                        OFPWWorldData worldData = OFPWWorldData.get();
                        if (worldData == null) {
                            FruitCleaner.LOGGER.error("OFPWWorldData is null!");
                            return;
                        }
                        worldData.lostOneFruit(devilFruitItem.getRegistryName(), null, "Chunk Unloaded");
                        entityData.remove(index);
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinWorldEvent event) {
        if (CommonConfig.INSTANCE.getFruitDeleteMode() != CommonConfig.FruitDeleteMode.TIMER_BASED) return;
        if (
                !(xyz.pixelatedw.mineminenomi.config.CommonConfig.INSTANCE.hasOneFruitPerWorldSimpleLogic() ||
                        xyz.pixelatedw.mineminenomi.config.CommonConfig.INSTANCE.hasOneFruitPerWorldExtendedLogic())
        ) return;

        FruitTimerWorldData worldData = FruitTimerWorldData.get().orElseThrow(() -> new RuntimeException("FruitTimerWorldData is null!"));

        if (event.getEntity() instanceof DFItemEntity) {
            DFItemEntity dfItemEntity = (DFItemEntity) event.getEntity();
            AkumaNoMiItem item = (AkumaNoMiItem) dfItemEntity.getItem().getItem();

            if (worldData.getFruitsToPurge().contains(item.getFruitKey())) {
                dfItemEntity.kill();
                worldData.removeFruitToPurge(item.getFruitKey());

                OFPWWorldData ofpwData = OFPWWorldData.get();
                if (ofpwData == null) {
                    FruitCleaner.LOGGER.error("OFPWWorldData is null!");
                    return;
                }
                ofpwData.lostOneFruit(item.getRegistryName(), null, "Timer ran out");
            }
        }
    }

    @SubscribeEvent
    public static void onFruitDropped(DroppedDevilFruitEvent event) {
        if (CommonConfig.INSTANCE.getFruitDeleteMode() != CommonConfig.FruitDeleteMode.TIMER_BASED) return;
        if (
                !(xyz.pixelatedw.mineminenomi.config.CommonConfig.INSTANCE.hasOneFruitPerWorldSimpleLogic() ||
                        xyz.pixelatedw.mineminenomi.config.CommonConfig.INSTANCE.hasOneFruitPerWorldExtendedLogic())
        ) return;

        AkumaNoMiItem devilFruit = (AkumaNoMiItem) event.getItem();

        FruitTimerWorldData worldData = FruitTimerWorldData.get().orElseThrow(() -> new RuntimeException("FruitTimerWorldData is null!"));
        if (worldData.getFruitsToPurge().contains(devilFruit.getFruitKey())) return;
        worldData.addFruitTimer(devilFruit.getFruitKey(), event.getEntity().level.getGameTime() + (CommonConfig.INSTANCE.getFruitDeleteTimer() * 20L)); // 20 ticks equal one second
    }

    @SubscribeEvent
    public static void onFruitInventory(InventoryDevilFruitEvent event) {
        if (CommonConfig.INSTANCE.getFruitDeleteMode() != CommonConfig.FruitDeleteMode.TIMER_BASED) return;
        if (
                !(xyz.pixelatedw.mineminenomi.config.CommonConfig.INSTANCE.hasOneFruitPerWorldSimpleLogic() ||
                        xyz.pixelatedw.mineminenomi.config.CommonConfig.INSTANCE.hasOneFruitPerWorldExtendedLogic())
        ) return;

        FruitTimerWorldData worldData = FruitTimerWorldData.get().orElseThrow(() -> new RuntimeException("FruitTimerWorldData is null!"));
        worldData
                .getFruitTimer(((AkumaNoMiItem) event.getItem()).getFruitKey())
                .ifPresent(x -> worldData.removeFruitTimer(((AkumaNoMiItem) event.getItem()).getFruitKey()));
    }

    @SubscribeEvent
    public static void onFruitLost(LostDevilFruitEvent event) {
        if (CommonConfig.INSTANCE.getFruitDeleteMode() != CommonConfig.FruitDeleteMode.TIMER_BASED) return;
        if (
                !(xyz.pixelatedw.mineminenomi.config.CommonConfig.INSTANCE.hasOneFruitPerWorldSimpleLogic() ||
                        xyz.pixelatedw.mineminenomi.config.CommonConfig.INSTANCE.hasOneFruitPerWorldExtendedLogic())
        ) return;

        FruitTimerWorldData worldData = FruitTimerWorldData.get().orElseThrow(() -> new RuntimeException("FruitTimerWorldData is null!"));
        worldData
                .getFruitTimer(((AkumaNoMiItem) event.getItem()).getFruitKey())
                .ifPresent(x -> worldData.removeFruitTimer(((AkumaNoMiItem) event.getItem()).getFruitKey()));
    }


    @SubscribeEvent
    public static void onTick(TickEvent.WorldTickEvent event) {
        if (CommonConfig.INSTANCE.getFruitDeleteMode() != CommonConfig.FruitDeleteMode.TIMER_BASED) return;
        if (
                !(xyz.pixelatedw.mineminenomi.config.CommonConfig.INSTANCE.hasOneFruitPerWorldSimpleLogic() ||
                        xyz.pixelatedw.mineminenomi.config.CommonConfig.INSTANCE.hasOneFruitPerWorldExtendedLogic())
        ) return;
        if (!(event.world instanceof ServerWorld)) return;

        if (event.phase == TickEvent.Phase.END) {
            FruitTimerWorldData worldData = FruitTimerWorldData.get().orElseThrow(() -> new RuntimeException("FruitTimerWorldData is null!"));
            if (worldData.getFruitTimers().isEmpty()) return;

            for (Map.Entry<String, Long> entry : worldData.getFruitTimers().entrySet()) {
                String fruitName = entry.getKey();
                long releaseGameTick = entry.getValue();

                if (event.world.getGameTime() >= releaseGameTick) {
                    Stream<Entity> entities = ((ServerWorld) event.world).getEntities();

                    AtomicBoolean found = new AtomicBoolean(false);
                    entities.forEach(entity -> {
                        if (entity instanceof DFItemEntity) {
                            DFItemEntity dfItemEntity = (DFItemEntity) entity;
                            if (dfItemEntity.getItem().getItem() instanceof AkumaNoMiItem) {
                                if (((AkumaNoMiItem) dfItemEntity.getItem().getItem()).getFruitKey().equals(fruitName)) {
                                    dfItemEntity.kill();
                                    OFPWWorldData ofpwData = OFPWWorldData.get();
                                    if (ofpwData == null) {
                                        FruitCleaner.LOGGER.error("OFPWWorldData is null!");
                                        return;
                                    }
                                    ofpwData.lostOneFruit(dfItemEntity.getItem().getItem().getRegistryName(), null, "Timer ran out");
                                    found.set(true);
                                }
                            }
                        }
                    });

                    if (!found.get()) {
                        worldData.addFruitToPurge(fruitName);
                    }

                    worldData.removeFruitTimer(fruitName);
                    return;
                }
            }
        }
    }
}
