package com.hytalemodjam.rustborn.event;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.asset.type.item.config.CraftingRecipe;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.asset.type.item.config.ItemQuality;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.CraftRecipeEvent;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.MaterialQuantity;
import com.hypixel.hytale.server.core.inventory.container.CombinedItemContainer;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.bson.BsonDocument;
import org.bson.BsonString;

import javax.annotation.Nonnull;

public class CraftEvent extends EntityEventSystem<EntityStore, CraftRecipeEvent.Pre> {
    public CraftEvent() {
        super(CraftRecipeEvent.Pre.class);
    }
    @Override
    public void handle(int index,
                       @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
                       @Nonnull Store<EntityStore> store,
                       @Nonnull CommandBuffer<EntityStore> commandBuffer,
                       @Nonnull CraftRecipeEvent.Pre craftRecipeEvent) {
        CraftingRecipe recipe = craftRecipeEvent.getCraftedRecipe();
        Ref<EntityStore> ref = archetypeChunk.getReferenceTo(index);
        Player player = store.getComponent(ref, Player.getComponentType());
        if (recipe.getInput() != null && player != null) {
            for (MaterialQuantity output : recipe.getOutputs()) {
                if (output.getItemId() != null && output.getItemId().toLowerCase().contains("rustborn_core")){
                    BsonDocument bsonDocument = new BsonDocument();
                    Inventory inventory = player.getInventory();
                    CombinedItemContainer combined = inventory.getCombinedHotbarFirst();
                    for (MaterialQuantity input : recipe.getInput()) {
                        if (input.getItemId() != null) {
                            ItemStack item = new ItemStack(input.getItemId());
                            Item item1 = item.getItem();
                            ItemQuality asset = ItemQuality.getAssetMap().getAsset(item1.getQualityIndex());
                            assert asset != null;
                            bsonDocument.put(input.getItemId(), new BsonString(asset.getId()));

                            combined.removeItemStack(item);
                        }
                    }
                    ItemStack itemStack = new ItemStack(output.getItemId(), 1, bsonDocument);
                    combined.addItemStack(itemStack);
                    craftRecipeEvent.setCancelled(true);
                }
            }
        }
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Query.any();
    }
}
