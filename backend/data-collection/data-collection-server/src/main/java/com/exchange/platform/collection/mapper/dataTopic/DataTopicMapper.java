package com.exchange.platform.collection.mapper.dataTopic;

import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.exchange.platform.collection.entity.dataTopic.DataTopic;

public interface DataTopicMapper extends BaseMapper<DataTopic> {

    List<String> selectAllTopicNames();

} 
