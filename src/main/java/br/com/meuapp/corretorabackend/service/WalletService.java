package br.com.meuapp.corretorabackend.service;

import br.com.meuapp.corretorabackend.dto.ConfirmDepositRequest;
import br.com.meuapp.corretorabackend.dto.DepositRequest;
import br.com.meuapp.corretorabackend.dto.WalletResponse;
import br.com.meuapp.corretorabackend.dto.WithdrawRequest;
import br.com.meuapp.corretorabackend.model.User;
import br.com.meuapp.corretorabackend.model.Wallet;
import br.com.meuapp.corretorabackend.model.WalletTransaction;
import br.com.meuapp.corretorabackend.repository.UserRepository;
import br.com.meuapp.corretorabackend.repository.WalletRepository;
import br.com.meuapp.corretorabackend.repository.WalletTransactionRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
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
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));

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
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));

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

    @Transactional
    public void creditFromStripe(String email, BigDecimal amount, String stripePaymentId) {
        if (transactionRepository.existsByStripePaymentId(stripePaymentId)) {
            return;
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));

        Wallet wallet = getOrCreateWallet(user);
        wallet.setBalance(wallet.getBalance().add(amount));
        walletRepository.save(wallet);

        WalletTransaction transaction = new WalletTransaction();
        transaction.setWallet(wallet);
        transaction.setType(WalletTransaction.TransactionType.DEPOSIT);
        transaction.setAmount(amount);
        transaction.setDescription("Depósito via Stripe - " + stripePaymentId);
        transaction.setStripePaymentId(stripePaymentId);
        transactionRepository.save(transaction);
    }

    @Transactional
    public WalletResponse confirmDeposit(String email, ConfirmDepositRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));

        PaymentIntent intent;
        try {
            intent = PaymentIntent.retrieve(request.getPaymentIntentId());
        } catch (StripeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "PaymentIntent inválido: " + e.getMessage());
        }

        if (!"succeeded".equals(intent.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Pagamento não confirmado. Status: " + intent.getStatus());
        }

        String emailNoMetadata = intent.getMetadata().get("email");
        if (!email.equals(emailNoMetadata)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "PaymentIntent não pertence ao usuário autenticado");
        }

        if (transactionRepository.existsByStripePaymentId(intent.getId())) {
            return buildResponse(getOrCreateWallet(user));
        }

        BigDecimal amount = BigDecimal.valueOf(intent.getAmount()).divide(BigDecimal.valueOf(100));
        Wallet wallet = getOrCreateWallet(user);
        wallet.setBalance(wallet.getBalance().add(amount));
        walletRepository.save(wallet);

        WalletTransaction transaction = new WalletTransaction();
        transaction.setWallet(wallet);
        transaction.setType(WalletTransaction.TransactionType.DEPOSIT);
        transaction.setAmount(amount);
        transaction.setDescription("Depósito via Stripe - " + intent.getId());
        transaction.setStripePaymentId(intent.getId());
        transactionRepository.save(transaction);

        return buildResponse(wallet);
    }

    @Transactional
    public WalletResponse withdraw(String email, WithdrawRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));

        Wallet wallet = getOrCreateWallet(user);

        if (wallet.getBalance().compareTo(request.getAmount()) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Saldo insuficiente");
        }

        wallet.setBalance(wallet.getBalance().subtract(request.getAmount()));
        walletRepository.save(wallet);

        WalletTransaction transaction = new WalletTransaction();
        transaction.setWallet(wallet);
        transaction.setType(WalletTransaction.TransactionType.WITHDRAW);
        transaction.setAmount(request.getAmount());
        transaction.setDescription(request.getDescription() != null
                ? request.getDescription() : "Saque");
        transactionRepository.save(transaction);

        return buildResponse(wallet);
    }
}
