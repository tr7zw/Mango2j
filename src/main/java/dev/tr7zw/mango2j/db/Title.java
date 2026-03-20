package dev.tr7zw.mango2j.db;

import jakarta.persistence.*;
import lombok.*;

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

}
