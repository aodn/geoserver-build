package au.org.emii.gogoduck.download;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Configuration setting for the ParallelDownloadManager
 */
public class Config {
    private final Path downloadDirectory;
    private final int poolSize;
    private final long localStorageLimit;
    private final int downloadAttempts;
    private final int connectTimeOut;
    private final int readTimeOut;

    private Config(ConfigBuilder builder) {
        downloadDirectory = builder.downloadDirectory;
        poolSize = builder.poolSize;
        localStorageLimit = builder.localStorageLimit;
        downloadAttempts = builder.downloadAttempts;
        connectTimeOut = builder.connectTimeOut;
        readTimeOut = builder.readTimeOut;
    }

    public Path getDownloadDirectory() {
        return downloadDirectory;
    }

    public int getPoolSize() {
        return poolSize;
    }

    public long getLocalStorageLimit() {
        return localStorageLimit;
    }

    public int getDownloadAttempts() {
        return downloadAttempts;
    }

    public int getConnectTimeOut() {
        return connectTimeOut;
    }

    public int getReadTimeOut() {
        return readTimeOut;
    }

    public static class ConfigBuilder {
        private Path downloadDirectory;
        private int poolSize;
        private long localStorageLimit;
        private int downloadAttempts;
        private int connectTimeOut;
        private int readTimeOut;

        public ConfigBuilder() {
            downloadDirectory = Paths.get(System.getProperty("java.io.tmpdir"));
            poolSize = 8;
            localStorageLimit = 100 * 1024 * 1024; // 100MiB
            downloadAttempts = 3;
            connectTimeOut = 60 * 1000; // 60 seconds
            readTimeOut = connectTimeOut;
        }

        public ConfigBuilder setDownloadDirectory(Path downloadDirectory) {
            this.downloadDirectory = downloadDirectory;
            return this;
        }

        public ConfigBuilder setPoolSize(int poolSize) {
            this.poolSize = poolSize;
            return this;
        }

        public ConfigBuilder setLocalStorageLimit(long localStorageLimit) {
            this.localStorageLimit = localStorageLimit;
            return this;
        }

        public ConfigBuilder setDownloadAttempts(int downloadAttempts) {
            this.downloadAttempts = downloadAttempts;
            return this;
        }

        public ConfigBuilder setConnectTimeOut(int connectTimeOut) {
            this.connectTimeOut = connectTimeOut;
            return this;
        }

        public ConfigBuilder setReadTimeOut(int readTimeOut) {
            this.readTimeOut = readTimeOut;
            return this;
        }

        public Config build() {
            return new Config(this);
        }
    }
}
