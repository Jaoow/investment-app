package dev.jaoow.investmentapp.application.dto.b3;

import dev.jaoow.investmentapp.domain.model.MovementType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class B3NegotiationItemDto {
    private LocalDate date;
    private MovementType type;
    private String institution;
    private String tickerSymbol;
    private BigDecimal quantity;
    private BigDecimal price;
    private BigDecimal totalValue;
    private String hash;
    
    // Generates a unique hash for this negotiation row based on the B3 spreadsheet columns
    public String generateHash(int indexInSameDay) {
        String raw = String.format("%s|%s|%s|%s|%s|%s|%d", 
            date.toString(), type.name(), institution, tickerSymbol, 
            quantity.toPlainString(), price.toPlainString(), indexInSameDay);
        return hashString(raw);
    }
    
    private String hashString(String input) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
