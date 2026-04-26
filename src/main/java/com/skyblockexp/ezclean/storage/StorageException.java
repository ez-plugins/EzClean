package com.skyblockexp.ezclean.storage;

/**
 * Thrown when the EzClean storage layer cannot initialise or operate correctly.
 */
public class StorageException extends Exception {

    public StorageException(String message) {
        super(message);
    }

    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
