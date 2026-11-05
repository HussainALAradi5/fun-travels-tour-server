package com.server.server.services.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

class SystemSchedulingServiceTest {

    @Test
    void eachTaskRunsInItsOwnTransactionAndFailuresDoNotStopTheRest() {
        PlatformTransactionManager manager = mock(PlatformTransactionManager.class);
        SystemSchedulingService service = new SystemSchedulingService(new TransactionTemplate(manager));
        List<String> ran = new ArrayList<>();
        service.registerNightlyTask(() -> ran.add("first"));
        service.registerNightlyTask(() -> {
            throw new IllegalStateException("boom");
        });
        service.registerNightlyTask(() -> ran.add("third"));

        service.runNightlyMaintenance();

        assertThat(ran).containsExactly("first", "third");
        verify(manager, times(3)).getTransaction(any());
        verify(manager, times(2)).commit(any());
        verify(manager, times(1)).rollback(any());
        assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
    }
}
