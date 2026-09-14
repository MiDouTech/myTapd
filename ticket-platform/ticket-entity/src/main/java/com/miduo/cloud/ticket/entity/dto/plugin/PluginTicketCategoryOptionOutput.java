package com.miduo.cloud.ticket.entity.dto.plugin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PluginTicketCategoryOptionOutput implements Serializable {
    private Long id;
    private String name;
}
