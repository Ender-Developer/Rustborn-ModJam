package com.hytalemodjam.rustborn.event;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hytalemodjam.rustborn.component.RobotRepairComponent;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

public class DamageEvent extends DamageEventSystem {

    @Override
    public void handle( int idx, @NonNullDecl ArchetypeChunk<EntityStore> archetypeChunk, @NonNullDecl Store<EntityStore> store, @NonNullDecl CommandBuffer<EntityStore> commandBuffer, @NonNullDecl Damage damage ) {
        ComponentType<EntityStore, NPCEntity> componentType = NPCEntity.getComponentType();
        Ref<EntityStore> ref = archetypeChunk.getReferenceTo(idx);

        if (componentType != null) {
            Damage.Source source = damage.getSource();

            if (source instanceof Damage.EntitySource entitySource){
                NPCEntity component = store.getComponent(entitySource.getRef(), componentType);

                if (component != null && component.getRoleName().toLowerCase().contains("robot")){
                    RobotRepairComponent robotComp = store.getComponent(entitySource.getRef(), RobotRepairComponent.getComponentType());

                    if ( robotComp != null ) damage.setAmount(robotComp.damage);

                }
            }
        }
    }

    @NullableDecl
    @Override
    public Query<EntityStore> getQuery() {
        return Query.any();
    }
}
