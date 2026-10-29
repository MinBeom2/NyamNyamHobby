package com.ssafy.nyamnyam.domain.recommendation.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;
import java.util.concurrent.*;

/** 수집 배치 스케줄러와 분리해 메시지 장애가 수집 작업을 지연시키지 않도록 한다. */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "recommendation", name = "reason-outbox-enabled", havingValue = "true", matchIfMissing = true)
public class RecommendationReasonScheduler implements SmartLifecycle {
    private final RecommendationReasonOutbox outbox;
    private ScheduledExecutorService executor;
    private volatile boolean running;
    @Override public synchronized void start() {
        if (running) return;
        executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "recommendation-reasons"); thread.setDaemon(true); return thread;
        });
        executor.scheduleWithFixedDelay(() -> {
            try { outbox.dispatchBatch(); } catch (Exception e) { log.warn("Reason outbox unavailable: {}", e.getClass().getSimpleName()); }
        }, 5, 5, TimeUnit.SECONDS);
        running = true;
    }
    @Override public synchronized void stop() { if (executor != null) executor.shutdownNow(); running = false; }
    @Override public boolean isRunning() { return running; }
}
