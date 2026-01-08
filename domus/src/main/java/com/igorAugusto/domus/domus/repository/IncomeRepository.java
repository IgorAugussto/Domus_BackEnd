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

    List<Income> findByUserId(Long userId);

    List<Income> findByUserIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            Long userId, LocalDate end, LocalDate start);

    @Query("""
                SELECT COALESCE(SUM(i.value), 0)
                FROM Income i
                WHERE i.user.id = :userId
                  AND i.startDate <= :date
                  AND (i.endDate IS NULL OR i.endDate >= :date)
            """)
    BigDecimal sumMonthlyIncome(@Param("userId") Long userId, @Param("date") LocalDate date);

    @Query("SELECT SUM(i.value) FROM Income i WHERE i.user.id = :userId")
    BigDecimal sumByUserId(@Param("userId") Long userId);

    List<Income> findAllByUserId(Long userId);

    // ESSA É A QUERY PRINCIPAL QUE VOCÊ JÁ USA NO SERVICE
    @Query("""
                SELECT COALESCE(SUM(i.value), 0)
                FROM Income i
                WHERE i.user.id = :userId
                  AND (YEAR(i.startDate) * 100 + MONTH(i.startDate)) <= :yearMonth
            """)
    BigDecimal sumIncomeUntilMonth(
            @Param("userId") Long userId,
            @Param("yearMonth") int yearMonth
    );

    // OPCIONAL: soma apenas do mês exato (útil para projeções ou depuração)
    @Query("""
                SELECT COALESCE(SUM(i.value), 0)
                FROM Income i
                WHERE i.user.id = :userId
                  AND (YEAR(i.startDate) * 100 + MONTH(i.startDate)) = :yearMonth
            """)
    BigDecimal sumIncomeByExactMonth(
            @Param("userId") Long userId,
            @Param("yearMonth") int yearMonth
    );
}