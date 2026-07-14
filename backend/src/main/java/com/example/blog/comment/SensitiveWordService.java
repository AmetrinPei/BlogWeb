package com.example.blog.comment;

import com.example.blog.common.BusinessException;
import com.example.blog.common.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class SensitiveWordService {

    private final SensitiveWordRepository sensitiveWordRepository;

    public SensitiveWordService(SensitiveWordRepository sensitiveWordRepository) {
        this.sensitiveWordRepository = sensitiveWordRepository;
    }

    @Transactional(readOnly = true)
    public List<SensitiveWordResponse> list() {
        return sensitiveWordRepository.findAllByOrderByIdAsc().stream()
                .map(SensitiveWordResponse::from)
                .toList();
    }

    @Transactional
    public SensitiveWordResponse create(SensitiveWordRequest request) {
        String word = normalize(request.getWord());
        if (word.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "word 不能为空");
        }
        if (sensitiveWordRepository.existsByWord(word)) {
            throw new BusinessException(ErrorCode.CONFLICT, "敏感词已存在");
        }
        SensitiveWord entity = new SensitiveWord();
        entity.setWord(word);
        return SensitiveWordResponse.from(sensitiveWordRepository.save(entity));
    }

    @Transactional
    public void delete(Long id) {
        if (!sensitiveWordRepository.existsById(id)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "敏感词不存在");
        }
        sensitiveWordRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public boolean hits(String content) {
        if (content == null || content.isBlank()) {
            return false;
        }
        String normalized = content.trim().toLowerCase(Locale.ROOT);
        for (SensitiveWord word : sensitiveWordRepository.findAll()) {
            if (normalized.contains(word.getWord())) {
                return true;
            }
        }
        return false;
    }

    static String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim().toLowerCase(Locale.ROOT);
    }
}
