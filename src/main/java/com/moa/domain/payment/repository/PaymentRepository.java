package com.moa.domain.payment.repository;

import com.moa.domain.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    @Query(value = "SELECT * FROM payments " +
            "WHERE order_id = :orderId",
            nativeQuery = true)
    Optional<Payment> findByOrderId(@Param("orderId") Long orderId);
}