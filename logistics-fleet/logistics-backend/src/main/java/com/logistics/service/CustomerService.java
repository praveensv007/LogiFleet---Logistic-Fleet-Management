package com.logistics.service;

import com.logistics.dto.request.CustomerUpdateRequest;
import com.logistics.dto.response.CustomerResponse;
import com.logistics.dto.response.PageResponse;
import com.logistics.dto.response.ShipmentResponse;
import com.logistics.entity.enums.ActiveStatus;
import com.logistics.entity.enums.CustomerType;

import java.util.List;

public interface CustomerService {

    PageResponse<CustomerResponse> list(String search, CustomerType type, ActiveStatus status, int page, int size, String sort);

    /** Staff with CUSTOMER_VIEW, or the customer themself. */
    CustomerResponse get(Long id);

    CustomerResponse me();

    CustomerResponse update(Long id, CustomerUpdateRequest request);

    List<ShipmentResponse> shipments(Long customerId);
}
