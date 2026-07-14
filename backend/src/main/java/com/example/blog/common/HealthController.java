package com.example.blog.common;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Task-1 验收用健康检查与校验演示接口。
 */
@RestController
@RequestMapping("/api")
public class HealthController {

    @GetMapping("/health")
    public Result<Map<String, String>> health() {
        return Result.ok(Map.of("status", "UP"));
    }

    @PostMapping("/echo")
    public Result<Map<String, String>> echo(@Valid @RequestBody EchoRequest request) {
        return Result.ok(Map.of("name", request.getName()));
    }

    @GetMapping("/demo/conflict")
    public Result<Void> demoConflict() {
        throw new BusinessException(ErrorCode.CONFLICT, "演示业务冲突");
    }
}
