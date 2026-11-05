package com.server.server.services.common;

import java.util.ArrayList;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Runs the nightly jobs other services register. Services register {@code this::method}, which bypasses their
 * transactional proxy, so each job runs here in its own transaction: one failing job rolls back only itself.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SystemSchedulingService {

    private final TransactionTemplate transactionTemplate;
    private final List<Runnable> nightlyTasks = new ArrayList<>();

    public void registerNightlyTask(Runnable task) {
        this.nightlyTasks.add(task);
    }

    @Scheduled(cron = "0 0 0 * * *")
    public void runNightlyMaintenance() {
        log.info("Starting nightly maintenance: {} tasks", nightlyTasks.size());
        for (Runnable task : nightlyTasks) {
            try {
                transactionTemplate.executeWithoutResult(status -> task.run());
            } catch (Exception e) {
                log.error("Scheduled task failed", e);
            }
        }
        log.info("Nightly maintenance completed.");
    }
}
