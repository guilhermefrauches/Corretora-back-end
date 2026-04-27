package br.com.meuapp.corretorabackend.service;

import br.com.meuapp.corretorabackend.dto.AuthResponse;
import br.com.meuapp.corretorabackend.dto.LoginRequest;
import br.com.meuapp.corretorabackend.dto.RegisterRequest;
import br.com.meuapp.corretorabackend.dto.UpdateProfileRequest;
import br.com.meuapp.corretorabackend.model.Role;
import br.com.meuapp.corretorabackend.model.User;
import br.com.meuapp.corretorabackend.repository.UserRepository;
import br.com.meuapp.corretorabackend.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "E-mail já cadastrado");
        }

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.USER);

        userRepository.save(user);

        String token = jwtService.generateToken(user);

        return new AuthResponse(user.getId(), token, user.getName(), user.getEmail(), user.getRole().name());
    }

    public AuthResponse updateMe(String email, UpdateProfileRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));

        if (request.getNewPassword() != null) {
            if (request.getCurrentPassword() == null ||
                    !passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Senha atual incorreta");
            }
            user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        }

        if (request.getName() != null) {
            user.setName(request.getName());
        }

        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "E-mail já cadastrado");
            }
            user.setEmail(request.getEmail());
        }

        userRepository.save(user);

        return new AuthResponse(user.getId(), null, user.getName(), user.getEmail(), user.getRole().name());
    }

    public AuthResponse me(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));
        return new AuthResponse(user.getId(), null, user.getName(), user.getEmail(), user.getRole().name());
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));

        String token = jwtService.generateToken(user);

        return new AuthResponse(user.getId(), token, user.getName(), user.getEmail(), user.getRole().name());
    }
}