package com.example.backend.utils.id;

public class SnowflakeIdUtil {

    private static final long START_STAMP = 1704067200000L;

    private static final long SEQUENCE_BIT = 12;
    private static final long MACHINE_BIT = 5;
    private static final long DATACENTER_BIT = 5;

    private static final long MAX_DATACENTER_NUM = ~(-1L << DATACENTER_BIT);
    private static final long MAX_MACHINE_NUM = ~(-1L << MACHINE_BIT);
    private static final long MAX_SEQUENCE = ~(-1L << SEQUENCE_BIT);

    private static final long MACHINE_LEFT = SEQUENCE_BIT;
    private static final long DATACENTER_LEFT = SEQUENCE_BIT + MACHINE_BIT;
    private static final long TIMESTAMP_LEFT = DATACENTER_LEFT + DATACENTER_BIT;

    private long datacenterId;
    private long machineId;
    private long sequence = 0L;
    private long lastStamp = -1L;

    private static final SnowflakeIdUtil DEFAULT = new SnowflakeIdUtil(1, 1);

    public SnowflakeIdUtil(long datacenterId, long machineId) {
        if (datacenterId > MAX_DATACENTER_NUM || datacenterId < 0) {
            throw new IllegalArgumentException("datacenterId is invalid");
        }
        if (machineId > MAX_MACHINE_NUM || machineId < 0) {
            throw new IllegalArgumentException("machineId is invalid");
        }
        this.datacenterId = datacenterId;
        this.machineId = machineId;
    }

    public synchronized long nextId() {
        long currStamp = System.currentTimeMillis();
        if (currStamp < lastStamp) {
            throw new IllegalStateException("Clock moved backwards");
        }
        if (currStamp == lastStamp) {
            sequence = (sequence + 1) & MAX_SEQUENCE;
            if (sequence == 0L) {
                currStamp = getNextMill();
            }
        } else {
            sequence = 0L;
        }
        lastStamp = currStamp;
        return (currStamp - START_STAMP) << TIMESTAMP_LEFT
            | datacenterId << DATACENTER_LEFT
            | machineId << MACHINE_LEFT
            | sequence;
    }

    private long getNextMill() {
        long mill = System.currentTimeMillis();
        while (mill <= lastStamp) {
            mill = System.currentTimeMillis();
        }
        return mill;
    }

    public static String nextUserId() {
        return "U" + DEFAULT.nextId();
    }

    public static String nextUserAddressId() {
        return "UA" + DEFAULT.nextId();
    }

    public static String nextUserProfileId() {
        return "UP" + DEFAULT.nextId();
    }

    public static String nextTechnicianId() {
        return "TA" + DEFAULT.nextId();
    }

    public static String nextTechnicianProfileId() {
        return "TP" + DEFAULT.nextId();
    }

    public static String nextImageId() {
        return "IM" + DEFAULT.nextId();
    }

    public static String nextFileId() {
        return "FI" + DEFAULT.nextId();
    }

    public static String nextVideoId() {
        return "VI" + DEFAULT.nextId();
    }

    public static String nextServiceCategoryId() {
        return "SC" + DEFAULT.nextId();
    }

    public static String nextServiceTypeId() {
        return "ST" + DEFAULT.nextId();
    }

    public static String nextProductId() {
        return "PD" + DEFAULT.nextId();
    }

    public static String nextProductCategoryId() {
        return "PC" + DEFAULT.nextId();
    }

    public static String nextProductFavoriteId() {
        return "PF" + DEFAULT.nextId();
    }

    public static String nextShoppingCartId() {
        return "SC" + DEFAULT.nextId();
    }

    public static String nextProductOrderId() {
        return "PO" + DEFAULT.nextId();
    }

    public static String nextOrderItemId() {
        return "OI" + DEFAULT.nextId();
    }

    public static String nextCouponId() {
        return "Q" + DEFAULT.nextId();
    }

    public static String nextUserCouponId() {
        return "UC" + DEFAULT.nextId();
    }

    public static String nextWarrantyCardId() {
        return "WC" + DEFAULT.nextId();
    }

    public static String nextWarrantyCardUsageRecordId() {
        return "WU" + DEFAULT.nextId();
    }

    public static String nextFaultPhenomenonId() {
        return "FP" + DEFAULT.nextId();
    }

    public static String nextAnnouncementId() {
        return "AN" + DEFAULT.nextId();
    }

    public static String nextUserFollowTechnicianId() {
        return "UF" + DEFAULT.nextId();
    }

    public static String nextTechnicianVisitFeePolicyId() {
        return "TVP" + DEFAULT.nextId();
    }

    public static String nextTechnicianServiceAreaId() {
        return "TSA" + DEFAULT.nextId();
    }

    public static String nextTechnicianSkillId() {
        return "TS" + DEFAULT.nextId();
    }

    public static String nextTechnicianWorkTimeId() {
        return "TW" + DEFAULT.nextId();
    }

    public static String nextTechnicianStatusLogId() {
        return "TG" + DEFAULT.nextId();
    }

    public static String nextRepairOrderId() {
        return "RO" + DEFAULT.nextId();
    }

    public static String nextRepairOrderFaultId() {
        return "ROF" + DEFAULT.nextId();
    }

    public static String nextRepairOrderPaymentId() {
        return "ROP" + DEFAULT.nextId();
    }

    public static String nextReviewId() {
        return "R" + DEFAULT.nextId();
    }

    public static String nextPaymentRecordId() {
        return "PR" + DEFAULT.nextId();
    }

    public static String nextConversationSessionId() {
        return "CS" + DEFAULT.nextId();
    }

    public static String nextConversationMessageId() {
        return "CM" + DEFAULT.nextId();
    }

    public static String nextOrderProgressId() {
        return "OP" + DEFAULT.nextId();
    }

    public static String nextFundFlowId() {
        return "FF" + DEFAULT.nextId();
    }

    public static String nextAccountBalanceId() {
        return "AB" + DEFAULT.nextId();
    }

    public static String nextOrderDoorQrCodeId() {
        return "ODQ" + DEFAULT.nextId();
    }

    public static String nextAfterSalesApplicationId() {
        return "AS" + DEFAULT.nextId();
    }

    public static String nextAccountCancelRecordId() {
        return "CR" + DEFAULT.nextId();
    }

    public static String nextSystemMessageId() {
        return "SM" + DEFAULT.nextId();
    }

    public static String nextSystemConfigId() {
        return "SC" + DEFAULT.nextId();
    }

    public static String nextStoreId() {
        return "ST" + DEFAULT.nextId();
    }

    public static String nextOperationLogId() {
        return "OL" + DEFAULT.nextId();
    }

    public static String nextStoreBusinessHoursId() {
        return "SBH" + DEFAULT.nextId();
    }

    public static String nextCancelReasonId() {
        return "CLR" + DEFAULT.nextId();
    }

    public static String nextReportId() {
        return "RP" + DEFAULT.nextId();
    }

    public static String nextCreditRecordId() {
        return "CDR" + DEFAULT.nextId();
    }

    public static String nextPenaltyRecordId() {
        return "PN" + DEFAULT.nextId();
    }

    public static String nextContentCheckRuleId() {
        return "CCR" + DEFAULT.nextId();
    }

    public static String nextContentCheckLogId() {
        return "CCL" + DEFAULT.nextId();
    }

    public static String nextImageReviewQueueId() {
        return "IRQ" + DEFAULT.nextId();
    }

    public static String nextAdminId() {
        return "AA" + DEFAULT.nextId();
    }
}
