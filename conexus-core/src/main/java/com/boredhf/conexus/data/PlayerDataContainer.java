package com.boredhf.conexus.data;

import java.util.UUID;

/**
 * A convenience container for accessing and updating all data types for a player.
 */
public interface PlayerDataContainer {
    UUID getPlayerId();

    <T extends PlayerData> T get(Class<T> dataType);

    <T extends PlayerData> void set(T data);

    <T extends PlayerData> boolean has(Class<T> dataType);

    <T extends PlayerData> void delete(Class<T> dataType);
}
