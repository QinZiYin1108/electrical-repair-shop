package com.example.backend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.backend.entity.TechnicianWithdrawals;
import com.example.backend.model.worker.TechnicianWithdrawalApplyRequest;
import java.util.List;

/** 维修人员提现。 */
public interface TechnicianWithdrawalService extends IService<TechnicianWithdrawals> {

    /** 提交提现申请：冻结可提现金额。 */
    TechnicianWithdrawals apply(
            String technicianAccountId, TechnicianWithdrawalApplyRequest request);

    /** 查询某师傅的提现记录。 */
    List<TechnicianWithdrawals> listForTechnician(String technicianAccountId, int limit);

    /** 管理端按状态查询提现记录（status 为 null 时全部）。 */
    List<TechnicianWithdrawals> listForAdmin(Integer status, int limit);

    /** 审核提现单：通过或驳回（驳回退回冻结金额）。 */
    TechnicianWithdrawals review(String id, String adminId, boolean approve, String remark);

    /** 标记打款成功。 */
    TechnicianWithdrawals markPaid(String id, String adminId, String providerNo);

    /** 标记打款失败并退回冻结金额。 */
    TechnicianWithdrawals markFailed(String id, String adminId, String reason);

    /** 高金额复核确认：仅需复核的已通过提现单可确认，确认后方可打款。 */
    TechnicianWithdrawals confirmReview(String id, String adminId);

    /** 人工拦截：阻止打款。 */
    TechnicianWithdrawals intercept(String id, String adminId, String reason);

    /** 解除人工拦截。 */
    TechnicianWithdrawals releaseIntercept(String id, String adminId);
}
