package com.logistics.service;

import com.logistics.dto.request.DriverRequest;
import com.logistics.dto.response.DriverResponse;
import com.logistics.dto.response.PageResponse;
import com.logistics.entity.enums.DriverAvailability;

import java.util.List;

public interface DriverService {

    PageResponse<DriverResponse> list(String search, DriverAvailability availability, int page, int size, String sort);

    /** AVAILABLE drivers whose license is still valid. */
    List<DriverResponse> available();

    DriverResponse get(Long id);

    DriverResponse create(DriverRequest request);

    DriverResponse update(Long id, DriverRequest request);

    DriverResponse changeAvailability(Long id, DriverAvailability availability);
}
