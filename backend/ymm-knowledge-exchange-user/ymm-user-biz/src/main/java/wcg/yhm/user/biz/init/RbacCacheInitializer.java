package wcg.yhm.user.biz.init;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import wcg.yhm.user.biz.service.RbacCacheService;

@Component
@Slf4j
public class RbacCacheInitializer {

    @Resource
    private RbacCacheService rbacCacheService;

    @PostConstruct
    public void init() {
        log.info("Starting RBAC cache synchronization");
        rbacCacheService.synchronizeAll();
    }
}
