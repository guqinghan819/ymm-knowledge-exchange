package wcg.yhm.user.relation.biz.controller;


import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;
import wcg.yhm.framework.biz.operationlog.aop.ApiOperationLog;
import wcg.yhm.user.relation.biz.domain.vo.FanAnalysisResponse;
import wcg.yhm.user.relation.biz.service.IFansService;
import wcg.yhm.user.relation.api.domain.request.AdminFanRankRequest;
import wcg.yhm.user.relation.api.domain.response.FanRankResponse;
import wcg.yhm.ymmknowledgeexchange.common.response.Response;

import java.util.List;

/**
 * <p>
 * 用户粉丝表 前端控制器
 * </p>
 *
 * @author 雨好绵
 * @since 2026-01-25
 */
@RestController
@RequestMapping("/fans")
public class FansController {

    @Resource
    private IFansService fansService;

    @PostMapping("/analysis")
    @ApiOperationLog(description = "当前用户粉丝分析")
    public Response<FanAnalysisResponse> analysis() {
        return Response.success(fansService.analysis());
    }

    @PostMapping("/admin/rank")
    @ApiOperationLog(description = "管理端粉丝增长排名")
    public Response<List<FanRankResponse>> adminFanRank(
            @Validated @RequestBody AdminFanRankRequest request) {
        return Response.success(fansService.adminFanRank(request));
    }
}
