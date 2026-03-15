package br.com.meuapp.corretorabackend.repository;

import br.com.meuapp.corretorabackend.model.Wallet;
import br.com.meuapp.corretorabackend.model.WalletTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {

    List<WalletTransaction> findByWalletOrderByCreatedAtDesc(Wallet wallet);
}