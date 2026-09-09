package com.omraty.backend.scheduler;

import com.omraty.backend.repository.AuthRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Deletes dead refresh tokens (expired or revoked) on a daily schedule so the refresh_tokens table
 * does not grow indefinitely.
 */
@Component
public class RefreshTokenCleanupTask {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenCleanupTask.class);

    private final AuthRepository authRepository;

    public RefreshTokenCleanupTask(AuthRepository authRepository) {
        this.authRepository = authRepository;
    }

    @Scheduled(cron = "0 0 3 * * *")
    public void cleanupExpiredAndRevokedTokens() {
        log.info("Starting refresh_tokens cleanup task");
        int deletedCount = authRepository.deleteExpiredOrRevokedRefreshTokens();
        log.info("Finished refresh_tokens cleanup task: {} token(s) deleted", deletedCount);
    }
}
