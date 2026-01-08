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

    List<Investments> findByUserId(Long userId);

    List<Investments> findByUserIdAndCreatedAtBetween(Long userId, LocalDate start, LocalDate end);

    @Query("SELECT SUM(i.value) FROM Investments i WHERE i.user.id = :userId")
    BigDecimal sumByUserId(@Param("userId") Long userId);

    List<Investments> findAllByUserId(Long userId);

    // Soma investimentos APENAS do mês exato (usado no monthly summary)
    @Query("""
                SELECT COALESCE(SUM(i.value), 0)
                FROM Investments i
                WHERE i.user.id = :userId
                  AND (YEAR(i.createdAt) * 100 + MONTH(i.createdAt)) = :yearMonth
            """)
    BigDecimal sumInvestmentsByExactMonth(
            @Param("userId") Long userId,
            @Param("yearMonth") int yearMonth
    );

    // Opcional: soma acumulada de todos os aportes até o mês
    @Query("""
                SELECT COALESCE(SUM(i.value), 0)
                FROM Investments i
                WHERE i.user.id = :userId
                  AND (YEAR(i.createdAt) * 100 + MONTH(i.createdAt)) <= :yearMonth
            """)
    BigDecimal sumInvestmentsUntilMonth(
            @Param("userId") Long userId,
            @Param("yearMonth") int yearMonth
    );
}