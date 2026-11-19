package com.exchange.platform.analytics.service.masterdata;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 主数据同步读取 Dataset 的抽象边界。
 *
 * <p>上层只声明逻辑 Dataset、需要的标准字段、筛选条件和分页信息，
 * 不直接依赖物理表、数据库连接或采集服务的实现细节。具体实现负责解析
 * Dataset 元数据并完成实际 I/O，因此替换数据服务时不会影响主数据发布流程。</p>
 */
public interface DatasetRowReader {
    /** 按逻辑查询条件读取数据行；返回的每一行以标准 fieldCode 为键。 */
    List<Map<String, Object>> read(DatasetReadRequest request);

    /**
     * 一次 Dataset 行读取的完整参数。
     *
     * <p>构造时立即复制集合，避免调用方在请求发送后继续修改查询条件；
     * 同时把分页参数收敛到安全范围，防止误读过大的数据集。</p>
     */
    record DatasetReadRequest(String datasetCode, List<String> fieldCodes, Map<String, Object> filters,
            List<String> assetCodes, LocalDateTime from, LocalDateTime to, String orderBy,
            int pageNo, int pageSize) {
        public DatasetReadRequest {
            // 空集合表示“不额外限定”；复制后保证请求对象不可变。
            fieldCodes = fieldCodes == null ? List.of() : List.copyOf(fieldCodes);
            filters = filters == null ? Map.of() : Map.copyOf(filters);
            assetCodes = assetCodes == null ? List.of() : List.copyOf(assetCodes);
            // 页码从 1 开始，单页最多读取 2000 行，避免一次发布占用过多内存。
            pageNo = pageNo < 1 ? 1 : pageNo;
            pageSize = pageSize < 1 ? 500 : Math.min(pageSize, 2000);
        }
    }
}
