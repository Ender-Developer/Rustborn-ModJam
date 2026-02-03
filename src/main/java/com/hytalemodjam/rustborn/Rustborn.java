package com.hytalemodjam.rustborn;

import com.hypixel.hytale.codec.lookup.Priority;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.io.adapter.PacketAdapters;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.universe.world.worldgen.provider.IWorldGenProvider;
import com.hytalemodjam.rustborn.component.RobotRepairComponent;
import com.hytalemodjam.rustborn.event.CraftEvent;
import com.hytalemodjam.rustborn.event.DamageEvent;
import com.hytalemodjam.rustborn.event.OnJoinEvent;
import com.hytalemodjam.rustborn.interaction.SpawnRobotInteraction;
import com.hytalemodjam.rustborn.listener.OpenRustbornInfoMenuPacket;

import javax.annotation.Nonnull;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Rustborn extends JavaPlugin {

    public static HytaleLogger LOGGER = HytaleLogger.get("Rustborn");
    private static Rustborn instance;
    public ComponentType<EntityStore, RobotRepairComponent> robotRepairComponentComponentType;


    public Rustborn( @Nonnull JavaPluginInit init) {
        super(init);
        instance = this;
    }

    @Override
    protected void setup() {
        super.setup();
        String currentlyYear = (new SimpleDateFormat("yyyy")).format(new Date());
        LOGGER.atInfo().log("");
        LOGGER.atInfo().log("=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=");
        LOGGER.atInfo().log("                                ");
        LOGGER.atInfo().log(" :: " + getManifest().getName() + " :: (v" + getManifest().getVersion().toString() + ")");
        LOGGER.atInfo().log(" [!] Copyright (c) 2026-" + currentlyYear);
        LOGGER.atInfo().log("");
        LOGGER.atInfo().log("=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=");
        LOGGER.atInfo().log("");
        try {
            IWorldGenProvider.CODEC.register(Priority.NORMAL, "Rustborn-GEN", SpawnParts.class, SpawnParts.CODEC);
        } catch (Exception e) {
            e.printStackTrace();
        }
        // ===========================REGISTRIES===========================
        PacketAdapters.registerInbound(new OpenRustbornInfoMenuPacket());
        this.robotRepairComponentComponentType = this.getEntityStoreRegistry().registerComponent(RobotRepairComponent.class, "RobotRepair", RobotRepairComponent.CODEC);
        this.getEntityStoreRegistry().registerSystem(new CraftEvent());
        this.getCodecRegistry(Interaction.CODEC).register("SpawnEntity", SpawnRobotInteraction.class, SpawnRobotInteraction.CODEC);
        this.getEntityStoreRegistry().registerSystem(new DamageEvent());
        this.getEventRegistry().registerGlobal(PlayerReadyEvent.class, OnJoinEvent::onPlayerReadyEvent);
        // ================================================================
    }

    public static Rustborn get() {
        return instance;
    }
}