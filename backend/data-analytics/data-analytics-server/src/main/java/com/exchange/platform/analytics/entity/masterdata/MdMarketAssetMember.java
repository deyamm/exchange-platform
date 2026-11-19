package com.exchange.platform.analytics.entity.masterdata;

import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

@Data @Accessors(chain = true) @TableName("md_market_asset_member")
public class MdMarketAssetMember {
    @TableId(type = IdType.AUTO) private Long id;
    private Long parentMarketAssetId;
    private Long memberMarketAssetId;
    private LocalDateTime validFrom;
    private LocalDateTime validTo;
    private Long batchId;
    @TableField(fill = FieldFill.INSERT) private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE) private LocalDateTime updatedAt;
}
