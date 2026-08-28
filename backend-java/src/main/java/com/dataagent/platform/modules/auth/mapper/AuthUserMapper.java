package com.dataagent.platform.modules.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dataagent.platform.modules.auth.domain.po.AuthUserPO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AuthUserMapper extends BaseMapper<AuthUserPO> {
}
