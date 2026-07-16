package com.example.blog.auth;

import com.example.blog.common.BusinessException;
import com.example.blog.common.ErrorCode;
import com.example.blog.config.LoginRateLimitProperties;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory sliding-window login failure limiter (blog-rate-limit Slice-A).
 */
@Service
public class LoginRateLimitService {

    private final LoginRateLimitProperties properties;
    private final ConcurrentHashMap<String, Deque<Long>> failures = new ConcurrentHashMap<>();

    public LoginRateLimitService(LoginRateLimitProperties properties) {
        this.properties = properties;
    }

    public void assertNotBlocked(String clientIp, String username) {
        if (!properties.isEnabled()) {
            return;
        }
        long now = System.currentTimeMillis();
        if (countInWindow(ipKey(clientIp), now) >= properties.getMaxFailures()
                || countInWindow(userKey(username), now) >= properties.getMaxFailures()) {
            throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS, "登录尝试过于频繁，请稍后再试");
        }
    }

    public void recordFailure(String clientIp, String username) {
        if (!properties.isEnabled()) {
            return;
        }
        long now = System.currentTimeMillis();
        record(ipKey(clientIp), now);
        record(userKey(username), now);
    }

    public void clear(String clientIp, String username) {
        failures.remove(ipKey(clientIp));
        failures.remove(userKey(username));
    }

    /** Test helper: clear all counters. */
    public void clearAll() {
        failures.clear();
    }

    private void record(String key, long nowMs) {
        Deque<Long> q = failures.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (q) {
            prune(q, nowMs);
            q.addLast(nowMs);
        }
    }

    private int countInWindow(String key, long nowMs) {
        Deque<Long> q = failures.get(key);
        if (q == null) {
            return 0;
        }
        synchronized (q) {
            prune(q, nowMs);
            return q.size();
        }
    }

    private void prune(Deque<Long> q, long nowMs) {
        long cutoff = nowMs - properties.getWindowSeconds() * 1000L;
        while (!q.isEmpty() && q.peekFirst() < cutoff) {
            q.removeFirst();
        }
    }

    private static String ipKey(String clientIp) {
        String ip = clientIp == null || clientIp.isBlank() ? "unknown" : clientIp.trim();
        return "ip:" + ip;
    }

    private static String userKey(String username) {
        String name = username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
        return "user:" + name;
    }
}
