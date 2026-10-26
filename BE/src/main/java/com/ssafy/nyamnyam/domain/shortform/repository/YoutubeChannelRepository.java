package com.ssafy.nyamnyam.domain.shortform.repository;

import com.ssafy.nyamnyam.domain.shortform.entity.YoutubeChannel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface YoutubeChannelRepository extends JpaRepository<YoutubeChannel, Long> {

    List<YoutubeChannel> findByYoutubeChannelIdIn(Collection<String> youtubeChannelIds);

    /** 주어진 채널 중 특정 상태(예: 차단 REJECTED)인 채널의 YouTube 채널 ID */
    @Query("SELECT c.youtubeChannelId FROM YoutubeChannel c " +
           "WHERE c.youtubeChannelId IN :ids AND c.status = :status")
    List<String> findChannelIdsByStatus(@Param("ids") Collection<String> youtubeChannelIds,
                                        @Param("status") String status);
}
