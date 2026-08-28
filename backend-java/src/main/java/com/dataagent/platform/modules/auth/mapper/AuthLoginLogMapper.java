package com.dataagent.platform.modules.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dataagent.platform.modules.auth.domain.po.AuthLoginLogPO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AuthLoginLogMapper extends BaseMapper<AuthLoginLogPO> {
}
