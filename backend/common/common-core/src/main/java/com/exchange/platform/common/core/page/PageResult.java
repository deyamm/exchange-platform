package com.exchange.platform.common.core.page;

import java.util.List;

public record PageResult<T>(int pageNo, int pageSize, long total, List<T> records) {
    
}
