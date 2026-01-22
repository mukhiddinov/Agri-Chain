package com.agrichain.inventory.grpc;

import com.agrichain.inventory.entity.InventoryItemEntity;
import com.agrichain.inventory.entity.InventoryReservationEntity;
import com.agrichain.inventory.entity.InventoryReservationItemEntity;
import com.agrichain.inventory.entity.InventoryReservationStatus;
import com.agrichain.inventory.repository.InventoryItemRepository;
import com.agrichain.inventory.repository.InventoryReservationRepository;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@GrpcService
@Slf4j
@RequiredArgsConstructor
public class InventoryGrpcService extends InventoryServiceGrpc.InventoryServiceImplBase {

    private final InventoryItemRepository inventoryItemRepository;
    private final InventoryReservationRepository reservationRepository;

    @Override
    @Transactional
    public void reserveInventory(ReserveInventoryRequest request, StreamObserver<ReserveInventoryResponse> responseObserver) {
        log.info("Received reserve inventory request for order: {}", request.getOrderId());

        try {
            List<InventoryItem> items = request.getItemsList();
            if (items.isEmpty()) {
                ReserveInventoryResponse response = ReserveInventoryResponse.newBuilder()
                        .setSuccess(false)
                        .setMessage("No items provided for reservation")
                        .build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
                return;
            }

            // Validate availability
            for (InventoryItem item : items) {
                Optional<InventoryItemEntity> inventoryOpt = inventoryItemRepository.findByProductId(item.getProductId());
                if (inventoryOpt.isEmpty()) {
                    String message = "Product not found: " + item.getProductId();
                    log.warn(message);
                    ReserveInventoryResponse response = ReserveInventoryResponse.newBuilder()
                            .setSuccess(false)
                            .setMessage(message)
                            .build();
                    responseObserver.onNext(response);
                    responseObserver.onCompleted();
                    return;
                }

                InventoryItemEntity inventoryItem = inventoryOpt.get();
                int reserved = Optional.ofNullable(inventoryItem.getReservedQuantity()).orElse(0);
                int available = inventoryItem.getTotalQuantity() - reserved;
                if (available < item.getQuantity()) {
                    String message = "Insufficient inventory for product: " + item.getProductId();
                    log.warn(message + ", available={}, requested={}", available, item.getQuantity());
                    ReserveInventoryResponse response = ReserveInventoryResponse.newBuilder()
                            .setSuccess(false)
                            .setMessage(message)
                            .build();
                    responseObserver.onNext(response);
                    responseObserver.onCompleted();
                    return;
                }
            }

            // Perform reservation
            String reservationId = UUID.randomUUID().toString();
            InventoryReservationEntity reservation = InventoryReservationEntity.builder()
                    .id(reservationId)
                    .orderId(request.getOrderId())
                    .status(InventoryReservationStatus.ACTIVE)
                    .createdAt(Instant.now())
                    .build();

            List<InventoryReservationItemEntity> reservationItems = items.stream()
                    .map(item -> InventoryReservationItemEntity.builder()
                            .reservation(reservation)
                            .productId(item.getProductId())
                            .quantity(item.getQuantity())
                            .build())
                    .toList();
            reservation.setItems(reservationItems);

            for (InventoryItem item : items) {
                InventoryItemEntity inventoryItem = inventoryItemRepository.findByProductId(item.getProductId()).orElseThrow();
                int reserved = Optional.ofNullable(inventoryItem.getReservedQuantity()).orElse(0);
                inventoryItem.setReservedQuantity(reserved + item.getQuantity());
            }

            reservationRepository.save(reservation);

            ReserveInventoryResponse response = ReserveInventoryResponse.newBuilder()
                    .setSuccess(true)
                    .setReservationId(reservationId)
                    .setMessage("Inventory reserved successfully")
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error reserving inventory", e);
            ReserveInventoryResponse response = ReserveInventoryResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Error reserving inventory: " + e.getMessage())
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }

    @Override
    @Transactional
    public void releaseInventory(ReleaseInventoryRequest request, StreamObserver<ReleaseInventoryResponse> responseObserver) {
        log.info("Received release inventory request for reservation: {}", request.getReservationId());

        try {
            Optional<InventoryReservationEntity> reservationOpt = reservationRepository.findById(request.getReservationId());
            if (reservationOpt.isEmpty()) {
                String message = "Reservation not found: " + request.getReservationId();
                log.warn(message);
                ReleaseInventoryResponse response = ReleaseInventoryResponse.newBuilder()
                        .setSuccess(false)
                        .setMessage(message)
                        .build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
                return;
            }

            InventoryReservationEntity reservation = reservationOpt.get();
            if (reservation.getStatus() != InventoryReservationStatus.ACTIVE) {
                String message = "Reservation already released or inactive: " + request.getReservationId();
                log.warn(message);
                ReleaseInventoryResponse response = ReleaseInventoryResponse.newBuilder()
                        .setSuccess(false)
                        .setMessage(message)
                        .build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
                return;
            }

            for (InventoryReservationItemEntity item : reservation.getItems()) {
                inventoryItemRepository.findByProductId(item.getProductId()).ifPresent(inventoryItem -> {
                    int reserved = Optional.ofNullable(inventoryItem.getReservedQuantity()).orElse(0);
                    int newReserved = Math.max(0, reserved - item.getQuantity());
                    inventoryItem.setReservedQuantity(newReserved);
                });
            }

            reservation.setStatus(InventoryReservationStatus.RELEASED);
            reservationRepository.save(reservation);

            ReleaseInventoryResponse response = ReleaseInventoryResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("Inventory released successfully")
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error releasing inventory", e);
            ReleaseInventoryResponse response = ReleaseInventoryResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Error releasing inventory: " + e.getMessage())
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }

    @Override
    public void checkAvailability(CheckAvailabilityRequest request, StreamObserver<CheckAvailabilityResponse> responseObserver) {
        log.info("Checking availability for product: {}", request.getProductId());

        try {
            Optional<InventoryItemEntity> inventoryOpt = inventoryItemRepository.findByProductId(request.getProductId());

            int availableQuantity = inventoryOpt.map(item -> {
                int reserved = Optional.ofNullable(item.getReservedQuantity()).orElse(0);
                return item.getTotalQuantity() - reserved;
            }).orElse(0);

            boolean available = availableQuantity >= request.getQuantity() && availableQuantity > 0;

            CheckAvailabilityResponse response = CheckAvailabilityResponse.newBuilder()
                    .setAvailable(available)
                    .setAvailableQuantity(availableQuantity)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error checking availability", e);
            CheckAvailabilityResponse response = CheckAvailabilityResponse.newBuilder()
                    .setAvailable(false)
                    .setAvailableQuantity(0)
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }
}
