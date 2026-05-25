package com.igorAugusto.domus.domus.entity;

import com.igorAugusto.domus.domus.enums.ImportJobStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Representa um job assíncrono de importação de extrato.
 *
 * Os bytes do arquivo ficam aqui (coluna bytea) para que o consumer do
 * RabbitMQ possa recuperá-los sem precisar de sistema de arquivos compartilhado.
 * O RabbitMQ só carrega o jobId — mensagem pequena e leve.
 */
@Entity
@Table(name = "import_jobs", indexes = {
        @Index(name = "idx_import_jobs_user_email", columnList = "user_email"),
        @Index(name = "idx_import_jobs_status",     columnList = "status")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportJob {

    @Id
    @Column(nullable = false, length = 36)
    private String id;  // UUID gerado pelo Java antes de salvar

    @Column(name = "user_email", nullable = false)
    private String userEmail;

    @Column(name = "due_date", nullable = false, length = 10)
    private String dueDate;

    /** Bytes brutos do arquivo CSV / OFX / PDF enviado pelo usuário. */
    @Column(name = "file_bytes", nullable = false, columnDefinition = "bytea")
    private byte[] fileBytes;

    @Column(name = "original_filename")
    private String originalFilename;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ImportJobStatus status;

    /** JSON com StatementImportResponse (quando DONE) ou { "error": "..." } (quando ERROR). */
    @Column(name = "result_json", columnDefinition = "TEXT")
    private String resultJson;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
