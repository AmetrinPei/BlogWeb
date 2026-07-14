package com.example.blog.like;

public class LikeToggleResponse {

    private final boolean liked;
    private final long count;

    public LikeToggleResponse(boolean liked, long count) {
        this.liked = liked;
        this.count = count;
    }

    public boolean isLiked() {
        return liked;
    }

    public long getCount() {
        return count;
    }
}
