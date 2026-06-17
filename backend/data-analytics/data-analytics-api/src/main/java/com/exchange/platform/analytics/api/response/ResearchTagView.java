package com.exchange.platform.analytics.api.response;

import java.time.LocalDateTime;

import com.exchange.platform.analytics.api.enums.CommonStatus;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResearchTagView {

    private String tagCode;
    private String tagName;
    private String tagCategory;
    private CommonStatus status;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}