package com.ai.manager.system.service.storage;

public interface LibraryFileStorage {

    String type();

    String save(String folder, String fileName, byte[] content, String extension);

    byte[] load(String storagePath);

    void delete(String storagePath);

    void ensureRoot();
}
