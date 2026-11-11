package com.moa.common.dto.response;

import org.springframework.data.domain.Page;

import java.util.List;

// 페이징 응답 공통 형식 (content, page, size, totalElements)
// Page 객체를 그대로 응답하면 내부 필드(pageable, sort 등)까지 다 노출돼서 명세에 있는 값만 담는다
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements
) {
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements()
        );
    }
}