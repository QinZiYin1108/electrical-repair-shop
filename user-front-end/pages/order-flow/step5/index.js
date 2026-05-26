const draftStore = require("../../../utils/orderDraftStore");
const flowNavigation = require("../../../utils/orderFlowNavigation");
const { fetchAppointmentSlots } = require("../../../api/userOrderFlow");

const DAY_LABELS = {
  1: "周一",
  2: "周二",
  3: "周三",
  4: "周四",
  5: "周五",
  6: "周六",
  7: "周日"
};

function pad2(value) {
  return String(value).padStart(2, "0");
}

function formatDate(date) {
  return `${date.getFullYear()}-${pad2(date.getMonth() + 1)}-${pad2(date.getDate())}`;
}

function formatTime(date) {
  return `${pad2(date.getHours())}:${pad2(date.getMinutes())}`;
}

function toDateParts(dateText) {
  const parts = String(dateText || "").split("-");
  if (parts.length !== 3) return null;
  const year = Number(parts[0]);
  const month = Number(parts[1]);
  const day = Number(parts[2]);
  if (!year || !month || !day) return null;
  return { year, month, day };
}

function toTimestamp(dateText, timeText) {
  const dateParts = toDateParts(dateText);
  const timeParts = String(timeText || "").split(":");
  if (!dateParts || timeParts.length < 2) return null;
  const hour = Number(timeParts[0]);
  const minute = Number(timeParts[1]);
  if (Number.isNaN(hour) || Number.isNaN(minute)) return null;
  return new Date(dateParts.year, dateParts.month - 1, dateParts.day, hour, minute, 0, 0).getTime();
}

function toDateTimeText(timestamp) {
  if (!timestamp) {
    return {
      dateText: "",
      timeText: ""
    };
  }
  const date = new Date(Number(timestamp));
  if (Number.isNaN(date.getTime())) {
    return {
      dateText: "",
      timeText: ""
    };
  }
  return {
    dateText: formatDate(date),
    timeText: formatTime(date)
  };
}

function normalizeTimeText(value) {
  const match = String(value || "").match(/(\d{1,2}):(\d{1,2})/);
  if (!match) return "";
  const hour = Number(match[1]);
  const minute = Number(match[2]);
  if (Number.isNaN(hour) || Number.isNaN(minute) || hour < 0 || hour > 23 || minute < 0 || minute > 59) {
    return "";
  }
  return `${pad2(hour)}:${pad2(minute)}`;
}

function toMinutes(timeText) {
  const normalized = normalizeTimeText(timeText);
  if (!normalized) return -1;
  const parts = normalized.split(":");
  return Number(parts[0]) * 60 + Number(parts[1]);
}

function getWeekDay(dateText) {
  const parts = toDateParts(dateText);
  if (!parts) return 0;
  const day = new Date(parts.year, parts.month - 1, parts.day).getDay();
  return day === 0 ? 7 : day;
}

function normalizeWorkWindows(list) {
  return (Array.isArray(list) ? list : [])
    .map((item) => {
      const dayOfWeek = Number(item.dayOfWeek || 0);
      const startTime = normalizeTimeText(item.startTime);
      const endTime = normalizeTimeText(item.endTime);
      const startMinutes = toMinutes(startTime);
      const endMinutes = toMinutes(endTime);
      return {
        dayOfWeek,
        dayLabel: item.dayLabel || DAY_LABELS[dayOfWeek] || "",
        startTime,
        endTime,
        startMinutes,
        endMinutes
      };
    })
    .filter((item) => item.dayOfWeek >= 1 && item.dayOfWeek <= 7 && item.startMinutes >= 0 && item.endMinutes >= 0 && item.startMinutes < item.endMinutes)
    .sort((a, b) => {
      if (a.dayOfWeek !== b.dayOfWeek) {
        return a.dayOfWeek - b.dayOfWeek;
      }
      return a.startMinutes - b.startMinutes;
    });
}

function groupWorkWindowsByDay(windows) {
  const map = {};
  (windows || []).forEach((item) => {
    if (!map[item.dayOfWeek]) {
      map[item.dayOfWeek] = [];
    }
    map[item.dayOfWeek].push(item);
  });
  return map;
}

function buildWorkWindowSummaryList(windows) {
  const map = groupWorkWindowsByDay(windows);
  return Object.keys(map)
    .map((key) => Number(key))
    .sort((a, b) => a - b)
    .map((dayOfWeek) => {
      const times = map[dayOfWeek].map((item) => `${item.startTime}-${item.endTime}`).join(" / ");
      return `${DAY_LABELS[dayOfWeek] || ""} ${times}`;
    });
}

