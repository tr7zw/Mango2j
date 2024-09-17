package dev.tr7zw.mango2j.db;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ChapterRepository extends JpaRepository<Chapter, Integer> {
    // You can add custom query methods here if needed
    List<Chapter> findByPath(String path);
    boolean existsByPathAndName(String path, String name);
    boolean existsByFullPath(String fullPath);
    List<Chapter> findByThumbnailIsNull();
    List<Chapter> findTop100ByOrderByIdDesc();
    List<Chapter> findByPageCountIsNotNullOrderByPageCountAsc();
    
}