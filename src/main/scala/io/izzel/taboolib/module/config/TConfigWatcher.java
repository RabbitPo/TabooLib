package io.izzel.taboolib.module.config;

import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.apache.commons.lang3.tuple.Triple;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * TConfig 配置文件改动监听工具
 *
 * @author lzzelAliz
 */
public class TConfigWatcher {

    private final static TConfigWatcher configWatcher = new TConfigWatcher();
    private final ScheduledExecutorService service = Executors.newScheduledThreadPool(1, new BasicThreadFactory.Builder().namingPattern("TConfigWatcherService-%d").build());
    private final Map<WatchService, Triple<File, Object, Consumer<Object>>> map = new HashMap<>();

    public TConfigWatcher() {
        service.scheduleAtFixedRate(() -> {
            synchronized (map) {
                map.forEach((service, triple) -> {
                    WatchKey key;
                    while ((key = service.poll()) != null) {
                        for (WatchEvent<?> watchEvent : key.pollEvents()) {
                            if (triple.getLeft().getName().equals(Objects.toString(watchEvent.context()))) {
                                triple.getRight().accept(triple.getMiddle());
                            }
                        }
                        key.reset();
                    }
                });
            }
        }, 1000, 100, TimeUnit.MILLISECONDS);
    }

    public static TConfigWatcher getInst() {
        return configWatcher;
    }

    public void addSimpleListener(File file, Runnable runnable) {
        try {
            addListener(file, null, obj -> runnable.run());
        } catch (Throwable ignored) {
        }
    }

    public void addOnListen(File file, Object obj, Consumer<Object> consumer) {
        try {
            WatchService service = FileSystems.getDefault().newWatchService();
            file.getParentFile().toPath().register(service, StandardWatchEventKinds.ENTRY_MODIFY);
            map.putIfAbsent(service, Triple.of(file, obj, consumer));
        } catch (Throwable ignored) {
        }
    }

    @SuppressWarnings("unchecked")
    public <T> void addListener(File file, T obj, Consumer<T> consumer) {
        addOnListen(file, obj, (Consumer<Object>) consumer);
    }

    public boolean hasListener(File file) {
        synchronized (map) {
            return map.values().stream().anyMatch(t -> t.getLeft().getPath().equals(file.getPath()));
        }
    }

    public void runListener(File file) {
        synchronized (map) {
            map.values().stream().filter(t -> t.getLeft().getPath().equals(file.getPath())).forEach(f -> f.getRight().accept(null));
        }
    }

    public void removeListener(File file) {
        synchronized (map) {
            map.entrySet().removeIf(entry -> {
                if (entry.getValue().getLeft().getPath().equals(file.getPath())) {
                    try {
                        entry.getKey().close();
                    } catch (IOException ignored) {
                    }
                    return true;
                }
                return false;
            });
        }
    }

    public void unregisterAll() {
        service.shutdown();
        map.forEach((service, pair) -> {
            try {
                service.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}
