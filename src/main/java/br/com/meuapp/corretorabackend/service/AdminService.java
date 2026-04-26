package br.com.meuapp.corretorabackend.service;

import br.com.meuapp.corretorabackend.dto.AuthResponse;
import br.com.meuapp.corretorabackend.dto.WalletResponse;
import br.com.meuapp.corretorabackend.model.Role;
import br.com.meuapp.corretorabackend.model.User;
import br.com.meuapp.corretorabackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final WalletService walletService;

    public List<AuthResponse> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(user -> new AuthResponse(
                        user.getId(),
                        null,
                        user.getName(),
                        user.getEmail(),
                        user.getRole().name()
                ))
                .toList();
    }

    public WalletResponse getUserWallet(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        return walletService.getWallet(user.getEmail());
    }

    public AuthResponse promoteToAdmin(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        user.setRole(Role.ADMIN);
        userRepository.save(user);

        return new AuthResponse(
                user.getId(),
                null,
                user.getName(),
                user.getEmail(),
                user.getRole().name()
        );
    }
}