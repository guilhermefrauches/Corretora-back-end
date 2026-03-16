package br.com.meuapp.corretorabackend.controller;

import br.com.meuapp.corretorabackend.dto.PaymentIntentRequest;
import br.com.meuapp.corretorabackend.dto.PaymentIntentResponse;
import br.com.meuapp.corretorabackend.service.StripeService;
import br.com.meuapp.corretorabackend.service.WalletService;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/stripe")
@RequiredArgsConstructor
public class StripeController {

    private final StripeService stripeService;
    private final WalletService walletService;

    @PostMapping("/create-payment-intent")
    public ResponseEntity<PaymentIntentResponse> createPaymentIntent(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody PaymentIntentRequest request) throws StripeException {

        PaymentIntent intent = stripeService.createPaymentIntent(
                request.getAmount(),
                request.getCurrency(),
                userDetails.getUsername());

        return ResponseEntity.ok(new PaymentIntentResponse(
                intent.getClientSecret(),
                stripeService.getPublicKey(),
                intent.getId(),
                intent.getAmount(),
                intent.getCurrency(),
                intent.getStatus()
        ));
    }
}