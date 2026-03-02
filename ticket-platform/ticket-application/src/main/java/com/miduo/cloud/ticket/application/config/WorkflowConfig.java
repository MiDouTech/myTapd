package com.miduo.cloud.ticket.application.config;

import com.miduo.cloud.ticket.domain.workflow.service.StateMachineWorkflowEngine;
import com.miduo.cloud.ticket.domain.workflow.service.WorkflowEngine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 工作流引擎配置
 */
@Configuration
public class WorkflowConfig {

    @Bean
    public WorkflowEngine workflowEngine() {
        return new StateMachineWorkflowEngine();
    }
}
