package com.ssafy.nyamnyam.domain.recommendation;

import com.ssafy.nyamnyam.domain.exploration.entity.*;
import com.ssafy.nyamnyam.domain.hobby.entity.Hobby;
import com.ssafy.nyamnyam.domain.hobby.repository.HobbyDislikeRepository;
import com.ssafy.nyamnyam.domain.recommendation.service.*;
import com.ssafy.nyamnyam.domain.recommendation.entity.ReasonStatus;
import com.ssafy.nyamnyam.domain.shortform.entity.Short;
import com.ssafy.nyamnyam.domain.shortform.entity.ShortStatus;
import com.ssafy.nyamnyam.domain.user.entity.*;
import com.ssafy.nyamnyam.domain.user.repository.UserRepository;
import com.ssafy.nyamnyam.global.security.UserPrincipal;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@SpringBootTest(properties={"app.short-collect.enabled=false","recommendation.reason-outbox-enabled=false",
        "spring.rabbitmq.listener.simple.auto-startup=false"})
@AutoConfigureMockMvc
class RecommendationWorkflowIntegrationTest {
    @Autowired RecommendationService service;
    @Autowired RecommendationReasonOutbox outbox;
    @Autowired HobbyDislikeRepository dislikes;
    @Autowired UserRepository users;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager em;
    @Autowired PlatformTransactionManager manager;
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @MockitoBean RabbitTemplate rabbit;
    TransactionTemplate tx;
    Long owner, other, sessionId, hobbyId, runningId;
    List<Long> shortIds;

