package com.backend.clinic.Repository;

import com.backend.clinic.Entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    Optional<Conversation> findByDoctor_DoctorIdAndPatient_PatientId(Long doctorId, Long patientId);
    List<Conversation> findByDoctor_DoctorId(Long doctorId);
    List<Conversation> findByPatient_PatientId(Long patientId);
}
