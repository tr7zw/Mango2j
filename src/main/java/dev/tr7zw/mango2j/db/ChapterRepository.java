package dev.tr7zw.mango2j.db;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ChapterRepository extends JpaRepository<Chapter, Integer> {
    // You can add custom query methods here if needed
    List<Chapter> findByPath(String path);
    boolean existsByPathAndName(String path, String name);
    boolean existsByFullPath(String fullPath);
    List<Chapter> findByThumbnailIsNull();
    List<Chapter> findTop100ByOrderByIdDesc();
    @Query("SELECT c FROM Chapter c " +
            "WHERE c.pageCount IS NOT NULL " +
            "AND (c.views IS NULL OR c.views = 0) " +
            "ORDER BY c.pageCount ASC")
    List<Chapter> findEmptyDownloads();
    @Query("SELECT c FROM Chapter c " +
            "WHERE c.pageCount IS NOT NULL " +
            "AND c.views > 0 " +
            "AND c.description IS NULL " +
            "ORDER BY c.views DESC")
    List<Chapter> findReadChaptersWithoutDescription();
    List<Chapter> findTop100ByOrderByViewsDesc();
    List<Chapter> findTop100ByOrderByViewsAsc();
    
}