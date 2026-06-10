package com.backend.clinic.Controller;

import com.backend.clinic.Repository.DoctorRepository;
import com.backend.clinic.Repository.PatientRepository;
import com.backend.clinic.Repository.PaymentRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.web.bind.annotation.RequestParam;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;
import com.backend.clinic.Entity.Payment;
import com.backend.clinic.DTO.PaymentDTO;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {

    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final PaymentRepository paymentRepository;

    @GetMapping("/patients/total")
    public ResponseEntity<Map<String, Long>> totalPatients() {
        long total = patientRepository.count();
        return ResponseEntity.ok(Map.of("total", total));
    }

    @GetMapping("/doctors/total")
    public ResponseEntity<Map<String, Long>> totalDoctors() {
        long total = doctorRepository.count();
        return ResponseEntity.ok(Map.of("total", total));
    }

    @GetMapping
    public ResponseEntity<Map<String, Long>> allTotals() {
        long patients = patientRepository.count();
        long doctors = doctorRepository.count();
        return ResponseEntity.ok(Map.of("patients", patients, "doctors", doctors));
    }

    @GetMapping("/revenue")
    public ResponseEntity<Map<String, Object>> revenue(@RequestParam(required = false) Integer year,
                                                       @RequestParam(required = false) Integer month) {
        LocalDate now = LocalDate.now();
        int y = (year != null) ? year : now.getYear();
        int m = (month != null) ? month : now.getMonthValue();

        BigDecimal monthSum = paymentRepository.sumAmountByYearAndMonth(y, m);
        BigDecimal yearSum = paymentRepository.sumAmountByYear(y);

        return ResponseEntity.ok(Map.of(
                "year", y,
                "month", m,
                "monthRevenue", monthSum,
                "yearRevenue", yearSum
        ));
    }

    @GetMapping("/payments")
    public ResponseEntity<List<PaymentDTO>> payments(@RequestParam(required = false) Integer year,
                                                     @RequestParam(required = false) Integer month) {
        LocalDate now = LocalDate.now();
        int y = (year != null) ? year : now.getYear();
        int m = (month != null) ? month : now.getMonthValue();

        List<Payment> payments = paymentRepository.findAll();

        List<PaymentDTO> dtos = payments.stream()
                .filter(p -> p.getStatus() != null && p.getStatus().equals("SUCCESS"))
                .filter(p -> {
                    LocalDateTime paidAt = p.getPaidAt();
                    return paidAt != null && paidAt.getYear() == y && paidAt.getMonthValue() == m;
                })
                .map(p -> new PaymentDTO(
                        p.getPaymentId(),
                        p.getPaymentCode(),
                        p.getAmount(),
                        p.getPaymentMethod(),
                        p.getStatus(),
                        p.getPaidAt(),
                        p.getInvoice() != null ? p.getInvoice().getInvoiceId() : null
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/revenue/monthly")
    public ResponseEntity<Map<String, Object>> monthlyRevenue(@RequestParam(required = false) Integer year) {
        LocalDate now = LocalDate.now();
        int y = (year != null) ? year : now.getYear();

        // initialize 12 months with 0
        List<BigDecimal> totals = Arrays.asList(
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO
        );

        List<Object[]> rows = paymentRepository.sumAmountGroupedByMonth(y);
        for (Object[] row : rows) {
            if (row == null || row.length < 2) continue;
            int month = ((Number) row[0]).intValue();
            BigDecimal total = (row[1] == null) ? BigDecimal.ZERO : new BigDecimal(row[1].toString());
            totals.set(month - 1, total);
        }

        List<String> labels = Arrays.asList("Thg 1","Thg 2","Thg 3","Thg 4","Thg 5","Thg 6","Thg 7","Thg 8","Thg 9","Thg 10","Thg 11","Thg 12");

        return ResponseEntity.ok(Map.of("year", y, "labels", labels, "data", totals));
    }
}
