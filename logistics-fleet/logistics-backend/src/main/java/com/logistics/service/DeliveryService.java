package com.logistics.service;

import com.logistics.dto.request.DeliveryStatusRequest;
import com.logistics.dto.response.AssignmentResponse;
import com.logistics.dto.response.DeliveryResponse;
import com.logistics.entity.enums.ResolutionStatus;

import java.util.List;

/** Driver actions (accept, status, OTP, POD, problems) and the fleet manager's live view. */
public interface DeliveryService {

    List<AssignmentResponse> myAssignments();

    AssignmentResponse accept(Long assignmentId);

    AssignmentResponse reject(Long assignmentId, String reason);

    DeliveryResponse get(Long deliveryId);

    DeliveryResponse updateStatus(Long deliveryId, DeliveryStatusRequest request);

    DeliveryResponse updateLocation(Long deliveryId, DeliveryStatusRequest.Location request);

    DeliveryResponse.OtpResponse sendOtp(Long deliveryId);

    DeliveryResponse complete(Long deliveryId, DeliveryStatusRequest.Complete request);

    DeliveryResponse.ProblemResponse reportProblem(Long deliveryId, DeliveryStatusRequest.Problem request);

    List<DeliveryResponse> active();

    List<DeliveryResponse.ProblemResponse> problems(ResolutionStatus status);

    DeliveryResponse.ProblemResponse resolveProblem(Long problemId, DeliveryStatusRequest.Resolve request);
}
