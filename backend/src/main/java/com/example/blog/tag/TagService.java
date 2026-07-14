package com.example.blog.tag;

import com.example.blog.article.ArticleRepository;
import com.example.blog.common.BusinessException;
import com.example.blog.common.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TagService {

    private final TagRepository tagRepository;
    private final ArticleRepository articleRepository;

    public TagService(TagRepository tagRepository, ArticleRepository articleRepository) {
        this.tagRepository = tagRepository;
        this.articleRepository = articleRepository;
    }

    @Transactional(readOnly = true)
    public List<TagResponse> list() {
        return tagRepository.findAllByOrderByIdAsc().stream()
                .map(TagResponse::from)
                .toList();
    }

    @Transactional
    public TagResponse create(TagRequest request) {
        String name = request.getName().trim();
        if (tagRepository.existsByName(name)) {
            throw new BusinessException(ErrorCode.CONFLICT, "标签名称已存在");
        }
        Tag tag = new Tag();
        tag.setName(name);
        return TagResponse.from(tagRepository.save(tag));
    }

    @Transactional
    public TagResponse update(Long id, TagRequest request) {
        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "标签不存在"));
        String name = request.getName().trim();
        if (tagRepository.existsByNameAndIdNot(name, id)) {
            throw new BusinessException(ErrorCode.CONFLICT, "标签名称已存在");
        }
        tag.setName(name);
        return TagResponse.from(tagRepository.save(tag));
    }

    @Transactional
    public void delete(Long id) {
        if (!tagRepository.existsById(id)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "标签不存在");
        }
        articleRepository.deleteTagAssociations(id);
        tagRepository.deleteById(id);
    }
}
