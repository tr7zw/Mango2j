package dev.tr7zw.mango2j.db;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@RequiredArgsConstructor
@NoArgsConstructor
@Entity
public class Chapter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Getter
    private Integer id;

    @NonNull
    @Getter
    @Setter
    @Column(unique = true, length = 1024)
    private String fullPath;
    
    @NonNull
    @Getter
    @Setter
    private String path;
    
    @NonNull
    @Getter
    private String name;
    
    @Getter
    @Setter
    @Column(length = 100_000)
    private byte[] thumbnail;
    
    @Getter
    @Setter
    private Integer pageCount;
    
    @Getter
    @Setter
    private Integer views = 0;
    
    @Getter
    @Setter
    private Instant lastView = null;
    
    @Getter
    @Setter
    @Column(length = 10_000)
    private String description;
    
    @Getter
    @Setter
    private float[] embedding;
    
}
