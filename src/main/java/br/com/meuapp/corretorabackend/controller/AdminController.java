package br.com.meuapp.corretorabackend.controller;

import br.com.meuapp.corretorabackend.dto.AuthResponse;
import br.com.meuapp.corretorabackend.dto.WalletResponse;
import br.com.meuapp.corretorabackend.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AuthResponse>> getAllUsers() {
        return ResponseEntity.ok(adminService.getAllUsers());
    }

    @GetMapping("/users/{id}/wallet")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<WalletResponse> getUserWallet(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.getUserWallet(id));
    }

    @PutMapping("/users/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AuthResponse> promoteToAdmin(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.promoteToAdmin(id));
    }
}