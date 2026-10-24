package com.customer.model.DTO;

import java.util.List;

import com.customer.model.Customer;

public record CustomerPage(List<Customer> content, long totalElements, int totalPages, int number, int size) {
}
