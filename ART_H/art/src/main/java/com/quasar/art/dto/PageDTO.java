package com.quasar.art.dto;

import lombok.Data;
import java.util.List;

@Data
public class PageDTO<T> {
    private List<T> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean hasNext;
    private boolean hasPrevious;

    public static <T> PageDTO<T> of(List<T> content, int page, int size, long totalElements, int totalPages) {
        PageDTO<T> dto = new PageDTO<>();
        dto.setContent(content);
        dto.setPage(page);
        dto.setSize(size);
        dto.setTotalElements(totalElements);
        dto.setTotalPages(totalPages);
        dto.setHasNext(page < totalPages - 1);
        dto.setHasPrevious(page > 0);
        return dto;
    }
}