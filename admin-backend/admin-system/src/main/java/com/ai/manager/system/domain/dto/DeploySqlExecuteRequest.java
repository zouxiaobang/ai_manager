package com.ai.manager.system.domain.dto;

import lombok.Data;

@Data
public class DeploySqlExecuteRequest {
    /** local | node118 */
    private String target;
    private String sql;
}
