package dev.tr7zw.mango2j.db;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface InternalTaskRepository extends JpaRepository<InternalTask, Long> {

    InternalTask findFirstByStatusOrderByIdAsc(InternalTaskStatus status);

    InternalTask findFirstByStatusOrderByIdDesc(InternalTaskStatus status);

    List<InternalTask> findByStatusOrderByIdAsc(InternalTaskStatus status);

    List<InternalTask> findTop300ByOrderByIdDesc();

    List<InternalTask> findByStatusInOrderByIdDesc(List<InternalTaskStatus> status);

    @Transactional
    void deleteByInternalTaskTrue();
}
