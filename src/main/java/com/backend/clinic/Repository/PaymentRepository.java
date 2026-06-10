package com.backend.clinic.Repository;

import com.backend.clinic.Entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.math.BigDecimal;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByPaymentCode(String paymentCode);
    List<Payment> findByInvoice_InvoiceId(Long invoiceId);

    @Query(value = "SELECT COALESCE(SUM(amount),0) FROM payments WHERE EXTRACT(YEAR FROM paid_at) = :year AND EXTRACT(MONTH FROM paid_at) = :month AND status='SUCCESS'", nativeQuery = true)
    BigDecimal sumAmountByYearAndMonth(@Param("year") int year, @Param("month") int month);

    @Query(value = "SELECT EXTRACT(MONTH FROM paid_at) as month, COALESCE(SUM(amount),0) as total FROM payments WHERE EXTRACT(YEAR FROM paid_at) = :year AND status='SUCCESS' GROUP BY month ORDER BY month", nativeQuery = true)
    List<Object[]> sumAmountGroupedByMonth(@Param("year") int year);

    @Query(value = "SELECT COALESCE(SUM(amount),0) FROM payments WHERE EXTRACT(YEAR FROM paid_at) = :year AND status='SUCCESS'", nativeQuery = true)
    BigDecimal sumAmountByYear(@Param("year") int year);
}
