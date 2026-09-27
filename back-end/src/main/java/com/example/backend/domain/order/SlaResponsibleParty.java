package com.example.backend.domain.order;

/** SLA 责任方。 */
public enum SlaResponsibleParty {
    /** 平台（如未接单，尚无师傅）。 */
    PLATFORM,
    /** 维修师傅。 */
    TECHNICIAN,
    /** 用户。 */
    USER
}
