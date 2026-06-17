package com.exchange.platform.collection.converter;

import com.exchange.platform.collection.api.response.dataTopic.DataTopicView;
import com.exchange.platform.collection.entity.dataTopic.DataTopic;

public final class DataTopicConverter {
    
    private DataTopicConverter() {}

    public static DataTopicView toView(DataTopic entity) {
        DataTopicView view = new DataTopicView();
        
        view.setDataTopicCode(entity.getDataTopicCode());
        view.setDataTopicName(entity.getDataTopicName());
        view.setDataTopicLabel(entity.getDataTopicLabel());
        view.setParentCode(entity.getParentCode());
        view.setNodeLevel(entity.getNodeLevel());
        view.setIsLeaf(entity.getIsLeaf());
        view.setSortNo(entity.getSortNo());
        view.setStatus(entity.getStatus());
        view.setDescription(entity.getDescription());
        view.setCreatedAt(entity.getCreatedAt());
        view.setUpdatedAt(entity.getUpdatedAt());
        return view;
    }
}
