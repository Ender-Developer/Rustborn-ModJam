package com.hytalemodjam.rustborn.listener;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.Packet;
import com.hypixel.hytale.protocol.packets.interaction.SyncInteractionChain;
import com.hypixel.hytale.protocol.packets.interaction.SyncInteractionChains;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAttachment;
import com.hypixel.hytale.server.core.auth.PlayerAuthentication;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.io.PacketHandler;
import com.hypixel.hytale.server.core.io.adapter.PacketWatcher;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.TargetUtil;
import com.hytalemodjam.rustborn.component.RobotRepairComponent;
import com.hytalemodjam.rustborn.ui.RustbornInfoPage;

import java.util.UUID;

public class OpenRustbornInfoMenuPacket implements PacketWatcher {


    @Override
    public void accept( PacketHandler packetHandler, Packet packet ) {
        if (packet.getId() != 290) return;
        if ( !(packet instanceof SyncInteractionChains interactionChains) ) return;
        boolean firstRun = true;

        SyncInteractionChain[] updates = interactionChains.updates;

        for ( SyncInteractionChain chain : updates ) {
            InteractionType type = chain.interactionType;
            if (!chain.initial) break;
            if ( type == InteractionType.Use ) {
                if (firstRun) {
                    PlayerAuthentication auth = packetHandler.getAuth();
                    if ( auth == null ) break;

                    UUID user = auth.getUuid();
                    PlayerRef playerRef = Universe.get().getPlayer(user);
                    if ( playerRef == null ) break;
                    UUID worldUuid = playerRef.getWorldUuid();
                    if ( worldUuid == null ) break;
                    World world = Universe.get().getWorld(worldUuid);
                    if ( world == null ) break;

                    world.execute(() -> {
                        EntityStore entityStore = world.getEntityStore();
                        Store<EntityStore> store = entityStore.getStore();

                        Ref<EntityStore> ref = playerRef.getReference();
                        Player player = store.getComponent(ref, Player.getComponentType());
                        Ref<EntityStore> targetRef = TargetUtil.getTargetEntity(ref, 8.0F, store);

                        if ( targetRef == null ) return;

                        ModelComponent modelComponent = store.getComponent(targetRef, ModelComponent.getComponentType());
                        RobotRepairComponent robotRepairComponent = store.getComponent(targetRef, RobotRepairComponent.getComponentType());

                        if ( modelComponent != null && modelComponent.getModel() != null && robotRepairComponent != null ) {
                            String entityID = modelComponent.getModel().getModelAssetId();

                            if ( entityID != null && entityID.equalsIgnoreCase("robot") ) {
                                if ( player != null ) {
                                    player.getPageManager().openCustomPage(
                                            ref,
                                            store,
                                            new RustbornInfoPage(playerRef, targetRef, robotRepairComponent)
                                    );
                                }
                            }
                        }
                    });
                    firstRun = false;
                    break;
                }
            }
        }
    }
}
