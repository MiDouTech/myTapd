package com.miduo.cloud.ticket.entity.dto.bugreport;

import lombok.Data;

import java.io.Serializable;

/**
 * 缺陷分类输出
 */
@Data
public class DefectCategoryOutput implements Serializable {

    private Long id;

    private String name;

    private String description;
}
