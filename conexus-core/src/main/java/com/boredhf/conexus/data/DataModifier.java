package com.boredhf.conexus.data;

/**
 * Functional interface for atomic updates to player data.
 */
@FunctionalInterface
public interface DataModifier<T extends PlayerData> {
    T apply(T current);
}
