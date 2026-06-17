package com.exchange.platform.collection.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.exchange.platform.collection.api.enums.CommonStatus;
import com.exchange.platform.collection.api.request.dataTopic.DataTopicSaveRequest;
import com.exchange.platform.collection.api.response.dataTopic.DataTopicView;
import com.exchange.platform.collection.entity.dataTopic.DataTopic;


public interface DataTopicService extends IService<DataTopic>{

    /* 按树形或分页查询数据主题 */
    Object queryDataTopics(String parentCode, String dataTopicName, String dataTopicLabel, CommonStatus status, Boolean tree, Integer pageNo, Integer pageSize);
    
    /* 保存更更新数据主题 */
    DataTopicView saveDataTopic(DataTopicSaveRequest request);

    /* 删除数据主题 */
    void deleteDataTopic(String dataTopicCode);
} 
