import com.igorAugusto.domus.domus.service.IncomeService;
import com.igorAugusto.domus.domus.service.OutgoingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;

@RestController
@RequestMapping("/api/import")
@RequiredArgsConstructor
public class ImportController {

    private final IncomeService incomeService;
    private final OutgoingService outgoingService;

    @PostMapping("/csv")
    public ResponseEntity<Void> importCsv(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam("file") MultipartFile file
    ) throws Exception {

        User user = userRepository
                .findByEmail(userDetails.getUsername())
                .orElseThrow();

        BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream())
        );

        String line;
        boolean header = true;

        while ((line = reader.readLine()) != null) {

            if (header) {
                header = false;
                continue;
            }

            String[] cols = line.split(",");

            String type = cols[0];

            if ("INCOME".equals(type)) {
                incomeService.createFromImport(user, cols);
            }

            if ("EXPENSE".equals(type)) {
                outgoingService.createFromImport(user, cols);
            }
        }

        return ResponseEntity.ok().build();
    }
}
