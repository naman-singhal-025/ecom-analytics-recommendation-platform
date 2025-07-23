package com.ecommerce.ecom_backend.repo.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ecommerce.ecom_backend.model.Orders;

/**
 * Spring Data repository for {@link Orders} entities.
 */
public interface OrderRepository extends JpaRepository<Orders, Long> {
}