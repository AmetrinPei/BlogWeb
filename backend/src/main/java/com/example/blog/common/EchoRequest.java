package com.example.blog.common;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Task-1 验收用：演示参数校验与统一响应（后续可删除）。
 */
public class EchoRequest {

    @NotBlank(message = "name 不能为空")
    @Size(max = 32, message = "name 最长 32 个字符")
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
