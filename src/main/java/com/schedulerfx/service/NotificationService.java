package com.schedulerfx.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.schedulerfx.model.t2_schedule;

import javafx.application.Platform;
import javafx.scene.control.Alert;

public class NotificationService {
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final Map<Long, ScheduledFuture<?>> jobs = new ConcurrentHashMap<>();

    public void schedule(t2_schedule e) {
        cancel(e.getId());
        LocalDateTime fireAt = e.getStartAt().minusMinutes(e.getRemindMinutes());
        long delayMs = Duration.between(LocalDateTime.now(), fireAt).toMillis();
        if (delayMs <= 0) return;

        ScheduledFuture<?> f = scheduler.schedule(() -> Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("알림");
            alert.setHeaderText(null);
            alert.setContentText(e.getStartAt().toLocalTime() + " " + e.getTitle());
            alert.show();
        }), delayMs, TimeUnit.MILLISECONDS);

        jobs.put(e.getId(), f);
    }

    public void cancel(Long id) {
        if (id == null) return;
        ScheduledFuture<?> f = jobs.remove(id);
        if (f != null) f.cancel(false);
    }

    public void shutdown() {
        scheduler.shutdownNow();
    }
}