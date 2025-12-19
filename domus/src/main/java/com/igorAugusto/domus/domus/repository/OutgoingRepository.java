package com.igorAugusto.domus.domus.repository;

import com.igorAugusto.domus.domus.entity.Outgoing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface OutgoingRepository extends JpaRepository<Outgoing, Long> {

    List<Outgoing> findByUserId(Long userId);

    @Query("SELECT SUM(o.value) FROM Outgoing o WHERE o.user.id = :userId")
    BigDecimal sumByUserId(@Param("userId") Long userId);

    List<Outgoing> findAllByUserId(Long userId);
}
