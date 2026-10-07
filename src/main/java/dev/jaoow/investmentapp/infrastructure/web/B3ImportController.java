package dev.jaoow.investmentapp.infrastructure.web;

import dev.jaoow.investmentapp.application.dto.response.B3ImportResultResponse;
import dev.jaoow.investmentapp.application.service.b3.B3NegotiationImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;

@RestController
@RequestMapping("/v1/portfolio/{portfolioId}/b3-import")
@Tag(name = "B3 Import", description = "B3 negotiation spreadsheet import and history")
public class B3ImportController {

    private final B3NegotiationImportService b3NegotiationImportService;

    public B3ImportController(B3NegotiationImportService b3NegotiationImportService) {
        this.b3NegotiationImportService = b3NegotiationImportService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Import a B3 negotiation spreadsheet")
    public B3ImportResultResponse importNegotiations(
            @PathVariable Long portfolioId,
            @RequestParam("file") MultipartFile file,
            Principal principal) {
        return b3NegotiationImportService.importNegotiations(portfolioId, file, principal);
    }

    @GetMapping
    @Operation(summary = "List B3 import history for a portfolio")
    public PagedModel<B3ImportResultResponse> getImportHistory(
            @PathVariable Long portfolioId,
            Pageable pageable) {
        return b3NegotiationImportService.getImportHistory(portfolioId, pageable);
    }
}
