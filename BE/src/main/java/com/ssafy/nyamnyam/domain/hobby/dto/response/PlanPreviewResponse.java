package com.ssafy.nyamnyam.domain.hobby.dto.response;

import com.ssafy.nyamnyam.domain.plan.entity.PlanTemplate;
import com.ssafy.nyamnyam.domain.plan.entity.PlanTemplateItem;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 취미 TODO 플랜 미리보기 응답(API 명세 3.4-4). 상세 화면 하단 "TODO 미리보기"와 숙련도 선택 모달이 쓴다.
 * 공개 원본이 없으면 ready=false에 weeks는 빈 배열이고, 프론트는 "준비 중인 계획이에요"를 보여 준다(FR-MY-02).
 */
public record PlanPreviewResponse(
        boolean ready,
        String skillLevel,
        String title,
        int totalWeeks,
        int previewWeeks,
        List<Week> weeks
) {
    public record Week(int weekNo, String title, List<Item> items) {
    }

    /** 원본 활동 1개. 사용자 계획이 아니라 상태·메모는 없다 */
    public record Item(int seq, String title, String description, int expectedMinutes,
                       String supplies, String doneCriteria) {
        static Item from(PlanTemplateItem item) {
            return new Item(item.getSeq(), item.getTitle(), item.getDescription(),
                    item.getExpectedMinutes(), item.getSupplies(), item.getDoneCriteria());
        }
    }

    public static PlanPreviewResponse notReady(String skillLevel) {
        return new PlanPreviewResponse(false, skillLevel, null, 0, 0, List.of());
    }

    /** 1주차부터 previewWeeks개(총 주차를 넘지 않음)만 담는다 */
    public static PlanPreviewResponse of(PlanTemplate template, int previewWeeks) {
        int weekCount = Math.min(previewWeeks, template.getTotalWeeks());
        List<Week> weeks = new ArrayList<>();
        for (int weekNo = 1; weekNo <= weekCount; weekNo++) {
            final short no = (short) weekNo;
            List<Item> items = template.getItems().stream()
                    .filter(i -> i.getWeekNo() == no)
                    .sorted(Comparator.comparingInt(PlanTemplateItem::getSeq).thenComparing(PlanTemplateItem::getId,
                            Comparator.nullsLast(Comparator.naturalOrder())))
                    .map(Item::from)
                    .toList();
            String title = template.getWeekTitles().getOrDefault(no, weekNo + "주차");
            weeks.add(new Week(weekNo, title, items));
        }
        return new PlanPreviewResponse(true, template.getSkillLevel(), template.getTitle(),
                template.getTotalWeeks(), weekCount, weeks);
    }
}
