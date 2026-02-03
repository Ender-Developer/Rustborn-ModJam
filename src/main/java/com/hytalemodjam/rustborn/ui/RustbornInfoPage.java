package com.hytalemodjam.rustborn.ui;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.protocol.packets.interface_.NotificationStyle;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.container.CombinedItemContainer;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.NotificationUtil;
import com.hytalemodjam.rustborn.component.RobotRepairComponent;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.awt.*;
import java.util.concurrent.atomic.AtomicReference;

public class RustbornInfoPage extends InteractiveCustomUIPage<RustbornInfoPage.Data> {

    private RobotRepairComponent robotComp;
    private Ref<EntityStore> targetRef;

    private EntityStatMap entityStat;
    private EntityStatValue entityStatValue;
    private float currentHealth;

    public RustbornInfoPage( @NonNullDecl PlayerRef playerRef, Ref<EntityStore> targetRef, RobotRepairComponent robotComp ) {
        super(playerRef, CustomPageLifetime.CanDismiss, Data.CODEC);
        this.robotComp = robotComp;
        this.targetRef = targetRef;
    }

    @Override
    public void build( @NonNullDecl Ref<EntityStore> ref, @NonNullDecl UICommandBuilder uiCommandBuilder, @NonNullDecl UIEventBuilder uiEventBuilder, @NonNullDecl Store<EntityStore> store ) {
        uiCommandBuilder.append("Pages/RustbornInfoPage.ui");

        uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating,
                "#RepairButton",
                new EventData().append("Type", "RepairButton"),
                false
        );

        uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating,
                "#Close",
                new EventData().append("Type", "CloseButton"),
                false
        );

        entityStat = store.getComponent(targetRef, EntityStatMap.getComponentType());
        entityStatValue = entityStat.get("Health");
        currentHealth = entityStatValue.get();

        this.buildModel(uiCommandBuilder);
    }

    @Override
    public void handleDataEvent( @NonNullDecl Ref<EntityStore> ref, @NonNullDecl Store<EntityStore> store, @NonNullDecl Data data ) {
        UICommandBuilder cmd = new UICommandBuilder();
        UIEventBuilder eventBuilder = new UIEventBuilder();

        var remainingRepair = robotComp.getRemainingRepair();

        switch ( data.type ) {
            case "CloseButton" -> close();
            case "RepairButton" -> {
                Player player = store.getComponent(ref, Player.getComponentType());

                if ( player == null ) return;

                Inventory inventory = player.getInventory();
                CombinedItemContainer combined = inventory.getCombinedHotbarFirst();

                ItemContainer container = combined.getContainer(combined.getContainersSize() - 1);

                AtomicReference<Short> itemShort = new AtomicReference<>((short) 0);

                container.forEach(( slot, itemStack ) -> {
                    if ( itemStack.getItemId().equalsIgnoreCase("rustborn_repair") ) {
                        itemShort.set(slot);
                    }
                });

                if ( itemShort.get() == 0 ) {
                    container = combined.getContainer(0);

                    container.forEach(( slot, itemStack ) -> {
                        if ( itemStack.getItemId().equalsIgnoreCase("rustborn_repair") ) {
                            itemShort.set(slot);
                        }
                    });
                }

                if ( itemShort.get() == 0 ) {
                    NotificationUtil.sendNotification(
                            playerRef.getPacketHandler(),
                            Message.join(
                                    Message.raw("You can't repair ").color(Color.RED),
                                    Message.raw("'Rustborn' ").color(Color.ORANGE),
                                    Message.raw("because you don't have any ").color(Color.RED),
                                    Message.raw("Rustborn Repairer").color(Color.MAGENTA)
                            ),
                            NotificationStyle.Danger
                    );
                    close();
                    return;
                }

                if ( container.getItemStack(itemShort.get()) == null ) return;

                if ( remainingRepair <= 0 ) {
                    NotificationUtil.sendNotification(
                            playerRef.getPacketHandler(),
                            Message.join(
                                    Message.raw("You can't repair ").color(Color.RED),
                                    Message.raw("'Rustborn' ").color(Color.ORANGE),
                                    Message.raw("because you don't have any remaining repairs.").color(Color.RED)
                            ),
                            NotificationStyle.Danger
                    );
                    close();
                    return;
                }

                if ( currentHealth >= entityStatValue.getMax() ) {
                    close();
                    NotificationUtil.sendNotification(
                            playerRef.getPacketHandler(),
                            Message.join(
                                    Message.raw("You can't repair ").color(Color.YELLOW),
                                    Message.raw("'Rustborn' ").color(Color.ORANGE),
                                    Message.raw("because it's full of durability.").color(Color.YELLOW)
                            ),
                            NotificationStyle.Warning
                    );
                    return;
                }

                remainingRepair--;

                if ( currentHealth < entityStatValue.getMax() ) {
                    entityStat.setStatValue(entityStatValue.getIndex(), entityStatValue.getMax());
                    entityStat.update();
                }

                currentHealth = entityStatValue.get();

                int healthInPercentage = (int) ((currentHealth / entityStatValue.getMax()) * 100);

                cmd.set("#DurabilityValue.Text", healthInPercentage + "%");
                cmd.set("#RepairValue.Text", "" + remainingRepair);

                store.putComponent(targetRef,
                        RobotRepairComponent.getComponentType(),
                        new RobotRepairComponent(remainingRepair, robotComp.getDamage(), robotComp.getRobotParts())
                );
                container.removeItemStackFromSlot(itemShort.get(), 1);
                this.sendUpdate(cmd, eventBuilder, false);
            }
        }
    }

    private void buildModel( UICommandBuilder cmd ) {
        var remainingRepair = robotComp.getRemainingRepair();

        var damage = (int) robotComp.getDamage();

        cmd.set("#DurabilityValue.Text", (int) ((currentHealth / entityStatValue.getMax()) * 100) + "%");
        cmd.set("#DamageValue.Text", "" + damage);
        cmd.set("#RepairValue.Text", "" + remainingRepair);

        robotComp.robotParts.forEach(( key, value ) -> {
            if ( key.toLowerCase().contains("head") ) cmd.set("#HeadValue.Text", value);
            if ( key.toLowerCase().contains("body") ) cmd.set("#BodyValue.Text", value);
            if ( key.toLowerCase().contains("arm") ) cmd.set("#ArmValue.Text", value);
            if ( key.toLowerCase().contains("leg") ) cmd.set("#LegValue.Text", value);
        });
    }

    public static class Data {
        private static final String KEY_TYPE = "Type";
        static final String KEY_VALUE = "Value";
        public String type;
        public String value;

        public static final BuilderCodec<Data> CODEC = BuilderCodec.builder(
                        Data.class, Data::new
                )
                .append(new KeyedCodec<>("Type", Codec.STRING), ( entry, s ) -> entry.type = s, entry -> entry.type)
                .add()
                .append(new KeyedCodec<>("Value", Codec.STRING), ( entry, s ) -> entry.value = s, entry -> entry.value)
                .add()
                .build();
    }
}
