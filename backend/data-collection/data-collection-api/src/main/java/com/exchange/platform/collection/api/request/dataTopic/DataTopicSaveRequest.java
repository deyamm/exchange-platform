package com.exchange.platform.collection.api.request.dataTopic;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.exchange.platform.collection.api.enums.CommonStatus;

public record DataTopicSaveRequest(
    @NotBlank String saveOrUpdate,
    @NotBlank @Size(max = 32) @Pattern(regexp = "^[A-N][0-9]*$", message = "首位必须为A-N大写字母、其余为数字") String dataTopicCode,
    @NotBlank @Size(max = 32) String dataTopicName,
    @Size(max = 32) String dataTopicLabel,
    @Size(max = 32) String parentCode,
    Integer nodeLevel,
    Boolean isLeaf,
    Integer sortNo,
    @NotNull CommonStatus status,
    @Size(max = 255) String description
) {}
