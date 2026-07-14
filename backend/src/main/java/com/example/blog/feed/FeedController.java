package com.example.blog.feed;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FeedController {

    private static final MediaType RSS_XML = MediaType.parseMediaType("application/rss+xml;charset=UTF-8");

    private final FeedService feedService;

    public FeedController(FeedService feedService) {
        this.feedService = feedService;
    }

    @GetMapping(value = "/feed.xml", produces = "application/rss+xml;charset=UTF-8")
    public ResponseEntity<String> feed() {
        return ResponseEntity.ok()
                .contentType(RSS_XML)
                .body(feedService.buildRss());
    }
}
