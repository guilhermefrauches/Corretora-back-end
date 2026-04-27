package br.com.meuapp.corretorabackend.dto;

import lombok.Data;

@Data
public class UpdateProfileRequest {

    private String name;
    private String email;
    private String currentPassword;
    private String newPassword;
}
