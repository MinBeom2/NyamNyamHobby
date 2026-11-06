package com.ssafy.nyamnyam.domain.feed.collect;

import com.ssafy.nyamnyam.domain.feed.entity.FeedItem;
import com.ssafy.nyamnyam.domain.feed.entity.FeedType;
import com.ssafy.nyamnyam.domain.feed.repository.FeedItemRepository;
import com.ssafy.nyamnyam.domain.hobby.entity.Hobby;
import com.ssafy.nyamnyam.domain.hobby.entity.HobbyStatus;
import com.ssafy.nyamnyam.domain.hobby.repository.HobbyRepository;
import com.ssafy.nyamnyam.global.batchrun.BatchJob;
import com.ssafy.nyamnyam.global.batchrun.BatchRun;
import com.ssafy.nyamnyam.global.batchrun.BatchRunRepository;
import com.ssafy.nyamnyam.global.batchrun.BatchRunStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * FEED-B04 정보 피드 취미 분류(매일 06:00, 수집이 끝난 뒤). 검색어로 붙인 후보 취미를 GMS(gpt-4o-mini)가 확인해
 * 맞으면 그대로, 틀리면 다른 취미나 '그 외'(hobby 없음)로 바꿔 공개한다. 요약은 만들지 않는다(토큰 절약).
 * 하루 처리 건수에 상한을 두고, 남은 항목은 다음 날 이어서 처리한다.
 * GMS 키가 없거나 답을 못 받으면 시도 횟수만 올리고, 여러 번 실패하면 후보(없으면 '그 외')로 확정한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeedClassifyService {

    private final FeedItemRepository feedItemRepository;
    private final HobbyRepository hobbyRepository;
    private final FeedHobbyClassifier classifier;
    private final FeedWriter feedWriter;
    private final BatchRunRepository batchRunRepository;
    private final FeedClassifyProperties properties;

    private final AtomicBoolean running = new AtomicBoolean(false);

    public BatchRun classify() {
        if (!running.compareAndSet(false, true)) {
            log.warn("[FEED_CLASSIFY] previous run still in progress, skipped");
            return null;
        }
        BatchRun run = batchRunRepository.save(BatchRun.start(BatchJob.FEED_CLASSIFY));
        int processed = 0, matched = 0, other = 0, failed = 0, calls = 0;
        String fatal = null;
        try {
            if (!classifier.isConfigured()) {
                fatal = "GMS 키 없음";
            } else {
                List<FeedItem> targets = feedItemRepository.findClassifyTargets(PageRequest.of(0, properties.dailyLimit()));
                List<Hobby> hobbies = hobbyRepository.findByStatus(HobbyStatus.PUBLISHED).stream()
                        .sorted(Comparator.comparing(Hobby::getId)).toList();
                Map<String, Hobby> byName = hobbies.stream()
                        .collect(Collectors.toMap(Hobby::getName, Function.identity(), (a, b) -> a));
                List<String> names = hobbies.stream().map(Hobby::getName).toList();
                int size = Math.max(1, properties.chunkSize());
                for (int from = 0; from < targets.size(); from += size) {
                    List<FeedItem> chunk = targets.subList(from, Math.min(from + size, targets.size()));
                    calls++;
                    Map<Long, String> answers = classifier.classify(names, chunk.stream().map(FeedClassifyService::toTarget).toList());
                    for (FeedItem item : chunk) {
                        processed++;
                        String answer = answers.get(item.getId());
                        boolean answered = answer != null;
                        Hobby hobby = answered ? byName.get(answer) : null; // '그 외'는 null
                        feedWriter.applyClassification(item.getId(), answered, hobby, properties.maxAttempts());
                        if (!answered) failed++;
                        else if (hobby == null) other++;
                        else matched++;
                    }
                }
            }
        } catch (RuntimeException e) {
            log.error("[FEED_CLASSIFY] unexpected failure", e);
            fatal = e.getClass().getSimpleName() + ": " + e.getMessage();
        } finally {
            BatchRunStatus status = fatal != null ? BatchRunStatus.FAILED
                    : failed == 0 ? BatchRunStatus.SUCCESS
                    : failed == processed ? BatchRunStatus.FAILED : BatchRunStatus.PARTIAL;
            String message = (fatal != null ? fatal + ", " : "") + "취미 " + matched + "건, 그 외 " + other + "건, 답 없음 " + failed + "건";
            run.finish(status, processed, matched + other, failed, calls, message);
            batchRunRepository.save(run);
            running.set(false);
            log.info("[FEED_CLASSIFY] {} processed={} calls={} {}", run.getStatus(), processed, calls, message);
        }
        return run;
    }

    private static FeedHobbyClassifier.Target toTarget(FeedItem item) {
        return new FeedHobbyClassifier.Target(item.getId(), item.getTitle(), typeLabel(item.getType()),
                item.getSourceCategory(), item.getHobby() == null ? null : item.getHobby().getName());
    }

    private static String typeLabel(FeedType type) {
        return switch (type) {
            case EVENT -> "축제·행사";
            case SHOW -> "공연·전시";
            case CLASS -> "강좌·클래스";
            case GUIDE -> "입문 자료";
        };
    }
}
