package com.garyzhangscm.cwms.outbound.transit;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.*;
import java.time.Instant;
import java.util.concurrent.locks.ReentrantLock;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Atomic replacement + file lock, with revision checks to prevent lost admin updates. */
public class TransitConfigurationStore {
    private final Path path;
    private final ObjectMapper mapper = new ObjectMapper();
    private final ReentrantLock lock = new ReentrantLock();
    public TransitConfigurationStore(Path path) { this.path = path; }
    public TransitShippingConfiguration read() {
        try {
            if (Files.size(path) > 65536) throw new IOException("Configuration too large");
            TransitShippingConfiguration document = mapper.readValue(Files.readAllBytes(path), TransitShippingConfiguration.class);
            if (document.rules == null || document.rules.size() > 100 || document.revision < 0) throw new IOException("Invalid rules");
            return document;
        } catch (IOException | RuntimeException error) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "In-transit configuration unavailable; selected shipments must be checked before shipping");
        }
    }
    public TransitShippingConfiguration write(TransitShippingConfiguration submitted, String actor) {
        lock.lock();
        try {
            Files.createDirectories(path.getParent());
            try (FileChannel channel = FileChannel.open(path.resolveSibling(path.getFileName()+".lock"),
                    StandardOpenOption.CREATE, StandardOpenOption.WRITE); FileLock ignored = channel.lock()) {
                TransitShippingConfiguration previous = read();
                if (previous.revision != submitted.revision) throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Configuration changed. Refresh before saving.");
                submitted.revision = Math.addExact(previous.revision, 1);
                submitted.modifiedBy = actor;
                submitted.modifiedAt = Instant.now().toString();
                byte[] bytes = mapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(submitted);
                if (bytes.length > 65536) throw new IOException("Configuration too large");
                Path temp = Files.createTempFile(path.getParent(), ".transit-", ".json");
                try {
                    try (FileChannel output = FileChannel.open(temp, StandardOpenOption.WRITE)) {
                        ByteBuffer buffer = ByteBuffer.wrap(bytes);
                        while (buffer.hasRemaining()) output.write(buffer);
                        output.force(true);
                    }
                    Files.move(temp, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                } finally { Files.deleteIfExists(temp); }
                return submitted;
            }
        } catch (IOException error) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Could not save in-transit configuration");
        } finally { lock.unlock(); }
    }
}
