package dev.tr7zw.mango2j.db;

import java.util.*;

import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.*;
import org.springframework.data.jpa.repository.*;

public interface ChapterRepository extends JpaRepository<Chapter, Integer>, JpaSpecificationExecutor<Chapter> {
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
            "ORDER BY c.views DESC")
    List<Chapter> findTop100ByOrderByViewsDesc();
    List<Chapter> findTop100ByOrderByViewsAsc();
    List<Chapter> findByNameContainingOrDescriptionContaining(String name, String description);

    static Specification<Chapter> descriptionMatches(String search) {
        return (root, query, cb) -> {
            if (search == null || search.isBlank()) {
                return cb.conjunction();
            }

            String[] tokens = search.split("[,\\s]+"); // split by comma or space
            List<Predicate> predicates = new ArrayList<>();

            for (String token : tokens) {
                if (!token.isBlank()) {
                    predicates.add(
                            cb.like(
                                    cb.lower(root.get("description")),
                                    "%" + token.trim().toLowerCase() + "%"
                            )
                    );
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    static Specification<Chapter> descriptionRankedSearch(String search) {
        return (root, query, cb) -> {

            if (search == null || search.isBlank()) {
                return cb.conjunction();
            }

            String[] tokens = search.split("[,\\s]+");

            List<Expression<Integer>> scoreParts = new ArrayList<>();
            List<Predicate> orPredicates = new ArrayList<>();

            for (String token : tokens) {
                if (!token.isBlank()) {

                    Expression<Integer> matchScore =
                            cb.<Integer>selectCase()
                                    .when(
                                            cb.like(
                                                    cb.lower(root.get("description")),
                                                    "%" + token.trim().toLowerCase() + "%"
                                            ),
                                            1
                                    )
                                    .otherwise(0);

                    scoreParts.add(matchScore);

                    // optional: ensure at least one token matches
                    orPredicates.add(
                            cb.like(
                                    cb.lower(root.get("description")),
                                    "%" + token.trim().toLowerCase() + "%"
                            )
                    );
                }
            }

            // sum all CASE expressions
            Expression<Integer> totalScore = scoreParts.get(0);
            for (int i = 1; i < scoreParts.size(); i++) {
                totalScore = cb.sum(totalScore, scoreParts.get(i));
            }

            // order by score DESC
            query.orderBy(cb.desc(totalScore));

            // match at least one token
            return cb.or(orPredicates.toArray(new Predicate[0]));
        };
    }

}