package dev.jaoow.investmentapp.application.dto.response;

import lombok.Data;

import java.util.Date;
import java.util.Set;

@Data
public class UserRegisterResponse {

    private Long id;
    private String email;
    private Date createdAt;
    private Date updatedAt;
    private Set<String> roles;

}
