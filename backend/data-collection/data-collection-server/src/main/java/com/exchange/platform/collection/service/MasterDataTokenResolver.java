package com.exchange.platform.collection.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.exchange.platform.analytics.client.MasterDataClient;
import com.exchange.platform.analytics.api.enums.MasterDataType;
import com.exchange.platform.analytics.api.request.PublishedMasterDataQueryRequest;
import com.exchange.platform.analytics.api.response.PublishedMasterDataTypeDetailView;
import com.exchange.platform.common.core.exception.BusinessException;
import com.exchange.platform.common.core.page.PageResult;
import com.exchange.platform.common.core.result.ApiResponse;

/** Resolves persisted {MASTER:type:logicalField} values at execution time. */
@Component
public class MasterDataTokenResolver {
    private static final Pattern TOKEN = Pattern.compile("^\\{MASTER:([^:}]+):([^:}]+)\\}$");
    private final MasterDataClient client;

    public MasterDataTokenResolver(MasterDataClient client) {
        this.client = client;
    }

    public Object resolve(Object value) {
        if (value instanceof Map<?, ?> map) {
            java.util.Map<String, Object> result = new java.util.LinkedHashMap<>();
            map.forEach((k, v) -> result.put(String.valueOf(k), resolve(v)));
            return result;
        }
        if (value instanceof List<?> list) {
            return list.stream().map(this::resolve).toList();
        }
        if (!(value instanceof String text))
            return value;
        Matcher matcher = TOKEN.matcher(text.trim());
        if (!matcher.matches())
            return value;
        String type = matcher.group(1);
        String field = matcher.group(2);
        ApiResponse<PublishedMasterDataTypeDetailView> detailResponse = client.publishedTypeDetail(type);
        if (detailResponse == null || detailResponse.code() != 0 || detailResponse.data() == null) {
            throw new BusinessException(40004, "主数据类型未发布或不可用：" + type);
        }
        boolean fieldExists = detailResponse.data().fields().stream().anyMatch(item -> field.equals(item.fieldCode()));
        if (!fieldExists)
            throw new BusinessException(40004, "主数据逻辑字段不存在：" + type + "." + field);

        List<String> values = new ArrayList<>();
        int pageNo = 1;
        long fetched = 0;
        final int pageSize = 500;
        long total;
        do {
            MasterDataType masterDataType = MasterDataType.valueOf(type);
            ApiResponse<PageResult<Map<String, Object>>> rowsResponse = client.publishedRows(new PublishedMasterDataQueryRequest(masterDataType, null, null, null, null, null, null, pageNo, pageSize));
            
            if (rowsResponse == null || rowsResponse.code() != 0 || rowsResponse.data() == null) {
                throw new BusinessException(40004, "读取主数据失败：" + type);
            }
            PageResult<Map<String, Object>> page = rowsResponse.data();
            page.records().forEach(row -> {
                Object item = row.get(field);
                if (item != null)
                    values.add(String.valueOf(item));
            });
            total = page.total();
            fetched += page.records().size();
            if (page.records().isEmpty())
                break;
            pageNo++;
        } while (fetched < total && pageNo < 10000);
        return String.join(",", values);
    }
}
