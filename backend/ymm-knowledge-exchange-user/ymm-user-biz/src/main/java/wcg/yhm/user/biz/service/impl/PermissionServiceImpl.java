package wcg.yhm.user.biz.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import wcg.yhm.user.biz.domain.po.Permission;
import wcg.yhm.user.biz.mapper.PermissionMapper;
import wcg.yhm.user.biz.service.IPermissionService;


/**
 * <p>
 * 权限表 服务实现类
 * </p>
 *
 * @author 雨好绵
 * @since 2025-11-23
 */
@Service
public class PermissionServiceImpl extends ServiceImpl<PermissionMapper, Permission> implements IPermissionService {

}
