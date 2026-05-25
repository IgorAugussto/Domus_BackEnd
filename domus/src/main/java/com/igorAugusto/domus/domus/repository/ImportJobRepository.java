package com.igorAugusto.domus.domus.repository;

import com.igorAugusto.domus.domus.entity.ImportJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ImportJobRepository extends JpaRepository<ImportJob, String> {
    // JpaRepository já fornece: findById, save, deleteById, existsById, etc.
    // O ID aqui é String (UUID gerado pelo Java).
}