function getDayWindowText(dateText, windowsByDay) {
  const day = getWeekDay(dateText);
  const dayWindows = windowsByDay[day] || [];
  if (!dayWindows.length) {
    return "无可预约时段";
  }
  return dayWindows.map((item) => `${item.startTime}-${item.endTime}`).join(" / ");
}

function formatLeadText(minLeadMinutes) {
  const minutes = Number(minLeadMinutes || 0);
  if (minutes > 0 && minutes % 60 === 0) {
    return `${minutes / 60}小时`;
  }
  return `${minutes || 0}分钟`;
}

function buildLimitHint(minLeadMinutes, bookingDays) {
  const leadText = formatLeadText(minLeadMinutes);
  const safeBookingDays = Number(bookingDays || 0);
  return `最早可约当前时间+${leadText}，最晚可约未来${safeBookingDays || 0}天`;
}

function roundNowToMinute(minLeadMinutes) {
  const now = new Date();
  now.setSeconds(0, 0);
  now.setMinutes(now.getMinutes() + Number(minLeadMinutes || 0));
  return now;
}

function findFirstAvailableSelection(startDateText, endDateText, windowsByDay, minLeadMinutes) {
  const startTs = toTimestamp(startDateText, "00:00");
  const endTs = toTimestamp(endDateText, "23:59");
  if (!startTs || !endTs || startTs > endTs) {
    return null;
  }

  const now = roundNowToMinute(minLeadMinutes);
  let cursor = new Date(startTs);
  while (cursor.getTime() <= endTs) {
    const dateText = formatDate(cursor);
    const day = getWeekDay(dateText);
    const dayWindows = windowsByDay[day] || [];
    if (dayWindows.length) {
      const nowMinutes = toMinutes(formatTime(now));
      for (let i = 0; i < dayWindows.length; i++) {
        const row = dayWindows[i];
        let candidateMinutes = row.startMinutes;
        if (dateText === formatDate(now)) {
          candidateMinutes = Math.max(candidateMinutes, nowMinutes);
        }
        if (candidateMinutes <= row.endMinutes) {
          const hour = Math.floor(candidateMinutes / 60);
          const minute = candidateMinutes % 60;
          return {
            dateText,
            timeText: `${pad2(hour)}:${pad2(minute)}`
          };
        }
      }
    }
    cursor = new Date(cursor.getTime() + 24 * 60 * 60 * 1000);
  }
  return null;
}

function validateSelection(dateText, timeText, bookingStartDate, bookingEndDate, windowsByDay, minLeadMinutes) {
  if (!dateText || !timeText) {
    return {
      valid: false,
      message: "请先选择预约日期和时间"
    };
  }

  const timestamp = toTimestamp(dateText, timeText);
  if (!timestamp) {
    return {
      valid: false,
      message: "预约时间格式无效"
    };
  }

  const startTs = toTimestamp(bookingStartDate, "00:00");
  const endTs = toTimestamp(bookingEndDate, "23:59");
  if (startTs && timestamp < startTs) {
    return {
      valid: false,
      message: "不在可预约日期范围内"
    };
  }
  if (endTs && timestamp > endTs) {
    return {
      valid: false,
      message: "不在可预约日期范围内"
    };
  }

  const safeMinLeadMinutes = Number(minLeadMinutes || 0);
  const minTs = Date.now() + safeMinLeadMinutes * 60 * 1000;
  if (timestamp < minTs) {
    return {
      valid: false,
      message: `预约时间需至少提前${formatLeadText(safeMinLeadMinutes)}`
    };
  }

  const day = getWeekDay(dateText);
  const dayWindows = windowsByDay[day] || [];
  if (!dayWindows.length) {
    return {
      valid: false,
      message: "所选日期不在师傅工作日内"
    };
  }

  const minuteValue = toMinutes(timeText);
  const inRange = dayWindows.some((item) => minuteValue >= item.startMinutes && minuteValue <= item.endMinutes);
  if (!inRange) {
    return {
      valid: false,
      message: `所选时间不在工作时间内，当日可选：${dayWindows
        .map((item) => `${item.startTime}-${item.endTime}`)
        .join(" / ")}`
    };
  }

  return {
    valid: true,
    timestamp,
    label: `${dateText} ${timeText}`
  };
}

