package com.example.blog.article;

public class ArchiveMonthResponse {

    private final String yearMonth;
    private final long count;

    public ArchiveMonthResponse(String yearMonth, long count) {
        this.yearMonth = yearMonth;
        this.count = count;
    }

    public String getYearMonth() {
        return yearMonth;
    }

    public long getCount() {
        return count;
    }
}
