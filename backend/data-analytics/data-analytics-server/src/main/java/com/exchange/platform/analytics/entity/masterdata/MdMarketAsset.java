package com.exchange.platform.analytics.entity.masterdata;

import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.*;
import com.exchange.platform.analytics.api.enums.MasterDataStatus;
import com.exchange.platform.common.core.enums.MarketAssetType;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@TableName("md_market_asset")
public class MdMarketAsset {
    @TableId(type = IdType.AUTO) private Long id;
    private MarketAssetType marketAssetType;
    private String assetCode;
    private String assetName;
    private MasterDataStatus status;
    @TableField(fill = FieldFill.INSERT) private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE) private LocalDateTime updatedAt;
}
