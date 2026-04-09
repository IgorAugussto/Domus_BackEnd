package com.igorAugusto.domus.domus.repository;

import com.igorAugusto.domus.domus.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentStatusRepository extends JpaRepository<PaymentStatus, Long> {

    // Busca o status de uma despesa em um mês específico
    Optional<PaymentStatus> findByOutgoingIdAndYearMonth(Long outgoingId, String yearMonth);

    // Busca todos os status de um usuário em um mês específico
    @Query("""
        SELECT ps FROM PaymentStatus ps
        WHERE ps.outgoing.user.id = :userId
          AND ps.yearMonth = :yearMonth
    """)
    List<PaymentStatus> findByUserIdAndYearMonth(
            @Param("userId") Long userId,
            @Param("yearMonth") String yearMonth
    );
}