package br.com.meuapp.corretorabackend.repository;

import br.com.meuapp.corretorabackend.model.Position;
import br.com.meuapp.corretorabackend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PositionRepository extends JpaRepository<Position, Long> {
    List<Position> findByUser(User user);
    Optional<Position> findByUserAndTicker(User user, String ticker);
    List<Position> findByTicker(String ticker);

    @Query("SELECT DISTINCT p.ticker FROM Position p")
    List<String> findDistinctTickers();
}