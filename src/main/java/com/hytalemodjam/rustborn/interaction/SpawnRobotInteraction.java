package com.hytalemodjam.rustborn.interaction;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.RotationTuple;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAttachment;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.Modifier;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.StaticModifier;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.client.SimpleBlockInteraction;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.BlockChunk;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockSection;
import com.hypixel.hytale.server.core.universe.world.npc.INonPlayerCharacter;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.NPCPlugin;
import com.hytalemodjam.rustborn.component.RobotRepairComponent;
import it.unimi.dsi.fastutil.Pair;
import org.bson.BsonDocument;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class SpawnRobotInteraction extends SimpleBlockInteraction {
    @Nonnull
    protected String entityId;
    @Nonnull
    protected Vector3d spawnOffset = new Vector3d();

    public static final BuilderCodec<SpawnRobotInteraction> CODEC = BuilderCodec.builder(SpawnRobotInteraction.class, SpawnRobotInteraction::new, SimpleBlockInteraction.CODEC)
            .append(new KeyedCodec<>("EntityId", Codec.STRING), (i, s) -> i.entityId = s, i -> i.entityId).add()
            .append(new KeyedCodec<>("SpawnOffset", Vector3d.CODEC), (i, s) -> i.spawnOffset.assign(s), i -> i.spawnOffset).add().build();

    private void spawnNPC(@Nonnull Store<EntityStore> store, @Nonnull Vector3i targetBlock, ItemStack itemStack) {
        World world = store.getExternalData().getWorld();
        SpawnData spawnData = this.computeSpawnData(world, targetBlock);
        Pair<Ref<EntityStore>, INonPlayerCharacter> pair = NPCPlugin.get().spawnNPC(store, this.entityId, (String) null, spawnData.position(), spawnData.rotation());
        if (pair == null) {
            System.out.println("pair null");
            return;
        }
        Ref<EntityStore> ref = pair.key();
        if (itemStack.getMetadata() != null){
            BsonDocument metadata = itemStack.getMetadata();
            RobotRepairComponent robotRepairComponent = new RobotRepairComponent(3);
            metadata.forEach((k, v) -> {
                robotRepairComponent.addPart(k, v.asString().getValue());
            });
            robotRepairComponent.calculateDamage();
            EntityStatMap entityStats = store.getComponent(ref, EntityStatMap.getComponentType());
            if (entityStats != null){
                EntityStatValue healthStat = entityStats.get("Health");
                if (healthStat != null){
                    Float[] healthAndScales = robotRepairComponent.calculateHealthAndScale();
                    float health = healthAndScales[0];
                    float scale = healthAndScales[1];
                    StaticModifier staticModifier = new StaticModifier(Modifier.ModifierTarget.MAX, StaticModifier.CalculationType.ADDITIVE, health);

                    entityStats.putModifier(healthStat.getIndex(), "MaxHealth", staticModifier);
                    entityStats.setStatValue(healthStat.getIndex(), healthStat.getMax());

                    entityStats.update();

                    ModelComponent modelComponent = store.getComponent(ref, ModelComponent.getComponentType());
                    if ( modelComponent != null ) {

                        Model newModel = getNewModel(modelComponent, scale - 0.25F);

                        store.putComponent(ref, ModelComponent.getComponentType(), new ModelComponent(newModel));
                    }
                }
            }
            store.putComponent(ref, RobotRepairComponent.getComponentType(), robotRepairComponent);
        }
    }

    @NonNullDecl
    private Model getNewModel( ModelComponent modelComponent, float scale ) {
        Model oldModel = modelComponent.getModel();
        ModelAttachment[] attachments = new ModelAttachment[]{};

        float newScale = oldModel.getScale() * (1 + scale);

        return new Model(
                oldModel.getModelAssetId(),
                newScale,
                oldModel.getRandomAttachmentIds(),
                attachments,
                oldModel.getBoundingBox(),
                oldModel.getModel(),
                oldModel.getTexture(),
                oldModel.getGradientSet(),
                oldModel.getGradientId(),
                oldModel.getEyeHeight(),
                oldModel.getCrouchOffset(),
                oldModel.getAnimationSetMap(),
                oldModel.getCamera(),
                oldModel.getLight(),
                oldModel.getParticles(),
                oldModel.getTrails(),
                oldModel.getPhysicsValues(),
                oldModel.getDetailBoxes(),
                oldModel.getPhobia(),
                oldModel.getPhobiaModelAssetId()
        );
    }

    @Nonnull
    private SpawnData computeSpawnData(@Nonnull World world, @Nonnull Vector3i targetBlock) {
        long chunkIndex = ChunkUtil.indexChunkFromBlock(targetBlock.x, targetBlock.z);
        ChunkStore chunkStore = world.getChunkStore();
        Ref<ChunkStore> chunkRef = chunkStore.getChunkReference(chunkIndex);
        if (chunkRef != null && chunkRef.isValid()) {
            WorldChunk worldChunkComponent = (WorldChunk)chunkStore.getStore().getComponent(chunkRef, WorldChunk.getComponentType());

            assert worldChunkComponent != null;

            BlockType blockType = worldChunkComponent.getBlockType(targetBlock.x, targetBlock.y, targetBlock.z);
            if (blockType == null) {
                return new SpawnData(this.spawnOffset.clone().add(targetBlock).add((double)0.5F, (double)0.5F, (double)0.5F), Vector3f.ZERO);
            } else {
                BlockChunk blockChunkComponent = (BlockChunk)chunkStore.getStore().getComponent(chunkRef, BlockChunk.getComponentType());
                if (blockChunkComponent == null) {
                    return new SpawnData(this.spawnOffset.clone().add(targetBlock).add((double)0.5F, (double)0.5F, (double)0.5F), Vector3f.ZERO);
                } else {
                    BlockSection section = blockChunkComponent.getSectionAtBlockY(targetBlock.y);
                    int rotationIndex = section.getRotationIndex(targetBlock.x, targetBlock.y, targetBlock.z);
                    RotationTuple rotationTuple = RotationTuple.get(rotationIndex);
                    Vector3d position = rotationTuple.rotate(this.spawnOffset);
                    Vector3d blockCenter = new Vector3d();
                    blockType.getBlockCenter(rotationIndex, blockCenter);
                    position.add(blockCenter).add(targetBlock);
                    Vector3f rotation = new Vector3f(0.0F, (float)(rotationTuple.yaw().getRadians() + Math.toRadians(0)), 0.0F);
                    return new SpawnData(position, rotation);
                }
            }
        } else {
            return new SpawnData(this.spawnOffset.clone().add(targetBlock).add((double)0.5F, (double)0.5F, (double)0.5F), Vector3f.ZERO);
        }
    }

    protected void interactWithBlock(@Nonnull World world, @Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull InteractionType type, @Nonnull InteractionContext context, @Nullable ItemStack itemInHand, @Nonnull Vector3i targetBlock, @Nonnull CooldownHandler cooldownHandler) {
        commandBuffer.run((store) -> this.spawnNPC(world.getEntityStore().getStore(), targetBlock, itemInHand));
    }

    protected void simulateInteractWithBlock(@Nonnull InteractionType type, @Nonnull InteractionContext context, @Nullable ItemStack itemInHand, @Nonnull World world, @Nonnull Vector3i targetBlock) {
        CommandBuffer<EntityStore> commandBuffer = context.getCommandBuffer();

        assert commandBuffer != null;

        commandBuffer.run((store) -> this.spawnNPC(world.getEntityStore().getStore(), targetBlock, itemInHand));
    }

    private static record SpawnData(@Nonnull Vector3d position, @Nonnull Vector3f rotation) {
    }
}
