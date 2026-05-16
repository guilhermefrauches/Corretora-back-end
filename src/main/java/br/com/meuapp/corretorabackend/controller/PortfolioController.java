package br.com.meuapp.corretorabackend.controller;

import br.com.meuapp.corretorabackend.dto.BuyRequest;
import br.com.meuapp.corretorabackend.dto.PortfolioHistoryResponse;
import br.com.meuapp.corretorabackend.dto.PortfolioResponse;
import br.com.meuapp.corretorabackend.dto.SellRequest;
import br.com.meuapp.corretorabackend.service.PortfolioHistoryService;
import br.com.meuapp.corretorabackend.service.PortfolioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/portfolio")
@RequiredArgsConstructor
public class PortfolioController {

    private final PortfolioService portfolioService;
    private final PortfolioHistoryService portfolioHistoryService;

    @GetMapping
    public ResponseEntity<PortfolioResponse> getPortfolio(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(portfolioService.getPortfolio(userDetails.getUsername()));
    }

    @PostMapping("/buy")
    public ResponseEntity<PortfolioResponse> buy(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody BuyRequest request) {
        return ResponseEntity.ok(portfolioService.buy(userDetails.getUsername(), request));
    }

    @PostMapping("/sell")
    public ResponseEntity<PortfolioResponse> sell(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody SellRequest request) {
        return ResponseEntity.ok(portfolioService.sell(userDetails.getUsername(), request));
    }

    @GetMapping("/history")
    public ResponseEntity<PortfolioHistoryResponse> history(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "3M") String period) {
        return ResponseEntity.ok(portfolioHistoryService.getHistory(userDetails.getUsername(), period));
    }
}
