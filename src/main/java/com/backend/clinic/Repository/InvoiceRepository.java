package com.backend.clinic.Repository;

import com.backend.clinic.Entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    Optional<Invoice> findByInvoiceCode(String invoiceCode);
    Optional<Invoice> findByMedicalRecord_RecordId(Long recordId);
    List<Invoice> findAllByMedicalRecord_Doctor_DoctorIdOrderByCreatedAtDesc(Long doctorId);
    List<Invoice> findByPatient_PatientIdOrderByCreatedAtDesc(Long patientId);
}
