package br.com.meuapp.corretorabackend.repository;

import br.com.meuapp.corretorabackend.model.Trade;
import br.com.meuapp.corretorabackend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TradeRepository extends JpaRepository<Trade, Long> {

    List<Trade> findByUserOrderByExecutedAtAsc(User user);
}
