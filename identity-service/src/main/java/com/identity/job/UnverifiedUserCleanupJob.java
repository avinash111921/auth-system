package com.identity.job;

import com.identity.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class UnverifiedUserCleanupJob {

    private final UserRepository userRepository;

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void cleanUpUnverifiedUsers() {
        Instant cutoffTime= Instant.now().minus(24, ChronoUnit.HOURS);
        log.info("Starting background cleanup for unverified users created before: {}", cutoffTime);
        int deletedCount = userRepository.deleteByVerifiedFalseAndCreatedAtBefore(cutoffTime);
        log.info("Cleanup successful. Purged {} abandoned registration records from the database.", deletedCount);
    }
}