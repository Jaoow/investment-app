package dev.jaoow.investmentapp.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class SectorResponse {
    private String sector;
    private List<String> subSectors;
}
