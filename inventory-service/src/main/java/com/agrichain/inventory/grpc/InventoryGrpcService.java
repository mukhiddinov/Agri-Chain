package com.agrichain.inventory.grpc;

import com.agrichain.inventory.grpc.CheckAvailabilityRequest;
import com.agrichain.inventory.grpc.CheckAvailabilityResponse;
import com.agrichain.inventory.grpc.InventoryServiceGrpc;
import com.agrichain.inventory.grpc.ReleaseInventoryRequest;
import com.agrichain.inventory.grpc.ReleaseInventoryResponse;
import com.agrichain.inventory.grpc.ReserveInventoryRequest;
import com.agrichain.inventory.grpc.ReserveInventoryResponse;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.UUID;

@GrpcService
@Slf4j
public class InventoryGrpcService extends InventoryServiceGrpc.InventoryServiceImplBase {

    @Override
    public void reserveInventory(ReserveInventoryRequest request, StreamObserver<ReserveInventoryResponse> responseObserver) {
        log.info("Received reserve inventory request for order: {}", request.getOrderId());
        
        // TODO: Implement actual inventory reservation logic
        String reservationId = UUID.randomUUID().toString();
        
        ReserveInventoryResponse response = ReserveInventoryResponse.newBuilder()
                .setSuccess(true)
                .setReservationId(reservationId)
                .setMessage("Inventory reserved successfully (stub)")
                .build();
        
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void releaseInventory(ReleaseInventoryRequest request, StreamObserver<ReleaseInventoryResponse> responseObserver) {
        log.info("Received release inventory request for reservation: {}", request.getReservationId());
        
        // TODO: Implement actual inventory release logic
        ReleaseInventoryResponse response = ReleaseInventoryResponse.newBuilder()
                .setSuccess(true)
                .setMessage("Inventory released successfully (stub)")
                .build();
        
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void checkAvailability(CheckAvailabilityRequest request, StreamObserver<CheckAvailabilityResponse> responseObserver) {
        log.info("Checking availability for product: {}", request.getProductId());
        
        // TODO: Implement actual availability check
        CheckAvailabilityResponse response = CheckAvailabilityResponse.newBuilder()
                .setAvailable(true)
                .setAvailableQuantity(100)
                .build();
        
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
