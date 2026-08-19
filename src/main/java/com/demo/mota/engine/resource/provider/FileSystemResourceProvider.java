package com.demo.mota.engine.resource.provider;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 文件系统资源提供者：从项目路径之外的某个目录读取资源。
 * <p>
 * 目录结构应与资源根目录保持一致，例如 baseDir 为 D:/mota-resources 时，
 * /data/map/floor_1.json 对应 D:/mota-resources/data/map/floor_1.json。
 * 由此可以支持"资源外置"（如 mod 目录、用户自定义资源包）。
 */
public class FileSystemResourceProvider implements ResourceProvider {

    private final Path baseDir;

    public FileSystemResourceProvider(Path baseDir) {
        this.baseDir = baseDir.toAbsolutePath().normalize();
    }

    @Override
    public InputStream openStream(String resourcePath) throws IOException {
        Path file = resolve(resourcePath);
        return Files.exists(file) ? Files.newInputStream(file) : null;
    }

    @Override
    public URL getResourceUrl(String resourcePath) {
        Path file = resolve(resourcePath);
        if (!Files.exists(file)) {
            return null;
        }
        try {
            return file.toUri().toURL();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * 去除前导 / 后与 baseDir 拼接，并做规范化 + 越界检查。
     */
    private Path resolve(String resourcePath) {
        String relative = resourcePath.startsWith("/") ? resourcePath.substring(1) : resourcePath;
        Path file = baseDir.resolve(relative).normalize();
        if (!file.startsWith(baseDir)) {
            throw new IllegalArgumentException("Resource path escapes base dir: " + resourcePath);
        }
        return file;
    }
}
