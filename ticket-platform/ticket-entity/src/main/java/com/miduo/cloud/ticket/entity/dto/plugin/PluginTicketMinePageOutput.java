package com.miduo.cloud.ticket.entity.dto.plugin;

import com.miduo.cloud.ticket.common.dto.common.PageOutput;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class PluginTicketMinePageOutput extends PageOutput<PluginTicketSummaryOutput> {
    private List<PluginTicketCategoryOptionOutput> categoryOptions = new ArrayList<>();
    private List<PluginTicketStatusOptionOutput> statusOptions = new ArrayList<>();

    public PluginTicketMinePageOutput(List<PluginTicketSummaryOutput> records, long total,
                                      int pageNum, int pageSize) {
        super(records, total, pageNum, pageSize);
    }
}
