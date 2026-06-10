package com.backend.clinic.Repository;

import com.backend.clinic.Entity.Invoice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    Optional<Invoice> findByInvoiceCode(String invoiceCode);
    Optional<Invoice> findByMedicalRecord_RecordId(Long recordId);
    List<Invoice> findAllByMedicalRecord_Doctor_DoctorIdOrderByCreatedAtDesc(Long doctorId);
    List<Invoice> findByPatient_PatientIdOrderByCreatedAtDesc(Long patientId);

    @Query("""
            select i from Invoice i
            left join i.patient p
            left join p.user u
            where (:keyword is null or :keyword = ''
                   or lower(i.invoiceCode) like lower(concat('%', :keyword, '%'))
                   or lower(u.fullName) like lower(concat('%', :keyword, '%'))
                   or lower(u.phone) like lower(concat('%', :keyword, '%')))
            order by i.createdAt desc
            """)
    Page<Invoice> searchInvoices(@Param("keyword") String keyword, Pageable pageable);
}
