package com.ssafy.nyamnyam.global.common;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/** 페이징 목록 응답(API 명세 1.4): { content, page: { number, size, totalElements, totalPages, hasNext } } */
public record PageResponse<T>(List<T> content, PageInfo page) {

    public record PageInfo(int number, int size, long totalElements, int totalPages, boolean hasNext) {
    }

    public static <E, T> PageResponse<T> of(Page<E> page, Function<E, T> mapper) {
        return new PageResponse<>(
                page.getContent().stream().map(mapper).toList(),
                new PageInfo(page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages(), page.hasNext())
        );
    }
}
