package com.igorAugusto.domus.domus.repository;

import com.igorAugusto.domus.domus.entity.CreditCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CreditCardRepository extends JpaRepository<CreditCard, Long> {

    List<CreditCard> findByUserIdAndActiveTrueOrderByCreatedAtDesc(Long userId);
}
