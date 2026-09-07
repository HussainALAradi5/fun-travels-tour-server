package com.server.server.services;

import java.util.ArrayList;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class SystemSchedulingService {

    // List of tasks to run during the nightly cleanup
    private final List<Runnable> nightlyTasks = new ArrayList<>();

    /**
     * Other services call this to register their cleanup logic.
     */
    public void registerNightlyTask(Runnable task) {
        this.nightlyTasks.add(task);
    }

    // Runs every day at midnight: 00:00:00
    @Scheduled(cron = "0 0 0 * * *")
    public void runNightlyMaintenance() {
        log.info("Starting Nightly Maintenance: Executing {} tasks", nightlyTasks.size());
        
        for (Runnable task : nightlyTasks) {
            try {
                task.run();
            } catch (Exception e) {
                log.error("Scheduled task failed: {}", e.getMessage());
            }
        }
        
        log.info("Nightly Maintenance completed.");
    }
}