package br.com.meuapp.corretorabackend.controller;

import br.com.meuapp.corretorabackend.dto.ConfirmDepositRequest;
import br.com.meuapp.corretorabackend.dto.DepositRequest;
import br.com.meuapp.corretorabackend.dto.WalletResponse;
import br.com.meuapp.corretorabackend.dto.WithdrawRequest;
import br.com.meuapp.corretorabackend.service.WalletService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    @GetMapping
    public ResponseEntity<WalletResponse> getWallet(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(walletService.getWallet(userDetails.getUsername()));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/deposit")
    public ResponseEntity<WalletResponse> deposit(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody DepositRequest request) {
        return ResponseEntity.ok(
                walletService.deposit(userDetails.getUsername(), request));
    }

    @PostMapping("/withdraw")
    public ResponseEntity<WalletResponse> withdraw(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody WithdrawRequest request) {
        return ResponseEntity.ok(
                walletService.withdraw(userDetails.getUsername(), request));
    }

    @PostMapping("/confirm-deposit")
    public ResponseEntity<WalletResponse> confirmDeposit(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ConfirmDepositRequest request) {
        return ResponseEntity.ok(
                walletService.confirmDeposit(userDetails.getUsername(), request));
    }
}