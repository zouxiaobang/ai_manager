package com.ai.manager.system.service.impl;

import com.ai.manager.common.result.PageResult;
import com.ai.manager.common.result.PageUtils;
import com.ai.manager.system.domain.entity.SysUser;
import com.ai.manager.system.domain.vo.SysUserVO;
import com.ai.manager.system.mapper.SysUserMapper;
import com.ai.manager.system.service.SysUserService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    @Override
    public PageResult<SysUserVO> pageUsers(Long page, Long pageSize) {
        long p = PageUtils.normalizePage(page);
        long ps = PageUtils.normalizePageSize(pageSize);
        Page<SysUser> entityPage = page(new Page<>(p, ps), new LambdaQueryWrapper<SysUser>()
                .orderByDesc(SysUser::getId));
        List<SysUserVO> vos = entityPage.getRecords().stream().map(this::toVO).toList();
        return PageUtils.of(vos, entityPage.getTotal(), entityPage.getCurrent(), entityPage.getSize());
    }

    @Override
    public SysUserVO getVO(Long id) {
        SysUser user = getById(id);
        return user == null ? null : toVO(user);
    }

    private SysUserVO toVO(SysUser entity) {
        SysUserVO vo = new SysUserVO();
        vo.setId(entity.getId());
        vo.setUsername(entity.getUsername());
        vo.setNickname(entity.getNickname());
        vo.setStatus(entity.getStatus());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
        return vo;
    }
}
