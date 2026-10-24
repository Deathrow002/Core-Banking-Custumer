package com.customer.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.customer.model.Customer;

import lombok.NonNull;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, UUID> {
    @Query("""
            select c from Customer c
            where lower(c.firstName) like lower(concat('%', :query, '%'))
               or lower(c.lastName) like lower(concat('%', :query, '%'))
               or lower(c.email) like lower(concat('%', :query, '%'))
               or c.nationalId like concat('%', :query, '%')
            """)
    Page<Customer> search(@Param("query") String query, Pageable pageable);
    boolean existsByCustomerId(@NonNull UUID customerId);
    Optional<Customer> findByEmail(@NonNull String email);
    boolean existsByNationalId(@NonNull String nationalId);
    boolean existsByEmail(@NonNull String email);
    boolean existsByPhoneNumber(@NonNull String phoneNumber);
}