package com.customer.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.customer.model.Customer;
import com.customer.repository.CustomerRepository;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private CustomerService customerService;

    private final Pageable pageable = PageRequest.of(0, 20);

    @Test
    void blankQueryReturnsAllCustomersPaged() {
        Page<Customer> expected = new PageImpl<>(List.of(new Customer()));
        when(customerRepository.findAll(pageable)).thenReturn(expected);

        assertThat(customerService.searchCustomers("  ", pageable)).isSameAs(expected);
        assertThat(customerService.searchCustomers(null, pageable)).isSameAs(expected);
        verify(customerRepository, times(2)).findAll(pageable);
        verifyNoMoreInteractions(customerRepository);
    }

    @Test
    void queryIsTrimmedBeforeSearching() {
        Page<Customer> expected = new PageImpl<>(List.of());
        when(customerRepository.search("ann", pageable)).thenReturn(expected);

        assertThat(customerService.searchCustomers("  ann ", pageable)).isSameAs(expected);
        verify(customerRepository).search("ann", pageable);
        verifyNoMoreInteractions(customerRepository);
    }
}
