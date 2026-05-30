package com.backend.clinic.Service;

import com.backend.clinic.DTO.InvoiceDTOs;
import com.backend.clinic.Entity.Doctor;
import com.backend.clinic.Entity.Invoice;
import com.backend.clinic.Entity.MedicalRecord;
import com.backend.clinic.Entity.Payment;
import com.backend.clinic.Entity.PrescriptionItem;
import com.backend.clinic.Repository.DoctorRepository;
import com.backend.clinic.Repository.InvoiceRepository;
import com.backend.clinic.Repository.MedicalRecordRepository;
import com.backend.clinic.Repository.PaymentRepository;
import com.backend.clinic.Repository.PrescriptionItemRepository;
import com.backend.clinic.Security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final PrescriptionItemRepository prescriptionItemRepository;
    private final PaymentRepository paymentRepository;
    private final DoctorRepository doctorRepository;

    @Transactional
    public InvoiceDTOs.InvoiceDetailResponse createInvoice(InvoiceDTOs.CreateInvoiceRequest request) {
        MedicalRecord record = medicalRecordRepository.findById(request.getRecordId())
                .orElseThrow(() -> new RuntimeException("Medical record not found"));

        if (invoiceRepository.findByMedicalRecord_RecordId(record.getRecordId()).isPresent()) {
            throw new RuntimeException("Invoice already exists for this medical record");
        }

        // Calculate medication fee
        List<PrescriptionItem> items = prescriptionItemRepository.findByMedicalRecord_RecordId(record.getRecordId());
        BigDecimal medFee = items.stream()
                .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal consultationFee = request.getConsultationFee() != null ? request.getConsultationFee() : BigDecimal.valueOf(150000); // Default 150k
        BigDecimal otherFee = request.getOtherFee() != null ? request.getOtherFee() : BigDecimal.ZERO;
        BigDecimal discount = request.getDiscount() != null ? request.getDiscount() : BigDecimal.ZERO;

        Invoice invoice = Invoice.builder()
                .invoiceCode("INV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .medicalRecord(record)
                .patient(record.getPatient())
                .consultationFee(consultationFee)
                .medicationFee(medFee)
                .otherFee(otherFee)
                .discount(discount)
                .paymentStatus("UNPAID")
                .build();

        invoice = invoiceRepository.save(invoice);
        
        // totalAmount is calculated by DB (generated column), but we might need it for response immediately
        BigDecimal total = consultationFee.add(medFee).add(otherFee).subtract(discount);
        if (total.compareTo(BigDecimal.ZERO) < 0) total = BigDecimal.ZERO;

        return mapToResponse(invoice, total);
    }

    @Transactional(readOnly = true)
    public List<InvoiceDTOs.DoctorInvoiceResponse> getDoctorInvoices(CustomUserDetails userDetails) {
        Doctor doctor = doctorRepository.findByUser_UserId(userDetails.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Current user is not linked to a doctor profile"));

        List<Invoice> invoices = invoiceRepository
                .findAllByMedicalRecord_Doctor_DoctorIdOrderByCreatedAtDesc(doctor.getDoctorId());

        return invoices.stream()
                .map(inv -> {
                    BigDecimal total = inv.getConsultationFee()
                            .add(inv.getMedicationFee())
                            .add(inv.getOtherFee() != null ? inv.getOtherFee() : BigDecimal.ZERO)
                            .subtract(inv.getDiscount() != null ? inv.getDiscount() : BigDecimal.ZERO);
                    if (total.compareTo(BigDecimal.ZERO) < 0) total = BigDecimal.ZERO;

                    String diagnosis = inv.getMedicalRecord().getFinalDiagnosis();
                    if ((diagnosis == null || diagnosis.isBlank())
                            && inv.getMedicalRecord().getFinalDisease() != null) {
                        diagnosis = inv.getMedicalRecord().getFinalDisease().getDiseaseNameVi();
                    }

                    return InvoiceDTOs.DoctorInvoiceResponse.builder()
                            .invoiceId(inv.getInvoiceId())
                            .invoiceCode(inv.getInvoiceCode())
                            .recordId(inv.getMedicalRecord().getRecordId())
                            .patientId(inv.getPatient().getPatientId())
                            .patientName(inv.getPatient().getUser().getFullName())
                            .consultationFee(inv.getConsultationFee())
                            .medicationFee(inv.getMedicationFee())
                            .totalAmount(total)
                            .paymentStatus(inv.getPaymentStatus())
                            .recordDiagnosis(diagnosis)
                            .createdAt(inv.getCreatedAt())
                            .build();
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public InvoiceDTOs.InvoiceDetailResponse getInvoiceByRecord(Long recordId) {
        Invoice invoice = invoiceRepository.findByMedicalRecord_RecordId(recordId)
                .orElseThrow(() -> new RuntimeException("Invoice not found for this record"));
        return mapToResponse(invoice, invoice.getTotalAmount());
    }

    @Transactional
    public InvoiceDTOs.PaymentResponse payInvoice(Long invoiceId, InvoiceDTOs.PaymentRequest request) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Invoice not found"));

        if ("PAID".equals(invoice.getPaymentStatus())) {
            throw new RuntimeException("Invoice is already paid");
        }

        Payment payment = Payment.builder()
                .paymentCode("PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .invoice(invoice)
                .amount(request.getAmount())
                .paymentMethod(request.getPaymentMethod() != null ? request.getPaymentMethod() : "CASH")
                .status("SUCCESS")
                .build();

        payment = paymentRepository.save(payment);

        invoice.setPaymentStatus("PAID");
        invoiceRepository.save(invoice);

        return InvoiceDTOs.PaymentResponse.builder()
                .paymentId(payment.getPaymentId())
                .paymentCode(payment.getPaymentCode())
                .amount(payment.getAmount())
                .paymentMethod(payment.getPaymentMethod())
                .status(payment.getStatus())
                .paidAt(LocalDateTime.now()) // approximate
                .build();
    }

    private InvoiceDTOs.InvoiceDetailResponse mapToResponse(Invoice invoice, BigDecimal calculatedTotal) {
        return InvoiceDTOs.InvoiceDetailResponse.builder()
                .invoiceId(invoice.getInvoiceId())
                .invoiceCode(invoice.getInvoiceCode())
                .recordId(invoice.getMedicalRecord().getRecordId())
                .patientId(invoice.getPatient().getPatientId())
                .patientName(invoice.getPatient().getUser() != null ? invoice.getPatient().getUser().getFullName() : "")
                .consultationFee(invoice.getConsultationFee())
                .medicationFee(invoice.getMedicationFee())
                .otherFee(invoice.getOtherFee())
                .discount(invoice.getDiscount())
                .totalAmount(calculatedTotal != null ? calculatedTotal : invoice.getTotalAmount())
                .paymentStatus(invoice.getPaymentStatus())
                .createdAt(invoice.getCreatedAt())
                .build();
    }
}
