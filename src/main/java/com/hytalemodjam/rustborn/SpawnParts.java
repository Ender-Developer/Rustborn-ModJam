package com.hytalemodjam.rustborn;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.universe.world.worldgen.GeneratedBlockChunk;
import com.hypixel.hytale.server.core.universe.world.worldgen.GeneratedChunk;
import com.hypixel.hytale.server.core.universe.world.worldgen.IWorldGen;
import com.hypixel.hytale.server.core.universe.world.worldgen.WorldGenLoadException;
import com.hypixel.hytale.server.core.universe.world.worldgen.WorldGenTimingsCollector;
import com.hypixel.hytale.server.core.universe.world.worldgen.provider.IWorldGenProvider;
import com.hypixel.hytale.server.worldgen.HytaleWorldGenProvider;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.function.LongPredicate;
import javax.annotation.Nullable;

public class SpawnParts implements IWorldGenProvider, IWorldGen{

    public static final BuilderCodec<SpawnParts> CODEC = BuilderCodec.builder(SpawnParts.class, SpawnParts::new)
            .build();

    private IWorldGen vanillaGenerator;

    private Random random;

    private boolean debugEnabled = false;

    private int decoBoneSkullsId = -1;

    private int decoBonePileId = -1;

    private int bodyCommonId = -1;

    private int bodyUncommonId = -1;

    private int headCommonId = -1;

    private int headUncommonId = -1;

    private int legCommonId = -1;

    private int legUncommonId = -1;

    private int legRareId = -1;

    private int armCommonId = -1;

    private int armUncommonId = -1;

    private int armRareId = -1;

    public SpawnParts() {
    }

    public IWorldGen getGenerator() throws WorldGenLoadException {
        if (this.vanillaGenerator == null) {
            HytaleWorldGenProvider hytaleProvider = new HytaleWorldGenProvider();
            this.vanillaGenerator = hytaleProvider.getGenerator();
        }
        return this;
    }

    @Nullable
    public WorldGenTimingsCollector getTimings() {
        return (this.vanillaGenerator != null) ? this.vanillaGenerator.getTimings() : null;
    }

    public CompletableFuture<GeneratedChunk> generate(int seed, long index, int x, int z, LongPredicate stillNeeded) {
        if (this.vanillaGenerator == null)
            return CompletableFuture.failedFuture((Throwable)new WorldGenLoadException("Vanilla generator not set"));
        this.random = new Random(seed ^ x * 31L + z);
        initializeBlockIds();
        return this.vanillaGenerator.generate(seed, index, x, z, stillNeeded)
                .thenApply(chunk -> {
                    if (chunk != null)
                        modifyOres(chunk, x, z);
                    return chunk;
                });
    }

    private void initializeBlockIds() {
        if (this.decoBoneSkullsId == -1) {
            int count = BlockType.getAssetMap().getAssetCount();
            int foundCount = 0;
            for (int i = 0; i < count; i++) {
                BlockType block = (BlockType)BlockType.getAssetMap().getAsset(i);
                if (block != null) {
                    String id = block.getId();

                    if ("Deco_Bone_Skulls".equals(id)) this.decoBoneSkullsId = i;
                    else if ("Deco_Bone_Pile".equals(id)) this.decoBonePileId = i;

                    else if ("BodyCommon".equals(id)) this.bodyCommonId = i;
                    else if ("BodyUncommon".equals(id)) this.bodyUncommonId = i;

                    else if ("HeadCommon".equals(id)) this.headCommonId = i;
                    else if ("HeadUncommon".equals(id)) this.headUncommonId = i;

                    else if ("LegCommon".equals(id)) this.legCommonId = i;
                    else if ("LegUncommon".equals(id)) this.legUncommonId = i;
                    else if ("LegRare".equals(id)) this.legRareId = i;

                    else if ("ArmCommon".equals(id)) this.armCommonId = i;
                    else if ("ArmUncommon".equals(id)) this.armUncommonId = i;
                    else if ("ArmRare".equals(id)) this.armRareId = i;
                }
            }
        }
    }

    private void modifyOres(GeneratedChunk chunk, int chunkX, int chunkZ) {
        if (chunk == null)
            return;
        GeneratedBlockChunk blockChunk = chunk.getBlockChunk();
        if (blockChunk == null)
            return;
        int chunkReplacements = 0;
        for (int x = 0; x < 32; x++) {
            for (int z = 0; z < 32; z++) {
                for (int y = 0; y < 320; y++) {
                    try {
                        int currentBlockId = blockChunk.getBlock(x, y, z);
                        int blockToSpawn = -1;
                        if (currentBlockId == this.decoBonePileId) {
                            if (this.random.nextDouble() < 0.50){

                                int partType = this.random.nextInt(3);
                                double rarityRoll = this.random.nextDouble();
                                if (partType == 0) {
                                    if (rarityRoll < 0.70) blockToSpawn = this.bodyCommonId;
                                    else blockToSpawn = this.bodyUncommonId;
                                }else if (partType == 1) {
                                    if (rarityRoll < 0.50) blockToSpawn = this.legCommonId;
                                    else if (rarityRoll < 0.85) blockToSpawn = this.legUncommonId;
                                    else blockToSpawn = this.legRareId;
                                }else {
                                    if (rarityRoll < 0.50) blockToSpawn = this.armCommonId;
                                    else if (rarityRoll < 0.85) blockToSpawn = this.armUncommonId;
                                    else blockToSpawn = this.armRareId;
                                }
                                if (blockToSpawn != -1) {
                                    int rotation = blockChunk.getRotationIndex(x, y, z);
                                    blockChunk.setBlock(x, y, z, blockToSpawn, rotation, 0);
                                    chunkReplacements++;
                                }
                            }
                        }else if(currentBlockId == this.decoBoneSkullsId){
                            if (this.random.nextDouble() < 0.25){
                                double rarityRoll = this.random.nextDouble();

                                if (rarityRoll < 0.70) blockToSpawn = this.headCommonId;
                                else blockToSpawn = this.headUncommonId;

                                if (blockToSpawn != -1) {
                                    int rotation = blockChunk.getRotationIndex(x, y, z);
                                    blockChunk.setBlock(x, y, z, blockToSpawn, rotation, 0);
                                    chunkReplacements++;
                                }
                            }

                        }
                    } catch (Exception exception) {}
                }
            }
        }
    }

    @Deprecated
    public Transform[] getSpawnPoints(int seed) {
        return (this.vanillaGenerator != null) ? this.vanillaGenerator.getSpawnPoints(seed) : new Transform[0];
    }

    public void shutdown() {
        if (this.vanillaGenerator != null) {
            this.vanillaGenerator.shutdown();
        }
    }

}
