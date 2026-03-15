package br.com.meuapp.corretorabackend.service;

import br.com.meuapp.corretorabackend.dto.DepositRequest;
import br.com.meuapp.corretorabackend.dto.WalletResponse;
import br.com.meuapp.corretorabackend.model.User;
import br.com.meuapp.corretorabackend.model.Wallet;
import br.com.meuapp.corretorabackend.model.WalletTransaction;
import br.com.meuapp.corretorabackend.repository.UserRepository;
import br.com.meuapp.corretorabackend.repository.WalletRepository;
import br.com.meuapp.corretorabackend.repository.WalletTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository transactionRepository;
    private final UserRepository userRepository;

    public Wallet getOrCreateWallet(User user) {
        return walletRepository.findByUser(user)
                .orElseGet(() -> {
                    Wallet wallet = new Wallet();
                    wallet.setUser(user);
                    return walletRepository.save(wallet);
                });
    }

    @Transactional
    public WalletResponse deposit(String email, DepositRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        Wallet wallet = getOrCreateWallet(user);
        wallet.setBalance(wallet.getBalance().add(request.getAmount()));
        walletRepository.save(wallet);

        WalletTransaction transaction = new WalletTransaction();
        transaction.setWallet(wallet);
        transaction.setType(WalletTransaction.TransactionType.DEPOSIT);
        transaction.setAmount(request.getAmount());
        transaction.setDescription(request.getDescription() != null
                ? request.getDescription() : "Depósito");
        transactionRepository.save(transaction);

        return buildResponse(wallet);
    }

    public WalletResponse getWallet(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        Wallet wallet = getOrCreateWallet(user);
        return buildResponse(wallet);
    }

    private WalletResponse buildResponse(Wallet wallet) {
        List<WalletResponse.TransactionResponse> transactions = transactionRepository
                .findByWalletOrderByCreatedAtDesc(wallet)
                .stream()
                .map(t -> new WalletResponse.TransactionResponse(
                        t.getId(),
                        t.getType(),
                        t.getAmount(),
                        t.getDescription(),
                        t.getCreatedAt()
                ))
                .toList();

        return new WalletResponse(
                wallet.getId(),
                wallet.getBalance(),
                wallet.getCreatedAt(),
                transactions
        );
    }
}