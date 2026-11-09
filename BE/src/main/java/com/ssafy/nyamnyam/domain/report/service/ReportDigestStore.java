package com.ssafy.nyamnyam.domain.report.service;

import com.ssafy.nyamnyam.domain.report.entity.Report;
import com.ssafy.nyamnyam.domain.report.entity.ReportStatus;
import com.ssafy.nyamnyam.domain.report.entity.ReportTargetType;
import com.ssafy.nyamnyam.domain.report.moderation.ModerationMessageFactory;
import com.ssafy.nyamnyam.domain.report.repository.ReportRepository;
import com.ssafy.nyamnyam.domain.report.service.ModerationTargets.State;
import com.ssafy.nyamnyam.domain.report.service.ModerationTargets.Target;
import com.ssafy.nyamnyam.global.infra.mattermost.MattermostMessage.Attachment;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 정기 알림의 DB 작업. 웹훅 전송(네트워크)은 트랜잭션 밖에서 하도록 읽기와 표시를 나눴다.
 * <p>
 * 신고 커밋 직후(AFTER_COMMIT) 리스너에서 불려도 앞 트랜잭션에 섞이지 않게 항상 새 트랜잭션을 연다.
 * 섞이면 notified_at 표시가 커밋되지 않아 같은 신고를 다음에 또 보낸다.
 */
@Component
@RequiredArgsConstructor
public class ReportDigestStore {

    /** 한 번에 읽을 미전송 신고 수(대상으로 묶기 전) */
    static final int SCAN_LIMIT = 1000;

    private final ReportRepository reportRepository;
    private final ModerationTargets targets;
    private final ModerationMessageFactory messageFactory;

    /**
     * 대상 하나 분량의 알림.
     *
     * @param reportIds  이번에 처음 알리는 신고(전송 성공 후 notified_at을 채운다)
     * @param attachment 보낼 메시지. 처리 전 신고가 없으면(이미 처리됨) null이라 보내지 않고 표시만 한다
     */
    public record Item(ReportTargetType type, Long id, List<Long> reportIds, Attachment attachment, boolean hidden) {
    }

    public record Batch(List<Item> items, int newReports, boolean hasMore) {
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public Batch loadPending(int maxTargets) {
        List<Report> unnotified = reportRepository.findUnnotified(PageRequest.of(0, SCAN_LIMIT));

        Map<String, List<Report>> byTarget = new LinkedHashMap<>();
        for (Report report : unnotified) {
            byTarget.computeIfAbsent(report.getTargetType() + ":" + report.getTargetId(), k -> new ArrayList<>()).add(report);
        }

        List<Item> items = new ArrayList<>();
        int newReports = 0;
        for (List<Report> group : byTarget.values()) {
            if (items.size() >= maxTargets) {
                break;
            }
            Report first = group.get(0);
            ReportTargetType type = first.getTargetType();
            Long id = first.getTargetId();
            Target target = targets.find(type, id).orElse(null);
            List<Report> received = reportRepository.findByTargetTypeAndTargetIdAndStatusOrderByIdAsc(type, id, ReportStatus.RECEIVED);

            Attachment attachment = received.isEmpty() ? null : messageFactory.pending(type, id, target, received);
            boolean hidden = target != null && target.state() == State.HIDDEN;
            items.add(new Item(type, id, group.stream().map(Report::getId).toList(), attachment, hidden));
            newReports += group.size();
        }
        boolean hasMore = byTarget.size() > items.size() || unnotified.size() >= SCAN_LIMIT;
        return new Batch(items, newReports, hasMore);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markNotified(Collection<Long> reportIds) {
        if (!reportIds.isEmpty()) {
            reportRepository.markNotified(reportIds, LocalDateTime.now());
        }
    }
}
