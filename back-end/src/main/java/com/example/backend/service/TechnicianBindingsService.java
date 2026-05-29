package com.example.backend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.backend.entity.TechnicianBindings;

import java.util.List;

public interface TechnicianBindingsService extends IService<TechnicianBindings> {

    /** 门店邀请师傅 */
    TechnicianBindings invite(String storeId, String technicianId);

    /** 师傅接受邀请 */
    TechnicianBindings accept(String technicianId);

    /** 师傅拒绝邀请 */
    TechnicianBindings reject(String technicianId);

    /** 师傅申请解绑 */
    TechnicianBindings requestUnbind(String technicianId);

    /** 门店直接解绑 */
    void directUnbind(String storeId, String technicianId);

    /** 门店同意解绑 */
    void approveUnbind(String storeId, String technicianId);

    /** 查询门店的绑定列表 */
    List<TechnicianBindings> listByStore(String storeId, Integer status);
}
