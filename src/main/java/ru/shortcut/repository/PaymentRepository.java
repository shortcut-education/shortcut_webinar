package ru.shortcut.repository;

import org.springframework.stereotype.Repository;

@Repository
public interface PaymentRepository {
    void save(Payment payment);
}
