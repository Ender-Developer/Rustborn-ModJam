package com.hytalemodjam.rustborn.component;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;
import com.hypixel.hytale.codec.codecs.simple.StringCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hytalemodjam.rustborn.Rustborn;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

import java.util.HashMap;
import java.util.Map;

public class RobotRepairComponent implements Component<EntityStore> {

    public static BuilderCodec<RobotRepairComponent> CODEC = BuilderCodec.builder(RobotRepairComponent.class, RobotRepairComponent::new)
            .append(new KeyedCodec<>("ReamingRepair", Codec.INTEGER), (v, s) -> v.remainingRepair = s, v -> v.remainingRepair).add()
            .append(new KeyedCodec<>("RobotPart", new MapCodec<>(StringCodec.STRING, HashMap::new)), (v, s) -> v.robotParts = s, v -> v.robotParts).add()
            .append(new KeyedCodec<>("Damage", Codec.FLOAT), (v, s) -> v.damage = s, v -> v.damage).add()
            .build();

    public int remainingRepair;
    public Map<String, String> robotParts;
    public float damage;

    public RobotRepairComponent() {

    }

    public RobotRepairComponent( int remainingRepair, float damage, Map<String, String> robotParts ) {
        this.remainingRepair = remainingRepair;
        this.damage = damage;
        this.robotParts = robotParts;
    }

    public RobotRepairComponent( int remainingRepair ) {
        this(remainingRepair, 20.0F);
    }

    public RobotRepairComponent( int remainingRepair, float damage ) {
        this.remainingRepair = remainingRepair;
        this.robotParts = new HashMap<>();
        this.damage = damage;
    }

    public RobotRepairComponent(RobotRepairComponent other){
        this.remainingRepair = other.remainingRepair;
        this.robotParts = other.robotParts;
        this.damage = other.damage;
    }

    public void addPart(String key, String value){
        this.robotParts.put(key, value);
    }

    public void calculateDamage(){
        if (!robotParts.isEmpty()){
            float damage = 0;
            for (Map.Entry<String, String> entry : robotParts.entrySet()) {
                String name = entry.getKey();
                String rarity = entry.getValue().toLowerCase();
                switch (rarity){
                    case "common":
                        damage += 5;
                        break;
                    case "uncommon":
                        damage += 10;
                        break;
                    case "rare":
                        damage += 15;
                        break;
                    default:
                        damage += 1;
                }
            }
            this.damage = damage;
        }
    }

    public Float[] calculateHealthAndScale( ) {
        if (!robotParts.isEmpty()){
            float health = 0;
            float scale = 0;
            for (Map.Entry<String, String> entry : robotParts.entrySet()) {
                String rarity = entry.getValue().toLowerCase();
                switch (rarity){
                    case "common":
                        health += 10;
                        scale += 0.03F;
                        break;
                    case "uncommon":
                        health += 15;
                        scale += 0.06F;
                        break;
                    case "rare":
                        health += 20;
                        scale += 0.09F;
                        break;
                    default:
                        health += 5;
                        scale += 0.01F;
                }
            }
            return new Float[]{ health, scale };
        }
        return new Float[]{ 0.0F, 0.0F };
    }

    public int getRemainingRepair() {
        return remainingRepair;
    }

    public Map<String, String> getRobotParts() {
        return robotParts;
    }

    public float getDamage() {
        return damage;
    }

    @NullableDecl
    @Override
    public Component<EntityStore> clone() {
        return new RobotRepairComponent(this);
    }

    public static ComponentType<EntityStore, RobotRepairComponent> getComponentType(){
        return Rustborn.get().robotRepairComponentComponentType;
    }
}
