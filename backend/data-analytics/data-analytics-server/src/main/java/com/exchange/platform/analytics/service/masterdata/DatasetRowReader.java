package com.exchange.platform.analytics.service.masterdata;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 主数据同步过程的数据集读取接口。
 *
 * <p>
 * 上游只传入 Dataset、需要读取的字段、筛选条件和分页信息， 不直接依赖物理表、数据库连接或采集服务的实现细节。 
 * 具体实现负责解析 Dataset 元数据并完成实际 I/O。
 * </p>
 *
 * <p>
 * 接口额外提供 {@link #readAll(DatasetReadRequest)}。该方法基于 pageNo/pageSize 自动翻页，避免固定只读取前 2000 条数据。
 * </p>
 */
public interface DatasetRowReader {

    /**
     * 单页读取 Dataset。
     *
     * @param request 查询条件
     * @return 当前页数据；实现不应返回 null
     */
    List<Map<String, Object>> read(DatasetReadRequest request);

    /**
     * 从请求指定的起始页开始自动翻页，读取完整结果集。
     *
     * <p>
     * 每次请求沿用原始查询条件，只递增页码。最后一页的记录数小于 pageSize 时停止；如果总记录数刚好是 pageSize 的整数倍，
     * 会额外读取一个空页后结束。
     * </p>
     */
    default List<Map<String, Object>> readAll(DatasetReadRequest request) {
        Objects.requireNonNull(request, "DatasetReadRequest 不能为空");

        List<Map<String, Object>> result = new ArrayList<>();
        int pageNo = request.pageNo();

        while (true) {
            DatasetReadRequest pageRequest = request.withPage(pageNo);
            List<Map<String, Object>> page = read(pageRequest);

            if (page == null) {
                throw new IllegalStateException("DatasetRowReader.read() 不允许返回 null: " + request.datasetCode());
            }
            if (page.size() > pageRequest.pageSize()) {
                throw new IllegalStateException("Dataset 单页返回数量超过 pageSize，无法安全执行自动分页: dataset=" 
                    + request.datasetCode()
                    + ", pageNo=" + pageNo + ", pageSize=" + pageRequest.pageSize() + ", actual=" + page.size());
            }
            if (page.isEmpty()) {
                break;
            }

            result.addAll(page);

            if (page.size() < pageRequest.pageSize()) {
                break;
            }
            if (pageNo == Integer.MAX_VALUE) {
                throw new IllegalStateException("Dataset 自动分页页码已达到 Integer.MAX_VALUE: " + request.datasetCode());
            }

            pageNo++;
        }

        return Collections.unmodifiableList(result);
    }

    /**
     * 一次 Dataset 行读取的完整参数。
     */
    record DatasetReadRequest(
        String datasetCode, 
        // 此处应该是数据集的逻辑字段，即数据类型字段
        List<String> fieldCodes, 
        Map<String, Object> filters, 
        List<String> assetCodes, 
        LocalDateTime from, 
        LocalDateTime to, 
        String orderBy, 
        int pageNo, 
        int pageSize
    ) {

        public DatasetReadRequest {
            fieldCodes = fieldCodes == null ? List.of() : List.copyOf(fieldCodes);
            filters = immutableMapAllowingNullValues(filters);
            assetCodes = assetCodes == null ? List.of() : List.copyOf(assetCodes);

            pageNo = pageNo < 1 ? 1 : pageNo;
            pageSize = pageSize < 1 ? 500 : Math.min(pageSize, 2000);
        }

        /** 基于当前请求创建指定页请求，其他条件保持不变。 */
        public DatasetReadRequest withPage(int newPageNo) {
            return new DatasetReadRequest(datasetCode, fieldCodes, filters, assetCodes, from, to, orderBy, newPageNo, pageSize);
        }

        private static Map<String, Object> immutableMapAllowingNullValues(Map<String, Object> source) {

            if (source == null || source.isEmpty()) {
                return Map.of();
            }

            return Collections.unmodifiableMap(new LinkedHashMap<>(source));
        }
    }
}
