package com.ssafy.nyamnyam.domain.user.repository;

import com.ssafy.nyamnyam.domain.user.entity.UserHobby;
import com.ssafy.nyamnyam.domain.user.entity.UserHobbyStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserHobbyRepository extends JpaRepository<UserHobby, Long> {

    Optional<UserHobby> findByUserIdAndHobbyId(Long userId, Long hobbyId);

    List<UserHobby> findByUserId(Long userId);

    List<UserHobby> findByUserIdAndStatus(Long userId, UserHobbyStatus status);

    long countByUserIdAndStatus(Long userId, UserHobbyStatus status);

    long countByUserIdAndStatusIn(Long userId, Collection<UserHobbyStatus> statuses);

    @Query("SELECT uh FROM UserHobby uh JOIN FETCH uh.hobby WHERE uh.user.id = :userId ORDER BY uh.savedAt DESC, uh.id DESC")
    List<UserHobby> findAllByUserIdWithHobby(@Param("userId") Long userId);

    @Query("SELECT uh FROM UserHobby uh JOIN FETCH uh.hobby WHERE uh.user.id = :userId AND uh.status = :status ORDER BY uh.savedAt DESC, uh.id DESC")
    List<UserHobby> findAllByUserIdAndStatusWithHobby(@Param("userId") Long userId, @Param("status") UserHobbyStatus status);
}
