package com.backend.clinic.Controller;

import com.backend.clinic.DTO.InvoiceDTOs;
import com.backend.clinic.Security.CustomUserDetails;
import com.backend.clinic.Service.InvoiceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@PreAuthorize("hasRole('DOCTOR') or hasRole('PATIENT') or hasRole('ADMIN')")
public class InvoiceController {

    private final InvoiceService invoiceService;

    @PostMapping
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<InvoiceDTOs.InvoiceDetailResponse> createInvoice(
            @Valid @RequestBody InvoiceDTOs.CreateInvoiceRequest request
    ) {
        return ResponseEntity.ok(invoiceService.createInvoice(request));
    }

    @GetMapping("/doctor")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<List<InvoiceDTOs.DoctorInvoiceResponse>> getDoctorInvoices(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(invoiceService.getDoctorInvoices(userDetails));
    }

    @GetMapping("/my")
    public ResponseEntity<List<InvoiceDTOs.PatientInvoiceResponse>> getMyInvoices(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(invoiceService.getPatientInvoices(userDetails));
    }

    @GetMapping("/record/{recordId}")
    public ResponseEntity<InvoiceDTOs.InvoiceDetailResponse> getInvoiceByRecord(
            @PathVariable Long recordId
    ) {
        return ResponseEntity.ok(invoiceService.getInvoiceByRecord(recordId));
    }

    @PostMapping("/{invoiceId}/create-payment-intent")
    public ResponseEntity<InvoiceDTOs.CreatePaymentIntentResponse> createPaymentIntent(
            @PathVariable Long invoiceId
    ) {
        return ResponseEntity.ok(invoiceService.createPaymentIntent(invoiceId));
    }

    @PutMapping("/{invoiceId}/pay")
    public ResponseEntity<InvoiceDTOs.PaymentResponse> payInvoice(
            @PathVariable Long invoiceId,
            @Valid @RequestBody InvoiceDTOs.PaymentRequest request
    ) {
        return ResponseEntity.ok(invoiceService.payInvoice(invoiceId, request));
    }
}
