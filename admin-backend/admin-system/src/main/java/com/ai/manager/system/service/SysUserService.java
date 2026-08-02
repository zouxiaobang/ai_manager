package com.ai.manager.system.service;

import com.ai.manager.common.result.PageResult;
import com.ai.manager.system.domain.entity.SysUser;
import com.ai.manager.system.domain.vo.SysUserVO;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 系统用户服务接口
 *
 * <p>提供系统用户的分页查询等用户管理功能。对外统一返回 VO，不暴露实体内部字段。</p>
 */
public interface SysUserService extends IService<SysUser> {

    /**
     * 分页查询用户列表（对外 VO）
     *
     * @param page     页码
     * @param pageSize 每页条数
     * @return 用户分页结果
     */
    PageResult<SysUserVO> pageUsers(Long page, Long pageSize);

    /**
     * 查询用户详情（对外 VO）
     *
     * @param id 用户 ID
     * @return 用户信息，不存在返回 null
     */
    SysUserVO getVO(Long id);
}
