package com.ssafy.nyamnyam.domain.report.service;

import com.ssafy.nyamnyam.domain.report.ReportProperties;
import com.ssafy.nyamnyam.domain.report.service.ReportDigestStore.Batch;
import com.ssafy.nyamnyam.domain.report.service.ReportDigestStore.Item;
import com.ssafy.nyamnyam.global.infra.mattermost.MattermostMessage;
import com.ssafy.nyamnyam.global.infra.mattermost.MattermostWebhookClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 매시 정각 신고 알림. 머리말 한 건 + 대상마다 메시지 한 건(버튼으로 처리하면 그 메시지만 바뀌도록 대상별로 나눴다).
 * 보낸 신고만 notified_at을 채우므로, 전송이 실패하면 다음 시간에 이어서 보낸다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportDigestService {

    static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("MM/dd HH:mm");

    private final ReportProperties properties;
    private final ReportDigestStore store;
    private final MattermostWebhookClient webhookClient;

    /**
     * 스케줄러와 즉시 알림이 겹쳐도 같은 신고를 두 번 보내지 않도록 한 번에 하나씩 보낸다(서버 한 대 기준).
     * @return 이번에 알린 신고 수
     */
    public synchronized int sendDigest() {
        if (!properties.notifyEnabled()) {
            return 0;
        }
        Batch batch = store.loadPending(properties.maxTargetsPerDigest());
        if (batch.items().isEmpty()) {
            return 0;
        }

        List<Item> toSend = batch.items().stream().filter(item -> item.attachment() != null).toList();
        if (!toSend.isEmpty() && !webhookClient.send(properties.mattermostWebhookUrl(), header(batch, toSend))) {
            return 0;
        }

        int notified = 0;
        for (Item item : batch.items()) {
            if (item.attachment() != null
                    && !webhookClient.send(properties.mattermostWebhookUrl(), new MattermostMessage(null, List.of(item.attachment())))) {
                log.warn("Report digest stopped: send failed at {} #{}", item.type(), item.id());
                break;
            }
            store.markNotified(item.reportIds());
            notified += item.reportIds().size();
        }
        log.info("Report digest sent: {} reports, {} targets", notified, toSend.size());
        return notified;
    }

    static MattermostMessage header(Batch batch, List<Item> toSend) {
        long hidden = toSend.stream().filter(Item::hidden).count();
        StringBuilder text = new StringBuilder()
                .append("#### 신고 알림 · ").append(ZonedDateTime.now(KST).format(TIME)).append('\n')
                .append("새 신고 ").append(batch.newReports()).append("건 · 대상 ").append(toSend.size()).append("개");
        if (hidden > 0) {
            text.append(" (자동 숨김 ").append(hidden).append("개)");
        }
        if (batch.hasMore()) {
            text.append("\n남은 신고는 다음 알림에서 이어서 보내요.");
        }
        return new MattermostMessage(text.toString(), null);
    }
}
