package com.ssafy.nyamnyam.domain.post.repository;

import com.ssafy.nyamnyam.domain.post.entity.PostImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface PostImageRepository extends JpaRepository<PostImage, Long> {

    /** 목록 썸네일(각 글의 첫 이미지). 결과 행은 [postId(Long), url(String)] */
    @Query("SELECT i.post.id, i.url FROM PostImage i WHERE i.post.id IN :postIds AND i.seq = 0")
    List<Object[]> findThumbnails(@Param("postIds") Collection<Long> postIds);
}