    @BeforeEach void setup() {
        tx = new TransactionTemplate(manager);
        tx.executeWithoutResult(status -> {
            String token = UUID.randomUUID().toString().substring(0,8);
            User member = User.builder().email(token+"@workflow.test").nickname("a"+token).build(); em.persist(member); owner=member.getId();
            User foreign = User.builder().email(token+"b@workflow.test").nickname("b"+token).build(); em.persist(foreign); other=foreign.getId();
            List<Long> ids = jdbc.queryForList("SELECT id FROM hobbies WHERE status='PUBLISHED' ORDER BY id LIMIT 2",Long.class);
            hobbyId=ids.get(0); runningId=ids.get(1);
            Hobby hobby=em.getReference(Hobby.class,hobbyId);
            ExplorationSession past=ExplorationSession.builder().user(member).build(); em.persist(past);
            ExplorationSession current=ExplorationSession.builder().user(member).build(); em.persist(current); sessionId=current.getId();
            shortIds=new ArrayList<>();
            for(int i=0;i<3;i++) {
                Short video=Short.builder().hobby(hobby).youtubeVideoId(token+i).title("test "+i).durationSeconds((short)60).status(ShortStatus.ACTIVE).build(); em.persist(video); shortIds.add(video.getId());
                ExplorationSession session=i<2?past:current; session.incrementViewedCount();
                em.persist(ExplorationView.builder().session(session).shortform(video).seq((short)(i<2?i+1:1))
                        .watchRatio(i<2?BigDecimal.ONE:new BigDecimal("0.75")).build());
                em.persist(ShortReaction.builder().user(member).sessionId(session.getId()).shortform(video).type(ReactionType.LIKE).build());
            }
            past.complete(); current.incrementViewedCount();
            em.persist(ExplorationView.builder().session(current).shortform(em.getReference(Short.class,shortIds.get(0)))
                    .seq((short)2).watchRatio(new BigDecimal("0.25")).build());
            em.persist(UserHobby.builder().user(member).hobby(hobby).status(UserHobbyStatus.SAVED).build());
        });
    }
    @AfterEach void cleanup() {
        if(owner==null)return;
        tx.executeWithoutResult(status -> {
            for(Long id:List.of(owner,other)) {
                jdbc.update("DELETE FROM recommendation_reason_outbox WHERE recommendation_id IN (SELECT id FROM recommendations WHERE user_id=?)",id);
                jdbc.update("DELETE FROM recommendation_items WHERE recommendation_id IN (SELECT id FROM recommendations WHERE user_id=?)",id);
                jdbc.update("DELETE FROM recommendations WHERE user_id=?",id);
                jdbc.update("DELETE FROM hobby_dislikes WHERE user_id=?",id);
                jdbc.update("DELETE FROM short_reactions WHERE user_id=?",id);
                jdbc.update("DELETE FROM user_conditions WHERE user_id=?",id);
                jdbc.update("DELETE FROM user_hobbies WHERE user_id=?",id);
                jdbc.update("DELETE FROM exploration_views WHERE session_id IN (SELECT id FROM exploration_sessions WHERE user_id=?)",id);
                jdbc.update("DELETE FROM exploration_sessions WHERE user_id=?",id);
                jdbc.update("DELETE FROM users WHERE id=?",id);
            }
            for(Long id:shortIds) jdbc.update("DELETE FROM shorts WHERE id=?",id);
        });
    }
    @Test void canonicalApiUsesCumulativeDeduplicatedHistoryAndSharesFinishIdempotency() throws Exception {
        String body="{\"basis\":\"EXPLORATION_SESSION\",\"sessionId\":"+sessionId+"}";
        String json=mvc.perform(post("/api/v1/recommendations").with(user(principal(owner))).contentType(APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.data.isExploratory").value(false))
                .andExpect(jsonPath("$.data.basis").value("EXPLORATION_SESSION"))
                .andExpect(jsonPath("$.data.items[0].hobby.id").value(hobbyId))
                .andExpect(jsonPath("$.data.items[0].hobby.sessionMinutes.min").isNumber())
                .andExpect(jsonPath("$.data.items[0].hobby.monthlyCost.min").isNumber())
                .andExpect(jsonPath("$.data.items[0].saved").value(true))
                .andExpect(jsonPath("$.data.items[0].rank").value(1))
                .andExpect(jsonPath("$.data.items[0].hobbyName").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        long recommendationId=mapper.readTree(json).get("data").get("recommendationId").asLong();
        assertThat(jdbc.queryForObject("SELECT score FROM recommendation_items WHERE recommendation_id=? AND hobby_id=?",BigDecimal.class,recommendationId,hobbyId)).isEqualByComparingTo("10.000");
        mvc.perform(post("/api/v1/explorations/"+sessionId+"/finish").with(user(principal(owner))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.recommendationId").value(recommendationId));
        mvc.perform(get("/api/v1/recommendations/"+recommendationId).with(user(principal(owner))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.items[0].fallbackReason").isNotEmpty());
        mvc.perform(get("/api/v1/recommendations/"+recommendationId).with(user(principal(other)))).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/recommendations/"+recommendationId)).andExpect(status().isUnauthorized());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM recommendation_reason_outbox WHERE recommendation_id=?",Long.class,recommendationId)).isEqualTo(1);
        verifyNoInteractions(rabbit);
    }
    @Test void dislikePutDeleteAreIdempotentPersonalAndAffectOnlyNewRecommendations() throws Exception {
        for(int i=0;i<2;i++) mvc.perform(put("/api/v1/hobbies/"+hobbyId+"/dislike").with(user(principal(owner))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.disliked").value(true));
        assertThat(dislikes.findIds(owner)).containsExactly(hobbyId); assertThat(dislikes.findIds(other)).isEmpty();
        var excluded=service.finishExploration(sessionId,member(owner)).response();
        assertThat(excluded.getItems()).extracting(i->i.getHobby().id()).doesNotContain(hobbyId);
        for(int i=0;i<2;i++) mvc.perform(delete("/api/v1/hobbies/"+hobbyId+"/dislike").with(user(principal(owner)))).andExpect(status().isOk());
        long next=newSession(owner);
        assertThat(service.finishExploration(next,member(owner)).response().getItems()).extracting(i->i.getHobby().id()).contains(hobbyId);
        assertThat(service.getRecommendation(excluded.getRecommendationId(),member(owner)).getItems()).extracting(i->i.getHobby().id()).doesNotContain(hobbyId);
    }
    @Test void snapshotsPreserveHobbyConditionsAndSavedStateAfterEdits() {
        jdbc.update("INSERT INTO user_conditions(user_id,weekly_hours,monthly_budget,place_pref,social_pref,updated_at) VALUES (?,0,0,'INDOOR','ALONE',CURRENT_TIMESTAMP)",owner);
        var result=service.finishExploration(sessionId,member(owner)).response();
        var original=result.getItems().get(0).getHobby();
        assertThat(result.getItems().get(0).getConditionWarning()).isTrue();
        String oldName=original.name(); int oldCost=original.monthlyCost().min();
        try {
            jdbc.update("UPDATE hobbies SET name=?,monthly_cost_min=? WHERE id=?","test-"+UUID.randomUUID().toString().substring(0,8),oldCost+1,hobbyId);
            jdbc.update("DELETE FROM user_hobbies WHERE user_id=?",owner);
            jdbc.update("UPDATE user_conditions SET weekly_hours=40,monthly_budget=10000000 WHERE user_id=?",owner);
            var reread=service.getRecommendation(result.getRecommendationId(),member(owner));
            assertThat(reread.getItems().get(0).getHobby()).isEqualTo(original);
            assertThat(reread.getItems().get(0).getSaved()).isTrue();
            assertThat(reread.getItems().get(0).getConditionWarning()).isTrue();
        } finally { jdbc.update("UPDATE hobbies SET name=?,monthly_cost_min=? WHERE id=?",oldName,oldCost,hobbyId); }
    }
    @Test void publicRelatedShortsValidateLimitAndExcludeInactiveVideos() throws Exception {
        jdbc.update("UPDATE shorts SET status='REJECTED' WHERE id=?",shortIds.get(0));
        mvc.perform(get("/api/v1/hobbies/"+hobbyId+"/shorts")).andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(2));
        mvc.perform(get("/api/v1/hobbies/"+hobbyId+"/shorts?limit=1")).andExpect(status().isOk()).andExpect(jsonPath("$.data[0].shortId").value(shortIds.get(2)));
        mvc.perform(get("/api/v1/hobbies/"+hobbyId+"/shorts?limit=7")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/hobbies/9223372036854775807/shorts")).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/hobbies/"+runningId+"/shorts")).andExpect(status().isOk()).andExpect(jsonPath("$.data").isEmpty());
        mvc.perform(put("/api/v1/hobbies/"+hobbyId+"/dislike")).andExpect(status().isUnauthorized());
    }
    @Test void rollbackLeavesNoRecommendationOrOutboxAndPublishFailureUsesFallback() {
        assertThatThrownBy(()->tx.executeWithoutResult(status->{service.finishExploration(sessionId,member(owner));throw new IllegalStateException("rollback");})).isInstanceOf(IllegalStateException.class);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM recommendations WHERE user_id=?",Long.class,owner)).isZero();
        verifyNoInteractions(rabbit);
        var result=service.finishExploration(sessionId,member(owner)).response();
        for(int i=0;i<5;i++) {
            jdbc.update("UPDATE recommendation_reason_outbox SET next_attempt_at=CURRENT_TIMESTAMP WHERE recommendation_id=?",result.getRecommendationId());
            outbox.dispatchBatch();
        }
        var failed=service.getRecommendation(result.getRecommendationId(),member(owner));
        assertThat(failed.getItems()).allSatisfy(item->{assertThat(item.getReasonStatus()).isEqualTo(ReasonStatus.FALLBACK);assertThat(item.getFallbackReason()).isNotBlank();});
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM recommendation_reason_outbox WHERE recommendation_id=?",Long.class,result.getRecommendationId())).isZero();
    }
    @Test void invalidCreationAndForeignSessionAreRejected() throws Exception {
        mvc.perform(post("/api/v1/recommendations").with(user(principal(owner))).contentType(APPLICATION_JSON)
                .content("{\"basis\":\"DIRECT_SELECTION\",\"sessionId\":"+sessionId+"}")).andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/recommendations").with(user(principal(other))).contentType(APPLICATION_JSON)
                .content("{\"basis\":\"EXPLORATION_SESSION\",\"sessionId\":"+sessionId+"}")).andExpect(status().isNotFound());
    }
    @Test void exploratorySuggestionsAreCategoryDiverseAndExcludeRunningHobbies() {
        tx.executeWithoutResult(status->em.persist(UserHobby.builder().user(em.getReference(User.class,other))
                .hobby(em.getReference(Hobby.class,runningId)).status(UserHobbyStatus.IN_PROGRESS).build()));
        var result=service.finishExploration(newSession(other),member(other)).response();
        assertThat(result.getIsExploratory()).isTrue();
        assertThat(result.getItems()).extracting(i->i.getHobby().id()).doesNotContain(runningId);
        Set<Long> categories=new HashSet<>();
        result.getItems().forEach(i->assertThat(categories.add(jdbc.queryForObject("SELECT category_id FROM hobbies WHERE id=?",Long.class,i.getHobby().id()))).isTrue());
    }
    private User member(Long id){return users.findById(id).orElseThrow();}
    private UserPrincipal principal(Long id){return new UserPrincipal(id,"test@example.com","USER");}
    private long newSession(Long id){return tx.execute(status->{ExplorationSession s=ExplorationSession.builder().user(em.getReference(User.class,id)).build();em.persist(s);return s.getId();});}
}
