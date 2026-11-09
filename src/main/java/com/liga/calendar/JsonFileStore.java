package com.liga.calendar;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

/**
 * Reads and writes a single record type as a JSON array in one file, so an
 * in-memory store ({@link EventSignupStore}, {@link DeckReservationStore})
 * can survive an app restart without a real database. Every write replaces
 * the whole file with the store's current full list — fine at this app's
 * scale (a handful of writes per event date), and much simpler than
 * appending or diffing.
 * <p>
 * Writes go to a temp file first and are then moved into place, so a crash
 * or power loss mid-write can't leave a half-written, corrupt JSON file
 * behind — the previous good version stays until the new one is fully
 * written.
 */
class JsonFileStore<T> {

    private static final Logger log = LoggerFactory.getLogger(JsonFileStore.class);

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private final Path file;
    private final Path tempFile;
    private final CollectionType listType;

    JsonFileStore(String dataDir, String fileName, Class<T> recordType) {
        this.file = Path.of(dataDir, fileName);
        this.tempFile = Path.of(dataDir, fileName + ".tmp");
        this.listType = objectMapper.getTypeFactory().constructCollectionType(List.class, recordType);
    }

    List<T> readAll() {
        if (!Files.exists(file)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(file.toFile(), listType);
        } catch (IOException e) {
            log.error("Could not read {}, starting with an empty list instead: {}", file, e.getMessage());
            return List.of();
        }
    }

    void writeAll(List<T> values) {
        try {
            Files.createDirectories(file.getParent());
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(tempFile.toFile(), values);
            try {
                Files.move(tempFile, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tempFile, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            // The in-memory store already has the new value; it just won't
            // survive a restart until this starts working again (e.g. disk
            // full, permissions). Signing people up shouldn't fail over it.
            log.error("Could not persist {}: {}", file, e.getMessage());
        }
    }
}
