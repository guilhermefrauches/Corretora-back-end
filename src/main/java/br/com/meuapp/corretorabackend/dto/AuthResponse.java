package br.com.meuapp.corretorabackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthResponse {

    private String token;
    private String name;
    private String email;
    private String role;
}