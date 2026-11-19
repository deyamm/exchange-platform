package com.exchange.platform.analytics.entity.masterdata;

import java.time.*;
import com.baomidou.mybatisplus.annotation.*;
import com.exchange.platform.analytics.api.enums.StockMarketType;
import lombok.Data;
import lombok.experimental.Accessors;

@Data @Accessors(chain = true) @TableName("md_market_asset_stock")
public class MdMarketAssetStock {
    @TableId private Long marketAssetId;
    private StockMarketType marketType;
    private String exchangeCode;
    private String currencyCode;
    private LocalDate listDate;
    private LocalDate delistDate;
    @TableField(fill = FieldFill.INSERT) private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE) private LocalDateTime updatedAt;
}
