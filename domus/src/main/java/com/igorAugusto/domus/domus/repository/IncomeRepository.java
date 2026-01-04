package com.igorAugusto.domus.domus.repository;

import com.igorAugusto.domus.domus.entity.Income;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface IncomeRepository extends JpaRepository<Income, Long> {

    // Busca todas as receitas de um usuário
    List<Income> findByUserId(Long userId);

    // Busca receitas de um usuário em um período
    List<Income> findByUserIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            Long userId,
            LocalDate end,
            LocalDate start
    );

    @Query("""
                SELECT COALESCE(SUM(i.value), 0)
                FROM Income i
                WHERE i.user.id = :userId
                  AND i.startDate <= :date
                  AND (i.endDate IS NULL OR i.endDate >= :date)
            """)
    BigDecimal sumMonthlyIncome(
            @Param("userId") Long userId,
            @Param("date") LocalDate date
    );


    // Soma total de receitas de um usuário
    @Query("SELECT SUM(i.value) FROM Income i WHERE i.user.id = :userId")
    BigDecimal sumByUserId(@Param("userId") Long userId);

    List<Income> findAllByUserId(Long userId);

    @Query("""
        SELECT COALESCE(SUM(i.value), 0)
        FROM Income i
        WHERE i.user.id = :userId
          AND i.startDate BETWEEN :start AND :end
    """)
    BigDecimal sumByUserIdAndStartDateBetween(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );


}
