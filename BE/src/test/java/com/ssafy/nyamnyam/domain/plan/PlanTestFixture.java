package com.ssafy.nyamnyam.domain.plan;

import com.ssafy.nyamnyam.domain.hobby.entity.*;
import com.ssafy.nyamnyam.domain.plan.entity.*;
import com.ssafy.nyamnyam.domain.user.entity.*;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.LocalDateTime;

public final class PlanTestFixture {
    public static final LocalDateTime START = LocalDateTime.of(2026, 10, 1, 15, 0); // Oct 2, Seoul
    public static <T> T id(T value, long id) { ReflectionTestUtils.setField(value, "id", id); return value; }
    public static User user(long id) { return id(User.builder().email("todo" + id + "@test.com").nickname("todo" + id).build(), id); }
    public static Hobby hobby(long id) {
        return id(Hobby.builder().name("취미" + id).status(HobbyStatus.PUBLISHED).build(), id);
    }
    public static UserHobby entry(User user, Hobby hobby, long id) {
        var entry = id(UserHobby.builder().user(user).hobby(hobby).savedAt(START).build(), id);
        entry.start("BEGINNER", START);
        return entry;
    }
    public static PlanTemplate template(long hobbyId) {
        var template = id(new PlanTemplate(hobbyId, "BEGINNER", "입문 계획", (short) 3, 2, "PUBLISHED", START), 50);
        for (short week = 1; week <= 3; week++) {
            template.addWeek(week, "제목" + week);
            template.addItem(id(new PlanTemplateItem(template, week, (short) 1, "활동" + week,
                    "설명", (short) 30, "연필, 종이", "완료 기준"), 100 + week));
        }
        return template;
    }
    public static UserPlan plan(UserHobby entry) {
        var plan = id(new UserPlan(entry, template(entry.getHobby().getId()), START), 55);
        int seq = 901;
        for (var item : plan.getItems()) id(item, seq++);
        return plan;
    }
}
