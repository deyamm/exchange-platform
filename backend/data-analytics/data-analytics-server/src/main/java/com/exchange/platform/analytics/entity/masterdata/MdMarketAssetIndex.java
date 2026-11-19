package com.exchange.platform.analytics.entity.masterdata;

import java.time.*;
import com.baomidou.mybatisplus.annotation.*;
import com.exchange.platform.analytics.api.enums.IndexCategory;
import lombok.Data;
import lombok.experimental.Accessors;

@Data @Accessors(chain = true) @TableName("md_market_asset_index")
public class MdMarketAssetIndex {
    @TableId private Long marketAssetId;
    private String publisherCode;
    private IndexCategory indexCategory;
    private String schemeCode;
    private Integer level;
    private String exchangeCode;
    private LocalDate launchDate;
    private LocalDate endDate;
    @TableField(fill = FieldFill.INSERT) private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE) private LocalDateTime updatedAt;
}
