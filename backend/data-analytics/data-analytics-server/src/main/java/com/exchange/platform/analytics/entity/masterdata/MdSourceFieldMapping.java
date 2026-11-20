package com.exchange.platform.analytics.entity.masterdata;

import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.*;
import com.exchange.platform.analytics.api.enums.MappingMode;
import lombok.Data;
import lombok.experimental.Accessors;

/** Dataset 标准 fieldCode 到 Registry targetField 的映射。 */
@Data 
@Accessors(chain = true) 
@TableName("md_source_field_mapping")
public class MdSourceFieldMapping {
    @TableId(type = IdType.AUTO) private Long id;
    private Long bindingId;
    private String targetField;
    private MappingMode mappingMode;
    private String sourceFieldCode;
    private String constantValue;
    @TableField(fill = FieldFill.INSERT) private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE) private LocalDateTime updatedAt;
}
