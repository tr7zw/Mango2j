package dev.tr7zw.mango2j.db;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TitleRepository extends JpaRepository<Title, Integer> {

    List<Title> findByPath(String path);
    boolean existsByPathAndName(String path, String name);
    boolean existsByFullPath(String fullPath);
    Title findByFullPath(String fullPath);
    
}