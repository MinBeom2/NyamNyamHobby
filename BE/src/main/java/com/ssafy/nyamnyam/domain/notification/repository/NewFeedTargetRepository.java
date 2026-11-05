package com.ssafy.nyamnyam.domain.notification.repository;

import com.ssafy.nyamnyam.domain.feed.entity.FeedItem;
import com.ssafy.nyamnyam.domain.feed.entity.FeedStatus;
import com.ssafy.nyamnyam.domain.user.entity.UserHobbyStatus;
import com.ssafy.nyamnyam.domain.user.entity.UserStatus;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/** 새 피드 알림 대상 조회 전용(읽기만). feed·user 도메인의 저장소는 건드리지 않는다 */
public interface NewFeedTargetRepository extends Repository<FeedItem, Long> {

    /** 기간 안에 공개된 피드(취미가 붙은 것만, '그 외' 제외). 마감·종료 여부는 코드에서 거른다 */
    @Query("SELECT f FROM FeedItem f JOIN FETCH f.hobby "
            + "WHERE f.status = :status AND f.publishedAt >= :from AND f.publishedAt < :to")
    List<FeedItem> findPublishedBetween(@Param("status") FeedStatus status,
                                        @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    /** 이 취미들을 '내 취미'로 고른 활성 회원: [userId, hobbyId] */
    @Query("SELECT s.user.id, s.hobby.id FROM InterestSelection s "
            + "WHERE s.hobby.id IN :hobbyIds AND s.user.status = :userStatus")
    List<Object[]> findInterestOwners(@Param("hobbyIds") Collection<Long> hobbyIds,
                                      @Param("userStatus") UserStatus userStatus);

    /** 이 취미들을 관심 저장했거나 진행 중·일시중지인 활성 회원: [userId, hobbyId] */
    @Query("SELECT uh.user.id, uh.hobby.id FROM UserHobby uh "
            + "WHERE uh.hobby.id IN :hobbyIds AND uh.status IN :statuses AND uh.user.status = :userStatus")
    List<Object[]> findHobbyOwners(@Param("hobbyIds") Collection<Long> hobbyIds,
                                   @Param("statuses") Collection<UserHobbyStatus> statuses,
                                   @Param("userStatus") UserStatus userStatus);
}
