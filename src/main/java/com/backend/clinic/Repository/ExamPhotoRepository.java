package com.backend.clinic.Repository;

import com.backend.clinic.Entity.ExamPhoto;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ExamPhotoRepository extends JpaRepository<ExamPhoto, Long> {
    List<ExamPhoto> findByRecordIdOrderBySortOrderAsc(Long recordId);
    void deleteByRecordId(Long recordId);
}
