package com.javan.smart.water.common.base;


import lombok.Data;

@Data
public class FPageInfo {
    /**当前页*/
    private int pageNum;
    /**每页的数量*/
    private int pageSize;
    /**总数*/
    private long total;

}
