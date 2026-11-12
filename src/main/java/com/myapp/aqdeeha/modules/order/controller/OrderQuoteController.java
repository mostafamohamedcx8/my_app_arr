package com.myapp.aqdeeha.modules.order.controller;

import com.myapp.aqdeeha.modules.order.dto.QuoteDtos.QuoteRequest;
import com.myapp.aqdeeha.modules.order.dto.QuoteDtos.QuoteResponse;
import com.myapp.aqdeeha.modules.order.service.OrderQuoteService;
import com.myapp.aqdeeha.security.access.AllowTo;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.myapp.aqdeeha.security.AuthPrincipal.Role.CUSTOMER;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderQuoteController {

    private final OrderQuoteService quoteService;

    /** Fee (shops) or allowed fee range + road distance (anything) before placing an order. */
    @PostMapping("/quote")
    @AllowTo(CUSTOMER)
    public QuoteResponse quote(@Valid @RequestBody QuoteRequest request) {
        return quoteService.quote(request);
    }
}
