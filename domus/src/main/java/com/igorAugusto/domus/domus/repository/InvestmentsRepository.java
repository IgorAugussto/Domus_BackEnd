package com.igorAugusto.domus.domus.repository;

import com.igorAugusto.domus.domus.entity.Investments;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface InvestmentsRepository extends JpaRepository<Investments, Long> {

    // Busca todas as receitas de um usuário
    List<Investments> findByUserId(Long userId);

    // Busca receitas de um usuário em um período
    List<Investments> findByUserIdAndCreatedAtBetween(Long userId, LocalDate start, LocalDate end);

    // Soma total de receitas de um usuário
    @Query("SELECT SUM(i.value) FROM Investments i WHERE i.user.id = :userId")
    BigDecimal sumByUserId(@Param("userId") Long userId);

    List<Investments> findAllByUserId(Long userId);

    @Query("""
        SELECT COALESCE(SUM(i.value), 0)
        FROM Investments i
        WHERE i.user.id = :userId
          AND i.startDate BETWEEN :start AND :end
    """)
    BigDecimal sumByUserIdAndStartDateBetween(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );



}
