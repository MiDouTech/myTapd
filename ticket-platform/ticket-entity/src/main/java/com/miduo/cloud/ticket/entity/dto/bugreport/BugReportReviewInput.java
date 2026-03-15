package com.miduo.cloud.ticket.entity.dto.bugreport;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * Bug简报审核入参
 */
@Data
public class BugReportReviewInput implements Serializable {

    @NotBlank(message = "审核意见不能为空")
    @Size(min = 10, message = "审核意见不能少于10个字符")
    private String reviewComment;
}
