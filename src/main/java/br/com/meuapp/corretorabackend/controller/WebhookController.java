package br.com.meuapp.corretorabackend.controller;

import br.com.meuapp.corretorabackend.service.WalletService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/stripe")
@RequiredArgsConstructor
public class WebhookController {

    @Value("${stripe.webhook-secret}")
    private String webhookSecret;

    private final WalletService walletService;

    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {

        Event event;

        try {
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            return ResponseEntity.badRequest().body("Assinatura inválida");
        }

        if ("payment_intent.succeeded".equals(event.getType())) {
            try {
                com.stripe.model.StripeObject stripeObject = event
                        .getDataObjectDeserializer()
                        .deserializeUnsafe();

                if (stripeObject instanceof PaymentIntent intent) {
                    String email = intent.getMetadata().get("email");
                    BigDecimal amount = BigDecimal.valueOf(intent.getAmount())
                            .divide(BigDecimal.valueOf(100));

                    System.out.println("Webhook recebido - email: " + email + " valor: " + amount);

                    if (email != null && !email.isEmpty()) {
                        walletService.creditFromStripe(email, amount, intent.getId());
                        System.out.println("Wallet creditada com sucesso!");
                    }
                }
            } catch (Exception e) {
                System.out.println("Erro no webhook: " + e.getMessage());
                e.printStackTrace();
                return ResponseEntity.status(500).body("Erro: " + e.getMessage());
            }
        }

        return ResponseEntity.ok("Webhook recebido");
    }
}