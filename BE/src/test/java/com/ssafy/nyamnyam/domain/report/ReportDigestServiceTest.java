package com.ssafy.nyamnyam.domain.report;

import com.ssafy.nyamnyam.domain.report.entity.ReportTargetType;
import com.ssafy.nyamnyam.domain.report.service.ReportDigestService;
import com.ssafy.nyamnyam.domain.report.service.ReportDigestStore;
import com.ssafy.nyamnyam.domain.report.service.ReportDigestStore.Batch;
import com.ssafy.nyamnyam.domain.report.service.ReportDigestStore.Item;
import com.ssafy.nyamnyam.global.infra.mattermost.MattermostMessage;
import com.ssafy.nyamnyam.global.infra.mattermost.MattermostMessage.Attachment;
import com.ssafy.nyamnyam.global.infra.mattermost.MattermostWebhookClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class ReportDigestServiceTest {

    private static final String HOOK = "https://mm/hooks/x";

    @Mock private ReportDigestStore store;
    @Mock private MattermostWebhookClient client;

    private ReportDigestService service(String webhook) {
        return new ReportDigestService(new ReportProperties(3, 30, false, webhook, "s", "https://a", "https://b"), store, client);
    }

    private static Attachment attachment(String title) {
        return new Attachment(title, "#000", title, null, "", List.of(), null);
    }

    @Test
    @DisplayName("머리말 + 대상마다 메시지 한 건, 보낸 신고만 전송 표시")
    void sendsHeaderAndOneMessagePerTarget() {
        Item post = new Item(ReportTargetType.POST, 12L, List.of(1L, 2L), attachment("게시글 #12"), true);
        Item comment = new Item(ReportTargetType.COMMENT, 7L, List.of(3L), attachment("댓글 #7"), false);
        Item processed = new Item(ReportTargetType.POST, 13L, List.of(4L), null, false);
        given(store.loadPending(30)).willReturn(new Batch(List.of(post, comment, processed), 4, false));
        given(client.send(eq(HOOK), any())).willReturn(true);

        int notified = service(HOOK).sendDigest();

        ArgumentCaptor<MattermostMessage> sent = ArgumentCaptor.forClass(MattermostMessage.class);
        verify(client, times(3)).send(eq(HOOK), sent.capture());
        assertThat(sent.getAllValues().get(0).text()).contains("새 신고 4건 · 대상 2개 (자동 숨김 1개)");
        assertThat(sent.getAllValues().get(1).attachments()).extracting(Attachment::title).containsExactly("게시글 #12");
        assertThat(sent.getAllValues().get(2).attachments()).extracting(Attachment::title).containsExactly("댓글 #7");
        verify(store).markNotified(List.of(1L, 2L));
        verify(store).markNotified(List.of(3L));
        verify(store).markNotified(List.of(4L)); // 이미 처리된 대상은 보내지 않고 표시만
        assertThat(notified).isEqualTo(4);
    }

    @Test
    @DisplayName("전송이 실패하면 그 뒤는 표시하지 않아 다음 시간에 다시 보낸다")
    void stopsOnFailure() {
        Item first = new Item(ReportTargetType.POST, 12L, List.of(1L), attachment("a"), false);
        Item second = new Item(ReportTargetType.POST, 13L, List.of(2L), attachment("b"), false);
        given(store.loadPending(30)).willReturn(new Batch(List.of(first, second), 2, true));
        given(client.send(eq(HOOK), any())).willReturn(true, true, false);

        int notified = service(HOOK).sendDigest();

        verify(store).markNotified(List.of(1L));
        verify(store, never()).markNotified(List.of(2L));
        assertThat(notified).isEqualTo(1);
    }

    @Test
    @DisplayName("머리말 전송이 실패하면 아무것도 표시하지 않는다")
    void headerFailure() {
        given(store.loadPending(30)).willReturn(new Batch(
                List.of(new Item(ReportTargetType.POST, 12L, List.of(1L), attachment("a"), false)), 1, false));
        given(client.send(eq(HOOK), any())).willReturn(false);

        assertThat(service(HOOK).sendDigest()).isZero();
        verify(store, never()).markNotified(any());
    }

    @Test
    @DisplayName("웹훅 URL이 없거나 새 신고가 없으면 보내지 않는다")
    void nothingToSend() {
        assertThat(service("").sendDigest()).isZero();
        verifyNoInteractions(store, client);

        given(store.loadPending(anyInt())).willReturn(new Batch(List.of(), 0, false));
        assertThat(service(HOOK).sendDigest()).isZero();
        verifyNoInteractions(client);
    }
}
