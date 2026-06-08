package com.backend.clinic.Service;

import com.backend.clinic.DTO.InvoiceDTOs;
import com.backend.clinic.Entity.Doctor;
import com.backend.clinic.Entity.Invoice;
import com.backend.clinic.Entity.MedicalRecord;
import com.backend.clinic.Entity.Patient;
import com.backend.clinic.Entity.Payment;
import com.backend.clinic.Entity.PrescriptionItem;
import com.backend.clinic.Repository.DoctorRepository;
import com.backend.clinic.Repository.InvoiceRepository;
import com.backend.clinic.Repository.MedicalRecordRepository;
import com.backend.clinic.Repository.PatientRepository;
import com.backend.clinic.Repository.PaymentRepository;
import com.backend.clinic.Repository.PrescriptionItemRepository;
import com.backend.clinic.Security.CustomUserDetails;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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
    private final PatientRepository patientRepository;

    @Value("${stripe.secret-key}")
    private String stripeSecretKey;

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeSecretKey;
    }

    @Transactional
    public InvoiceDTOs.InvoiceDetailResponse createInvoice(InvoiceDTOs.CreateInvoiceRequest request) {
        MedicalRecord record = medicalRecordRepository.findById(request.getRecordId())
                .orElseThrow(() -> new RuntimeException("Medical record not found"));

        if (invoiceRepository.findByMedicalRecord_RecordId(record.getRecordId()).isPresent()) {
            throw new RuntimeException("Invoice already exists for this medical record");
        }

        List<PrescriptionItem> items = prescriptionItemRepository
                .findByMedicalRecord_RecordId(record.getRecordId());
        BigDecimal medFee = items.stream()
                .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal consultationFee = request.getConsultationFee() != null
                ? request.getConsultationFee() : BigDecimal.valueOf(150000);
        BigDecimal otherFee = request.getOtherFee() != null
                ? request.getOtherFee() : BigDecimal.ZERO;
        BigDecimal discount = request.getDiscount() != null
                ? request.getDiscount() : BigDecimal.ZERO;

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
    public List<InvoiceDTOs.PatientInvoiceResponse> getPatientInvoices(CustomUserDetails userDetails) {
        Patient patient = patientRepository.findByUser_UserId(userDetails.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Current user is not linked to a patient profile"));

        List<Invoice> invoices = invoiceRepository
                .findByPatient_PatientIdOrderByCreatedAtDesc(patient.getPatientId());

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

                    String doctorName = inv.getMedicalRecord().getDoctor() != null
                            ? inv.getMedicalRecord().getDoctor().getUser().getFullName() : "";

                    return InvoiceDTOs.PatientInvoiceResponse.builder()
                            .invoiceId(inv.getInvoiceId())
                            .invoiceCode(inv.getInvoiceCode())
                            .doctorName(doctorName)
                            .recordDiagnosis(diagnosis)
                            .consultationFee(inv.getConsultationFee())
                            .medicationFee(inv.getMedicationFee())
                            .otherFee(inv.getOtherFee())
                            .discount(inv.getDiscount())
                            .totalAmount(total)
                            .paymentStatus(inv.getPaymentStatus())
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
    public InvoiceDTOs.CreatePaymentIntentResponse createPaymentIntent(Long invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Invoice not found"));

        if ("PAID".equals(invoice.getPaymentStatus())) {
            throw new RuntimeException("Invoice is already paid");
        }

        if (invoice.getStripePaymentIntentId() != null) {
            try {
                PaymentIntent existing = PaymentIntent.retrieve(invoice.getStripePaymentIntentId());
                return InvoiceDTOs.CreatePaymentIntentResponse.builder()
                        .clientSecret(existing.getClientSecret())
                        .invoiceId(invoiceId)
                        .build();
            } catch (StripeException e) {
                // Intent expired or invalid, create new one
            }
        }

        BigDecimal total = invoice.getConsultationFee()
                .add(invoice.getMedicationFee())
                .add(invoice.getOtherFee() != null ? invoice.getOtherFee() : BigDecimal.ZERO)
                .subtract(invoice.getDiscount() != null ? invoice.getDiscount() : BigDecimal.ZERO);
        if (total.compareTo(BigDecimal.ZERO) < 0) total = BigDecimal.ZERO;

        try {
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(total.longValue())
                    .setCurrency("vnd")
                    .putMetadata("invoice_id", String.valueOf(invoiceId))
                    .putMetadata("invoice_code", invoice.getInvoiceCode())
                    .build();

            PaymentIntent paymentIntent = PaymentIntent.create(params);

            invoice.setStripePaymentIntentId(paymentIntent.getId());
            invoiceRepository.save(invoice);

            return InvoiceDTOs.CreatePaymentIntentResponse.builder()
                    .clientSecret(paymentIntent.getClientSecret())
                    .invoiceId(invoiceId)
                    .build();
        } catch (StripeException e) {
            throw new RuntimeException("Failed to create Stripe PaymentIntent: " + e.getMessage());
        }
    }

    @Transactional
    public InvoiceDTOs.PaymentResponse payInvoice(Long invoiceId, InvoiceDTOs.PaymentRequest request) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Invoice not found"));

        if ("PAID".equals(invoice.getPaymentStatus())) {
            throw new RuntimeException("Invoice is already paid");
        }

        if (invoice.getStripePaymentIntentId() != null) {
            try {
                PaymentIntent paymentIntent = PaymentIntent.retrieve(invoice.getStripePaymentIntentId());
                if (!"succeeded".equals(paymentIntent.getStatus())) {
                    throw new RuntimeException("Payment has not been completed. Status: " + paymentIntent.getStatus());
                }
            } catch (StripeException e) {
                throw new RuntimeException("Failed to verify Stripe payment: " + e.getMessage());
            }
        }

        Payment payment = Payment.builder()
                .paymentCode("PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .invoice(invoice)
                .amount(request.getAmount())
                .paymentMethod(request.getPaymentMethod() != null ? request.getPaymentMethod() : "CREDIT_CARD")
                .transactionRef(request.getStripePaymentIntentId())
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
                .paidAt(LocalDateTime.now())
                .build();
    }

    private InvoiceDTOs.InvoiceDetailResponse mapToResponse(Invoice invoice, BigDecimal calculatedTotal) {
        return InvoiceDTOs.InvoiceDetailResponse.builder()
                .invoiceId(invoice.getInvoiceId())
                .invoiceCode(invoice.getInvoiceCode())
                .recordId(invoice.getMedicalRecord().getRecordId())
                .patientId(invoice.getPatient().getPatientId())
                .patientName(invoice.getPatient().getUser() != null
                        ? invoice.getPatient().getUser().getFullName() : "")
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
