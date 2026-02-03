package com.hytalemodjam.rustborn.event;

import com.hypixel.hytale.builtin.weather.resources.WeatherResource;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.universe.world.WorldConfig;

public class OnJoinEvent {

    public static void onPlayerReadyEvent( PlayerReadyEvent event ) {
        var player = event.getPlayer();
        var world = player.getWorld();

        if ( world == null ) return;

        var entityStore = world.getEntityStore();
        var store = entityStore.getStore();
        String rustbornAmbience = "Rustborn_Ambience";
        WorldConfig config = world.getWorldConfig();

        WeatherResource weatherResource = store.getResource(WeatherResource.getResourceType());
        weatherResource.setForcedWeather(rustbornAmbience);

        config.setForcedWeather(rustbornAmbience);
        config.markChanged();
    }
}
