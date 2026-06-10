package com.backend.clinic.Service;

import com.backend.clinic.Entity.MonthlyStat;
import com.backend.clinic.Entity.Payment;
import com.backend.clinic.Repository.MonthlyStatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class StatsUpdaterService {

    private final MonthlyStatRepository monthlyStatRepository;

    @Transactional
    public void recordPayment(Payment payment) {
        if (payment == null) return;
        if (payment.getStatus() == null || !payment.getStatus().equals("SUCCESS")) return;
        LocalDateTime paidAt = payment.getPaidAt();
        if (paidAt == null) return;

        int year = paidAt.getYear();
        int month = paidAt.getMonthValue();

        MonthlyStat stat = monthlyStatRepository.findByYearAndMonth(year, month)
                .orElseGet(() -> MonthlyStat.builder()
                        .year(year)
                        .month(month)
                        .paymentCount(0)
                        .totalAmount(BigDecimal.ZERO)
                        .build());

        BigDecimal amount = payment.getAmount() == null ? BigDecimal.ZERO : payment.getAmount();
        stat.setPaymentCount(stat.getPaymentCount() + 1);
        stat.setTotalAmount(stat.getTotalAmount().add(amount));

        monthlyStatRepository.save(stat);
    }
}
