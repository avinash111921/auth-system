package com.identity.dto;


import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

@Data
@Builder@JsonInclude(JsonInclude.Include.NON_NULL)

public class UserResponse {
    private String id;

    private String email;

    private String fullName;

    private Boolean verified;

    private Boolean enabled;

    private String message;
}