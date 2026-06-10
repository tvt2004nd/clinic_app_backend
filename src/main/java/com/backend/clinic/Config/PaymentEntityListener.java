package com.backend.clinic.Config;

import com.backend.clinic.Entity.Payment;
import com.backend.clinic.Service.StatsUpdaterService;

import jakarta.persistence.PostPersist;
import jakarta.persistence.PostUpdate;

public class PaymentEntityListener {

    @PostPersist
    public void postPersist(Payment payment) {
        try {
            StatsUpdaterService svc = SpringContext.getBean(StatsUpdaterService.class);
            svc.recordPayment(payment);
        } catch (Exception ex) {
            // logging is not available here; swallow to avoid breaking transaction
        }
    }

    @PostUpdate
    public void postUpdate(Payment payment) {
        try {
            StatsUpdaterService svc = SpringContext.getBean(StatsUpdaterService.class);
            svc.recordPayment(payment);
        } catch (Exception ex) {
            // ignore
        }
    }
}
