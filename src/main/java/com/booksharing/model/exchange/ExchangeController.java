package com.booksharing.model.exchange;

import com.booksharing.model.user.User;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/exchanges")
@RequiredArgsConstructor
public class ExchangeController {

    private final ExchangeService exchangeService;

    @GetMapping("/{id}")
    public ExchangeResponse getById(@PathVariable UUID id) {
        return exchangeService.getById(id);
    }

    @GetMapping("/my")
    public List<ExchangeResponse> getMyExchanges(@AuthenticationPrincipal User currentUser) {
        return exchangeService.getMyExchanges(currentUser.getId());
    }

    @PostMapping("/{id}/handover-photos")
    public ExchangeResponse addHandoverPhoto(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody AddExchangePhotoRequest request) {
        return exchangeService.addHandoverPhoto(id, currentUser.getId(), request);
    }

    @PostMapping("/{id}/confirm-received")
    public ExchangeResponse confirmReceived(@PathVariable UUID id, @AuthenticationPrincipal User currentUser) {
        return exchangeService.confirmReceived(id, currentUser.getId());
    }

    @PostMapping("/{id}/return-photos")
    public ExchangeResponse addReturnPhoto(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody AddExchangePhotoRequest request) {
        return exchangeService.addReturnPhoto(id, currentUser.getId(), request);
    }

    @PostMapping("/{id}/confirm-return")
    public ExchangeResponse confirmReturn(@PathVariable UUID id, @AuthenticationPrincipal User currentUser) {
        return exchangeService.confirmReturn(id, currentUser.getId());
    }

    @PostMapping("/{id}/extension-requests")
    public ExchangeResponse requestExtension(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody CreateExtensionRequest request) {
        return exchangeService.requestExtension(id, currentUser.getId(), request);
    }

    @PatchMapping("/{id}/extension-requests/{extensionId}")
    public ExchangeResponse decideExtension(
            @PathVariable UUID id,
            @PathVariable UUID extensionId,
            @AuthenticationPrincipal User currentUser,
            @RequestParam boolean approve) {
        return exchangeService.decideExtension(id, extensionId, currentUser.getId(), approve);
    }

    @PostMapping("/{id}/shipment")
    public ExchangeResponse createShipment(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody CreateShipmentRequest request) {
        return exchangeService.createShipment(id, currentUser.getId(), request);
    }

    @PatchMapping("/{id}/shipment/{shipmentId}/ship")
    public ExchangeResponse shipWaybill(
            @PathVariable UUID id,
            @PathVariable UUID shipmentId,
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody ShipWaybillRequest request) {
        return exchangeService.shipWaybill(id, shipmentId, currentUser.getId(), request);
    }

    @PatchMapping("/{id}/shipment/{shipmentId}/delivered")
    public ExchangeResponse confirmDelivered(
            @PathVariable UUID id,
            @PathVariable UUID shipmentId,
            @AuthenticationPrincipal User currentUser) {
        return exchangeService.confirmDelivered(id, shipmentId, currentUser.getId());
    }
}