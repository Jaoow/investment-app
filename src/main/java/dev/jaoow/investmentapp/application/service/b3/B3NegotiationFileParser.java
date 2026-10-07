package dev.jaoow.investmentapp.application.service.b3;

import dev.jaoow.investmentapp.application.dto.b3.B3NegotiationSnapshotDto;
import org.springframework.web.multipart.MultipartFile;

public interface B3NegotiationFileParser {
    boolean supports(MultipartFile file);
    B3NegotiationSnapshotDto parse(MultipartFile file);
}
