package com.ai.manager.system.controller;

import com.ai.manager.common.result.ApiResult;
import com.ai.manager.common.result.PageResult;
import com.ai.manager.system.domain.dto.UpdateUserRequest;
import com.ai.manager.system.domain.entity.SysUser;
import com.ai.manager.system.domain.vo.SysUserVO;
import com.ai.manager.system.service.SysUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 系统用户管理接口
 *
 * <p>提供用户分页查询、详情与昵称更新。</p>
 */
@RestController
@RequestMapping("/api/system/users")
@RequiredArgsConstructor
public class SysUserController {

    private final SysUserService sysUserService;

    /**
     * 分页查询用户列表
     *
     * @param page     页码，为空时使用默认值
     * @param pageSize 每页条数，为空时使用默认值
     * @return 用户分页结果
     */
    @GetMapping
    public ApiResult<PageResult<SysUserVO>> list(@RequestParam(required = false) Long page,
                                                 @RequestParam(required = false) Long pageSize) {
        return ApiResult.ok(sysUserService.pageUsers(page, pageSize));
    }

    /**
     * 查询用户详情
     *
     * @param id 用户 ID
     * @return 用户信息（对外 VO，不含内部字段）
     */
    @GetMapping("/{id}")
    public ApiResult<SysUserVO> getById(@PathVariable Long id) {
        SysUserVO vo = sysUserService.getVO(id);
        if (vo == null) {
            return ApiResult.fail(404, "用户不存在");
        }
        return ApiResult.ok(vo);
    }

    /**
     * 更新用户信息（当前支持昵称）
     *
     * @param id   用户 ID
     * @param body 待更新字段
     * @return 成功无数据
     */
    @PutMapping("/{id}")
    public ApiResult<Void> update(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest body) {
        SysUser user = sysUserService.getById(id);
        if (user == null) {
            return ApiResult.fail(404, "用户不存在");
        }
        if (body.getNickname() != null) {
            user.setNickname(body.getNickname());
        }
        sysUserService.updateById(user);
        return ApiResult.ok();
    }
}
