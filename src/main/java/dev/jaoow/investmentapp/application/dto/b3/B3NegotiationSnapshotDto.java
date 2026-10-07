package dev.jaoow.investmentapp.application.dto.b3;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class B3NegotiationSnapshotDto {
    private String fileName;
    private String fileHash;
    private List<B3NegotiationItemDto> items;
}
