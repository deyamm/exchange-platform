package com.exchange.platform.collection.api.response.dataTopic;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.exchange.platform.collection.api.enums.CommonStatus;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DataTopicView {
    private String dataTopicCode;
    private String dataTopicName;
    private String dataTopicLabel;
    private String parentCode;
    private Integer nodeLevel;
    private Boolean isLeaf;
    private Integer sortNo;
    private CommonStatus status;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<DataTopicView> children = new ArrayList<>();

} 
