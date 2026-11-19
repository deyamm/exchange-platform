package com.exchange.platform.analytics.entity.masterdata;

import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.*;
import com.exchange.platform.analytics.api.enums.MasterDataType;
import lombok.Data;
import lombok.experimental.Accessors;

/** 长期来源配置；不保存物理表、物理列或 SQL。 */
@Data @Accessors(chain = true) @TableName("md_source_binding")
public class MdSourceBinding {
    @TableId(type = IdType.AUTO) private Long id;
    private String bindingCode;
    private String bindingName;
    private MasterDataType masterDataType;
    private String datasetCode;
    private String triggerMode;
    private Boolean enabled;
    private String description;
    @TableField(fill = FieldFill.INSERT) private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE) private LocalDateTime updatedAt;
}
