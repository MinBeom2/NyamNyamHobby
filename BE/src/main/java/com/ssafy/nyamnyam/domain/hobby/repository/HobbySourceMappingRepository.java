package com.ssafy.nyamnyam.domain.hobby.repository;

import com.ssafy.nyamnyam.domain.hobby.entity.HobbySourceMapping;
import com.ssafy.nyamnyam.domain.hobby.entity.HobbyStatus;
import com.ssafy.nyamnyam.domain.hobby.entity.SourceTarget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface HobbySourceMappingRepository extends JpaRepository<HobbySourceMapping, Long> {

    /** 수집 서비스는 트랜잭션 밖에서 취미 이름을 쓰므로 취미를 함께 읽어 온다. */
    @Query("SELECT m FROM HobbySourceMapping m " +
           "JOIN FETCH m.hobby h " +
           "WHERE m.target = :target AND h.status = :hobbyStatus")
    List<HobbySourceMapping> findAllWithHobby(@Param("target") SourceTarget target,
                                              @Param("hobbyStatus") HobbyStatus hobbyStatus);

    @Query("SELECT m FROM HobbySourceMapping m " +
           "JOIN FETCH m.hobby h " +
           "WHERE m.hobby.id = :hobbyId AND m.target = :target")
    List<HobbySourceMapping> findByHobbyIdAndTarget(@Param("hobbyId") Long hobbyId,
                                                   @Param("target") SourceTarget target);
}