Page({
  data: {
    serviceMode: 1,
    serviceModeName: "",
    serviceTypeName: "",
    technicianName: "",
    addressDetail: "",
    bookingStartDate: "",
    bookingEndDate: "",
    selectedDate: "",
    selectedTime: "",
    selectedAppointmentLabel: "",
    currentDayWindowText: "",
    workWindows: [],
    workWindowSummaryList: [],
    hasAvailableWindow: false,
    appointmentEditable: true,
    readonlyAppointmentTip: "",
    minLeadMinutes: 60,
    limitHintText: "",
    labels: {
      step: "步骤 5 / 6",
      title: "预约时间与确认",
      infoTitle: "本单信息确认",
      technician: "师傅",
      address: "地址",
      appointmentTitle: "选择预约时间",
      workWindow: "工作时间",
      date: "预约日期",
      time: "预约时间",
      dayRange: "当日可选时段",
      selected: "当前选择",
      choose: "请选择",
      emptyWindow: "当前师傅未配置可预约工作时间",
      prev: "上一步",
      next: "下一步"
    }
  },

  onLoad() {
    const draft = draftStore.getDraft();
    if (Number(draft.serviceMode) === 3) {
      flowNavigation.redirectTo(this, "/pages/order-flow/step6/index");
      return;
    }
    if (!draft.selectedTechnicianId) {
      wx.showToast({
        title: "请先选择师傅",
        icon: "none"
      });
      flowNavigation.redirectTo(this, "/pages/order-flow/step3/index");
      return;
    }
    this.rebuild(draft);
  },

  onUnload() {
    flowNavigation.handleUnload(this);
  },

  rebuild(sourceDraft) {
    const draft = sourceDraft || draftStore.getDraft();
    const params = {
      serviceMode: draft.serviceMode,
      serviceTypeId: draft.selectedServiceTypeId,
      technicianId: draft.selectedTechnicianId,
      addressId: draft.selectedAddressId || ""
    };

    wx.showLoading({
      title: "加载中..."
    });

    fetchAppointmentSlots(params)
      .then((res) => {
        if (!res || res.code !== 200 || !res.data) {
          throw new Error("empty");
        }

        const data = res.data;
        const today = formatDate(new Date());
        const bookingStartDate = data.bookingStartDate || today;
        const bookingEndDate = data.bookingEndDate || bookingStartDate;
        const minLeadMinutes = Number(data.minLeadMinutes || 60);
        const bookingDays = Number(data.bookingDays || 0);
        const workWindows = normalizeWorkWindows(data.workWindows);
        const windowsByDay = groupWorkWindowsByDay(workWindows);
        const workWindowSummaryList = buildWorkWindowSummaryList(workWindows);
        const appointmentEditable = !(draft.editingOrderId && draft.canModifyAppointment === false);

        let selectedDate = "";
        let selectedTime = "";

        if (draft.selectedAppointmentTime) {
          const oldValue = toDateTimeText(Number(draft.selectedAppointmentTime));
          selectedDate = oldValue.dateText;
          selectedTime = oldValue.timeText;
        }

        let validation = appointmentEditable
          ? validateSelection(
              selectedDate,
              selectedTime,
              bookingStartDate,
              bookingEndDate,
              windowsByDay,
              minLeadMinutes
            )
          : {
              valid: !!(selectedDate && selectedTime),
              label: selectedDate && selectedTime ? `${selectedDate} ${selectedTime}` : ""
            };
        if (appointmentEditable && !validation.valid) {
          const first = findFirstAvailableSelection(
            bookingStartDate,
            bookingEndDate,
            windowsByDay,
            minLeadMinutes
          );
          selectedDate = first ? first.dateText : bookingStartDate;
          selectedTime = first ? first.timeText : "08:00";
          validation = validateSelection(
            selectedDate,
            selectedTime,
            bookingStartDate,
            bookingEndDate,
            windowsByDay,
            minLeadMinutes
          );
        }

        this._windowsByDay = windowsByDay;

        this.setData({
          serviceMode: Number(data.serviceMode || draft.serviceMode || 1),
          serviceModeName: data.serviceModeName || "上门服务",
          serviceTypeName: data.serviceTypeName || draft.selectedServiceTypeName || "",
          technicianName: data.technicianName || draft.selectedTechnicianName || "",
          addressDetail: data.addressDetail || draft.selectedAddressText || "",
          minLeadMinutes,
          limitHintText: appointmentEditable
            ? buildLimitHint(minLeadMinutes, bookingDays)
            : "师傅已上门，当前订单不可再修改预约时间",
          bookingStartDate,
          bookingEndDate,
          selectedDate,
          selectedTime,
          selectedAppointmentLabel: validation.valid ? validation.label : "",
          currentDayWindowText: getDayWindowText(selectedDate, windowsByDay),
          workWindows,
          workWindowSummaryList,
          hasAvailableWindow: workWindowSummaryList.length > 0,
          appointmentEditable,
          readonlyAppointmentTip: appointmentEditable ? "" : "师傅已上门，可继续下一步修改其他信息"
        });
      })
      .catch(() => {
        wx.showToast({
          title: "预约时间加载失败",
          icon: "none"
        });
      })
      .finally(() => {
        wx.hideLoading();
      });
  },

  onDateChange(e) {
    if (!this.data.appointmentEditable) {
      return;
    }
    const selectedDate = e.detail.value;
    const currentDayWindowText = getDayWindowText(selectedDate, this._windowsByDay || {});
    const validation = validateSelection(
      selectedDate,
      this.data.selectedTime,
      this.data.bookingStartDate,
      this.data.bookingEndDate,
      this._windowsByDay || {},
      this.data.minLeadMinutes
    );

    this.setData({
      selectedDate,
      currentDayWindowText,
      selectedAppointmentLabel: validation.valid ? validation.label : ""
    });

    if (!validation.valid) {
      const first = findFirstAvailableSelection(
        selectedDate,
        selectedDate,
        this._windowsByDay || {},
        this.data.minLeadMinutes
      );
      if (first) {
        const nextValidation = validateSelection(
          first.dateText,
          first.timeText,
          this.data.bookingStartDate,
          this.data.bookingEndDate,
          this._windowsByDay || {},
          this.data.minLeadMinutes
        );
        this.setData({
          selectedDate: first.dateText,
          selectedTime: first.timeText,
          selectedAppointmentLabel: nextValidation.valid ? nextValidation.label : "",
          currentDayWindowText: getDayWindowText(first.dateText, this._windowsByDay || {})
        });
      }
    }
  },

  onTimeChange(e) {
    if (!this.data.appointmentEditable) {
      return;
    }
    const selectedTime = e.detail.value;
    const validation = validateSelection(
      this.data.selectedDate,
      selectedTime,
      this.data.bookingStartDate,
      this.data.bookingEndDate,
      this._windowsByDay || {},
      this.data.minLeadMinutes
    );

    this.setData({
      selectedTime,
      selectedAppointmentLabel: validation.valid ? validation.label : ""
    });

    if (!validation.valid) {
      wx.showToast({
        title: validation.message,
        icon: "none"
      });
    }
  },

  onPrev() {
    flowNavigation.navigateBack(this);
  },

  onNext() {
    const draft = draftStore.getDraft();
    if (!this.data.appointmentEditable) {
      const selectedAppointmentLabel = this.data.selectedAppointmentLabel || draft.selectedAppointmentLabel || "";
      const selectedAppointmentTime = Number(draft.selectedAppointmentTime || toTimestamp(this.data.selectedDate, this.data.selectedTime) || 0);
      const selectedAppointmentId = draft.selectedAppointmentId || `${this.data.selectedDate}_${this.data.selectedTime}`;
      draftStore.saveDraft(
        Object.assign({}, draft, {
          selectedAppointmentId,
          selectedAppointmentLabel,
          selectedAppointmentTime
        })
      );

      wx.navigateTo({
        url: "/pages/order-flow/step6/index"
      });
      return;
    }

    const validation = validateSelection(
      this.data.selectedDate,
      this.data.selectedTime,
      this.data.bookingStartDate,
      this.data.bookingEndDate,
      this._windowsByDay || {},
      this.data.minLeadMinutes
    );

    if (!validation.valid) {
      wx.showToast({
        title: validation.message,
        icon: "none"
      });
      return;
    }

    const selectedAppointmentId = `${this.data.selectedDate}_${this.data.selectedTime}`;
    draftStore.saveDraft(
      Object.assign({}, draft, {
        selectedAppointmentId,
        selectedAppointmentLabel: validation.label,
        selectedAppointmentTime: Number(validation.timestamp || 0)
      })
    );

    wx.navigateTo({
      url: "/pages/order-flow/step6/index"
    });
  }
});
