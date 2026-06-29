package com.javan.smart.water.common.base;


import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class FPageInfo {
    /**当前页*/
    private int pageNum;
    /**每页的数量*/
    private int pageSize;
    /**总数*/
    private long total;

    public FPageInfo(List<?> dataList) {
        this.total = dataList == null ? 0 : dataList.size();
        this.pageNum = 1;
        this.pageSize = Long.valueOf(this.total).intValue();
    }
}
