package com.boredhf.conexus.data;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.time.Instant;

/**
 * Base interface for all player data types.
 * 
 * Player data must be serializable and versioned to support
 * schema evolution and conflict resolution.
 * 
 * @author BoredHF
 * @since 1.0.0
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
public interface PlayerData {
    
    /**
     * Gets the version of this data structure.
     * Used for schema evolution and migration.
     * 
     * @return the data version
     */
    int getVersion();
    
    /**
     * Gets the timestamp when this data was last modified.
     * Used for conflict resolution.
     * 
     * @return the last modified timestamp
     */
    Instant getLastModified();
    
    /**
     * Gets the ID of the server that last modified this data.
     * Used for conflict resolution and audit trails.
     * 
     * @return the server ID that last modified this data
     */
    String getLastModifiedBy();
    
    /**
     * Gets the data type identifier.
     * 
     * @return the data type identifier
     */
    default String getDataType() {
        return this.getClass().getSimpleName();
    }
    
    /**
     * Creates a copy of this data with updated metadata.
     * 
     * @param serverId the server ID making the modification
     * @return a new instance with updated metadata
     */
    PlayerData withUpdatedMetadata(String serverId);
}