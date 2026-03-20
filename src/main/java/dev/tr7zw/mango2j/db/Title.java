package dev.tr7zw.mango2j.db;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@RequiredArgsConstructor
@NoArgsConstructor
@Entity
public class Title {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Getter
    private Integer id;

    @NonNull
    @Getter
    @Column(unique = true, length = 1024)
    private String fullPath;

    @NonNull
    @Getter
    private String path;

    @NonNull
    @Getter
    private String name;

    @Getter
    @Setter
    private Integer totalViews = 0;

    @Getter
    @Setter
    private Integer chapterCount = 0;

    @Getter
    @Setter
    private Instant newestChapterTime;

    @Getter
    @Setter
    private Long fileSize;

}
