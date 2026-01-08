package com.igorAugusto.domus.domus.controller.importacao_teste;

import com.igorAugusto.domus.domus.entity.User;
import com.igorAugusto.domus.domus.repository.UserRepository;
import com.igorAugusto.domus.domus.service.OutgoingService;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/import")
@RequiredArgsConstructor
public class ImportController {

    private final OutgoingService outgoingService;
    private final UserRepository userRepository;

    @PostMapping("/xlsx")
    public ResponseEntity<Void> importXlsx(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam("file") MultipartFile file
    ) throws Exception {

        User user = userRepository
                .findByEmail(userDetails.getUsername())
                .orElseThrow();

        Workbook workbook = WorkbookFactory.create(file.getInputStream());
        Sheet sheet = workbook.getSheetAt(0);

        boolean header = true;

        for (Row row : sheet) {
            if (header) {
                header = false;
                continue;
            }

            String type = row.getCell(0).getStringCellValue();

            if ("EXPENSE".equals(type)) {
                outgoingService.createFromImport(user, row);
            }
        }

        workbook.close();
        return ResponseEntity.ok().build();
    }
}
