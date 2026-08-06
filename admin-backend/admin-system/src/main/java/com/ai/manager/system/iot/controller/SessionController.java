package com.ai.manager.system.iot.controller;

import com.ai.manager.common.result.ApiResult;
import com.ai.manager.common.result.PageResult;
import com.ai.manager.system.iot.domain.vo.OnlineSessionVO;
import com.ai.manager.system.iot.service.SessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 会话管理接口（后台鉴权）。
 * <p>分页查询 iot_session 表；online=true 仅在线、false 仅已结束、缺省全部。</p>
 */
@RestController
@RequestMapping("/api/iot/session")
@RequiredArgsConstructor
public class SessionController {

    private final SessionService sessionService;

    @GetMapping
    public ApiResult<PageResult<OnlineSessionVO>> page(@RequestParam(defaultValue = "1") Long page,
                                                       @RequestParam(defaultValue = "20") Long pageSize,
                                                       @RequestParam(required = false) Boolean online) {
        return ApiResult.ok(sessionService.pageSessions(page, pageSize, online));
    }
}
