package com.logistics.service.impl;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/** page / size / sort ("field,asc|desc") from list endpoints. */
public final class PagingUtils {

    private PagingUtils() {
    }

    public static Pageable pageable(int page, int size, String sort, String defaultField) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        Sort s = Sort.by(Sort.Direction.DESC, defaultField);
        if (sort != null && !sort.isBlank()) {
            String[] parts = sort.split(",");
            String field = parts[0].trim();
            if (field.matches("[A-Za-z.]+")) {
                Sort.Direction dir = parts.length > 1 && parts[1].trim().equalsIgnoreCase("asc")
                        ? Sort.Direction.ASC : Sort.Direction.DESC;
                s = Sort.by(dir, field);
            }
        }
        return PageRequest.of(Math.max(page, 0), safeSize, s);
    }

    public static String like(String s) {
        return "%" + s.trim().toLowerCase() + "%";
    }
}
