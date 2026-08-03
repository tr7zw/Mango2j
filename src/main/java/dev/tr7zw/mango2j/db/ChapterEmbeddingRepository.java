package dev.tr7zw.mango2j.db;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ChapterEmbeddingRepository extends JpaRepository<ChapterEmbedding, Integer> {
    Optional<ChapterEmbedding> findByChapterId(Integer chapterId);
    List<ChapterEmbedding> findByChapterIdIn(Collection<Integer> chapterIds);
}
