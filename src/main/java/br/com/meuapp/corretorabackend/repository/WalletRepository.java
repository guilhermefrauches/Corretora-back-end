package br.com.meuapp.corretorabackend.repository;

import br.com.meuapp.corretorabackend.model.Wallet;
import br.com.meuapp.corretorabackend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, Long> {

    Optional<Wallet> findByUser(User user);
}