package com.example.blog.comment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class SensitiveWordRequest {

    @NotBlank(message = "word 不能为空")
    @Size(max = 64, message = "word 长度不能超过 64")
    private String word;

    public String getWord() {
        return word;
    }

    public void setWord(String word) {
        this.word = word;
    }
}
