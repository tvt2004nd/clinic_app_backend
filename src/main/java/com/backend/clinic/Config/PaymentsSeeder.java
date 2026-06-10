package com.backend.clinic.Config;

import com.backend.clinic.Entity.*;
import com.backend.clinic.Repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.util.stream.IntStream;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentsSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final SpecialtyRepository specialtyRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final AppointmentRepository appointmentRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
                // Seed until we reach TARGET payments (idempotent)
                final int TARGET = 100; // desired total payments in DB after seeding
                long existing = paymentRepository.count();
                if (existing >= TARGET) {
                        log.info("PaymentsSeeder: {} payments exist (>= {}), skipping seeding.", existing, TARGET);
                        return;
                }

        // ensure roles
        Role patientRole = roleRepository.findByRoleCode("PATIENT").orElseGet(() -> roleRepository.save(Role.builder()
                .roleCode("PATIENT").roleName("Bệnh nhân").description("Vai trò bệnh nhân").build()));
        Role doctorRole = roleRepository.findByRoleCode("DOCTOR").orElseGet(() -> roleRepository.save(Role.builder()
                .roleCode("DOCTOR").roleName("Bác sĩ").description("Vai trò bác sĩ").build()));

        // ensure specialty
        Specialty spec = specialtyRepository.findById(1).orElseGet(() -> specialtyRepository.save(Specialty.builder()
                .specialtyCode("GEN").specialtyName("General").description("General").build()));

        // ensure a doctor user + doctor
        User docUser = userRepository.findByUsername("seed_doctor").orElseGet(() -> userRepository.save(User.builder()
                .username("seed_doctor").email("seed_doctor@example.com").passwordHash(passwordEncoder.encode("Doctor123!"))
                .fullName("Seed Doctor").phone("0909000000").role(doctorRole).isActive(true).build()));

        Doctor doctor = doctorRepository.findByUser_UserId(docUser.getUserId()).orElseGet(() -> doctorRepository.save(Doctor.builder()
                .user(docUser).doctorCode("SD001").title("BS").specialty(spec).licenseNumber("LIC-SD-1").experienceYears(5).build()));

        // Create payments until TARGET is reached — skip existing by payment code
        int start = (int) existing + 1;
        int year = LocalDate.now().getYear();
        for (int i = start; i <= TARGET; i++) {
                final int idx = i;
                try {
                                String uname = "seed_patient" + idx;
                User patientUser = userRepository.findByUsername(uname).orElseGet(() -> userRepository.save(User.builder()
                        .username(uname)
                        .email(uname + "@example.com")
                                                .passwordHash(passwordEncoder.encode("Patient123!"))
                                                .fullName("Seed Patient " + idx)
                                                .phone(String.format("090100%04d", idx))
                        .role(patientRole)
                        .isActive(true)
                        .build()));

                Patient patient = patientRepository.findByUser_UserId(patientUser.getUserId()).orElseGet(() -> patientRepository.save(Patient.builder()
                        .user(patientUser)
                        .patientCode(String.format("P%04d", idx))
                        .bloodType("UNKNOWN")
                        .medicalHistory("seeded")
                        .build()));

                String apptCode = "SDAPPT" + idx;
                // spread appointments over the months so stats look varied
                int month = ((idx - 1) % 12) + 1;
                int day = Math.min(20, ((idx - 1) % 28) + 1);
                Appointment appt = appointmentRepository.findByAppointmentCode(apptCode).orElseGet(() -> appointmentRepository.save(Appointment.builder()
                        .appointmentCode(apptCode)
                        .patient(patient)
                        .doctor(doctor)
                        .appointmentDate(LocalDate.of(year, month, day))
                        .appointmentTime(LocalTime.of(9 + (idx % 8), 0))
                        .reason("seed")
                        .status("CONFIRMED")
                        .build()));

                String recordCode = "SEEDREC" + idx;
                MedicalRecord record = medicalRecordRepository.findByRecordCode(recordCode).orElseGet(() -> medicalRecordRepository.save(MedicalRecord.builder()
                        .recordCode(recordCode)
                        .appointment(appt)
                        .patient(patient)
                        .doctor(doctor)
                        .symptoms("symptom")
                        .finalDiagnosis("diagnosis")
                        .build()));

                String invCode = "SEEDINV" + idx;
                // vary fees slightly so totals differ
                BigDecimal consult = new BigDecimal(100000 + ((idx % 5) * 10000));
                BigDecimal med = new BigDecimal(50000 + ((idx % 3) * 5000));
                Invoice invoice = invoiceRepository.findByInvoiceCode(invCode).orElseGet(() -> invoiceRepository.save(Invoice.builder()
                        .invoiceCode(invCode)
                        .medicalRecord(record)
                        .patient(patient)
                        .consultationFee(consult)
                        .medicationFee(med)
                        .otherFee(BigDecimal.ZERO)
                        .discount(BigDecimal.ZERO)
                        .paymentStatus("PAID")
                        .build()));

                                String payCode = "SEEDPAY" + idx;
                                paymentRepository.findByPaymentCode(payCode).orElseGet(() -> {
                                        BigDecimal total = invoice.getTotalAmount() != null ? invoice.getTotalAmount() : invoice.getConsultationFee().add(invoice.getMedicationFee()).add(invoice.getOtherFee()).subtract(invoice.getDiscount());
                                        // paidAt distributed across months
                                        LocalDateTime paidAt = LocalDateTime.of(LocalDate.of(year, month, Math.min(day,28)), LocalTime.of(10 + (idx % 6), 15));
                                        Payment p = Payment.builder()
                                                        .paymentCode(payCode)
                                                        .invoice(invoice)
                                                        .amount(total)
                                                        .paymentMethod(idx % 2 == 0 ? "VNPAY" : "CASH")
                                                        .transactionRef("TXN-SEED-" + System.currentTimeMillis() + "-" + idx)
                                                        .status("SUCCESS")
                                                        .paidAt(paidAt)
                                                        .build();
                                        Payment saved = paymentRepository.save(p);
                                        log.info("Created payment {} for invoice {} (paidAt={})", saved.getPaymentCode(), invoice.getInvoiceId(), saved.getPaidAt());
                                        return saved;
                                });

                        } catch (Exception ex) {
                                log.error("PaymentsSeeder: error seeding payment #{}: {}", idx, ex.getMessage());
                        }
                }

        // Add targeted seeds for July (7) and August (8) so charts show those months
        int addPerMonth = 10; // number of extra payments per month
        for (int m : new int[]{7, 8}) {
            for (int k = 1; k <= addPerMonth; k++) {
                final int mm = m;
                final int kk = k;
                String payCode = String.format("SEEDPAY_M%d_%03d", mm, kk);
                if (paymentRepository.findByPaymentCode(payCode).isPresent()) continue;
                try {
                    String uname = String.format("seed_patient_m%d_%03d", mm, kk);
                    User patientUser = userRepository.findByUsername(uname).orElseGet(() -> userRepository.save(User.builder()
                            .username(uname)
                            .email(uname + "@example.com")
                            .passwordHash(passwordEncoder.encode("Patient123!"))
                            .fullName("Seed Patient " + uname)
                            .phone(String.format("0902%04d", (mm * 1000 + kk)))
                            .role(patientRole)
                            .isActive(true)
                            .build()));

                    Patient patient = patientRepository.findByUser_UserId(patientUser.getUserId()).orElseGet(() -> patientRepository.save(Patient.builder()
                            .user(patientUser)
                            .patientCode(String.format("PM%02d%03d", mm, kk))
                            .bloodType("UNKNOWN")
                            .medicalHistory("seeded-month-specific")
                            .build()));

                    String apptCode = String.format("SDAPPT_M%d_%03d", mm, kk);
                    int day = Math.min(20, (kk % 28) + 1);
                    Appointment appt = appointmentRepository.findByAppointmentCode(apptCode).orElseGet(() -> appointmentRepository.save(Appointment.builder()
                            .appointmentCode(apptCode)
                            .patient(patient)
                            .doctor(doctor)
                            .appointmentDate(LocalDate.of(year, mm, day))
                            .appointmentTime(LocalTime.of(9 + (kk % 8), 0))
                            .reason("seed-month")
                            .status("CONFIRMED")
                            .build()));

                    String recordCode = String.format("SEEDREC_M%d_%03d", mm, kk);
                    MedicalRecord record = medicalRecordRepository.findByRecordCode(recordCode).orElseGet(() -> medicalRecordRepository.save(MedicalRecord.builder()
                            .recordCode(recordCode)
                            .appointment(appt)
                            .patient(patient)
                            .doctor(doctor)
                            .symptoms("symptom")
                            .finalDiagnosis("diagnosis")
                            .build()));

                    String invCode = String.format("SEEDINV_M%d_%03d", mm, kk);
                    BigDecimal consult = new BigDecimal(120000 + ((kk % 5) * 10000));
                    BigDecimal med = new BigDecimal(60000 + ((kk % 3) * 5000));
                    Invoice invoice = invoiceRepository.findByInvoiceCode(invCode).orElseGet(() -> invoiceRepository.save(Invoice.builder()
                            .invoiceCode(invCode)
                            .medicalRecord(record)
                            .patient(patient)
                            .consultationFee(consult)
                            .medicationFee(med)
                            .otherFee(BigDecimal.ZERO)
                            .discount(BigDecimal.ZERO)
                            .paymentStatus("PAID")
                            .build()));

                    // create payment for that invoice with paidAt in target month
                    LocalDateTime paidAt = LocalDateTime.of(LocalDate.of(year, mm, Math.min(day, 28)), LocalTime.of(10 + (kk % 6), 15));
                    BigDecimal total = invoice.getTotalAmount() != null ? invoice.getTotalAmount() : invoice.getConsultationFee().add(invoice.getMedicationFee()).add(invoice.getOtherFee()).subtract(invoice.getDiscount());
                    Payment p = Payment.builder()
                            .paymentCode(payCode)
                            .invoice(invoice)
                            .amount(total)
                            .paymentMethod(kk % 2 == 0 ? "VNPAY" : "CASH")
                            .transactionRef("TXN-SEED-M" + mm + "-" + System.currentTimeMillis() + "-" + kk)
                            .status("SUCCESS")
                            .paidAt(paidAt)
                            .build();
                    Payment saved = paymentRepository.save(p);
                    log.info("Created month-specific payment {} (month {})", saved.getPaymentCode(), mm);
                } catch (Exception ex) {
                    log.error("PaymentsSeeder: error seeding month {} payment #{}: {}", mm, kk, ex.getMessage());
                }
            }
        }

        log.info("PaymentsSeeder finished. Total payments now: {}", paymentRepository.count());
    }
}
