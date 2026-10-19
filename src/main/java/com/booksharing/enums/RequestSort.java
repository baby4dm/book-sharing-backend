package com.booksharing.enums;

import org.springframework.data.domain.Sort;

public enum RequestSort {
    NEWEST(Sort.by(Sort.Direction.DESC, "createdAt")),
    OLDEST(Sort.by(Sort.Direction.ASC, "createdAt")),
    DEADLINE(Sort.by(Sort.Direction.ASC, "readingDays")
            .and(Sort.by(Sort.Direction.DESC, "createdAt"))),
    RATING(Sort.by(Sort.Direction.DESC, "requester.ratingAvg")
            .and(Sort.by(Sort.Direction.DESC, "createdAt")));

    private final Sort sort;

    RequestSort(Sort sort) {
        this.sort = sort;
    }

    public Sort toSort() {
        return sort;
    }
}