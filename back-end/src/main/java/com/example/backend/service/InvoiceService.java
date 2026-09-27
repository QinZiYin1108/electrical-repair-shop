package com.example.backend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.backend.entity.Invoices;
import com.example.backend.model.finance.InvoiceApplyRequest;
import java.util.List;

/** 发票：用户申请、管理员开票/驳回/红冲。 */
public interface InvoiceService extends IService<Invoices> {

    /** 用户申请开票（金额不超过订单可开票额度）。 */
    Invoices apply(String userAccountId, InvoiceApplyRequest request);

    /** 用户查询自己的发票申请。 */
    List<Invoices> listForUser(String userAccountId, int limit);

    /** 管理端查询（可按状态/订单类型过滤）。 */
    List<Invoices> listForAdmin(Integer status, Integer orderType, int limit);

    /** 开票（返回开票结果地址）。 */
    Invoices issue(String id, String adminId, String invoiceUrl);

    /** 驳回申请。 */
    Invoices reject(String id, String adminId, String reason);

    /** 红冲已开票记录。 */
    Invoices red(String id, String adminId, String reason);
}
