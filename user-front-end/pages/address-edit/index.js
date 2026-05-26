const userAddressApi = require("../../api/userAddress");

const ADDRESS_TYPE_OPTIONS = [
  { value: 1, label: "家庭" },
  { value: 2, label: "公司" },
  { value: 3, label: "其他" }
];

const DEFAULT_MAP_LATITUDE = 39.90923;
const DEFAULT_MAP_LONGITUDE = 116.397428;
const REGION_RESOLVE_DELAY = 320;
const COORD_EPSILON = 0.00001;

Page({
  data: {
    isEdit: false,
    addressId: "",
    saving: false,
    locating: false,
    locationPicking: false,
    resolvingAddress: false,
    mapDragging: false,
    locationText: "",
    mapCenterLatitude: DEFAULT_MAP_LATITUDE,
    mapCenterLongitude: DEFAULT_MAP_LONGITUDE,
    mapScale: 16,
    addressTypeOptions: ADDRESS_TYPE_OPTIONS.map((item) => item.label),
    addressTypeIndex: 0,
    form: {
      contactName: "",
      contactPhone: "",
      province: "",
      city: "",
      district: "",
      street: "",
      detailedAddress: "",
      postalCode: "",
      isDefault: false,
      longitude: null,
      latitude: null
    }
  },

  onLoad(options) {
    this.lastResolveRequestId = 0;
    this.regionResolveTimer = null;
    this.ignoreRegionChangeUntil = 0;

    const addressId = options && options.id ? options.id : "";
    if (addressId) {
      this.setData({
        isEdit: true,
        addressId
      });
      wx.setNavigationBarTitle({
        title: "编辑地址"
      });
      this.loadAddressDetail(addressId);
      return;
    }

    wx.setNavigationBarTitle({
      title: "新增地址"
    });
    this.locateCurrentPosition();
  },

  onReady() {
    this.mapCtx = wx.createMapContext("addressMap", this);
  },

  onHide() {
    this.clearRegionResolveTimer();
    this.lastResolveRequestId = (this.lastResolveRequestId || 0) + 1;
    this.setMapDraggingState(false);
  },

  onUnload() {
    this.clearRegionResolveTimer();
    this.lastResolveRequestId = (this.lastResolveRequestId || 0) + 1;
    this.setMapDraggingState(false);
    this.mapCtx = null;
  },

  loadAddressDetail(addressId) {
    wx.showLoading({
      title: "加载中..."
    });
    userAddressApi
      .getUserAddressDetail(addressId)
      .then((resp) => {
        if (resp.code !== 200 || !resp.data) {
          wx.showToast({
            title: resp.message || "地址详情加载失败",
            icon: "none"
          });
          return;
        }

        const data = resp.data;
        const typeValue = this.normalizeAddressType(data.addressType);
        const latitude = this.normalizeCoordinate(data.latitude);
        const longitude = this.normalizeCoordinate(data.longitude);
        const fullAddress = [data.province, data.city, data.district, data.street]
          .filter((item) => !!item)
          .join("");

        this.setData({
          addressTypeIndex: this.findTypeIndex(typeValue),
          locationText: fullAddress,
          form: {
            contactName: data.contactName || "",
            contactPhone: data.contactPhone || "",
            province: data.province || "",
            city: data.city || "",
            district: data.district || "",
            street: data.street || "",
            detailedAddress: data.detailedAddress || "",
            postalCode: data.postalCode || "",
            isDefault: data.isDefault === 1,
            longitude,
            latitude
          }
        });

        if (latitude !== null && longitude !== null) {
          this.setMapCenter(latitude, longitude);
          if (!fullAddress) {
            this.resolveAddressByCoordinate(latitude, longitude, {
              keepCurrentDetail: true,
              silentError: true
            });
          }
          return;
        }
        this.locateCurrentPosition();
      })
      .catch(() => {
        wx.showToast({
          title: "地址详情加载失败",
          icon: "none"
        });
      })
      .finally(() => {
        wx.hideLoading();
      });
  },

  onRelocateTap() {
    this.locateCurrentPosition();
  },

  locateCurrentPosition() {
    if (this.data.locating) {
      return;
    }

    this.ensurePrivacyAuthorize()
      .catch((err) => {
        this.handlePrivacyAuthFail(err);
        throw {
          __privacyStopped: true
        };
      })
      .then(() => {
        this.setData({
          locating: true
        });
        return this.requestCurrentLocation(true).catch((err) => {
          if (this.shouldRetryWithNormalLocation(err)) {
            return this.requestCurrentLocation(false);
          }
          throw err;
        });
      })
      .then((res) => {
        if (!res) {
          return;
        }
        const latitude = this.normalizeCoordinate(res.latitude);
        const longitude = this.normalizeCoordinate(res.longitude);
        if (latitude === null || longitude === null) {
          this.showValidateToast("定位失败，请重试");
          return;
        }
        this.updateLocationByCoordinate(latitude, longitude, {
          forceAutoDetail: true,
          silentError: false
        });
      })
      .catch((err) => {
        if (err && err.__privacyStopped) {
          return;
        }
        this.handleLocateFail(err);
      })
      .finally(() => {
        if (this.data.locating) {
          this.setData({
            locating: false
          });
        }
      });
  },

  requestCurrentLocation(useHighAccuracy) {
    return new Promise((resolve, reject) => {
      const options = {
        type: "gcj02",
        success: resolve,
        fail: reject
      };
      if (useHighAccuracy) {
        options.isHighAccuracy = true;
      }
      wx.getLocation(options);
    });
  },

  shouldRetryWithNormalLocation(err) {
    const errorMsg = (err && err.errMsg ? err.errMsg : "").toLowerCase();
    if (!errorMsg) {
      return false;
    }
    return (
      errorMsg.includes("high accuracy") ||
      errorMsg.includes("accuracy") ||
      errorMsg.includes("timeout")
    );
  },

  ensurePrivacyAuthorize() {
    return new Promise((resolve, reject) => {
      if (typeof wx.requirePrivacyAuthorize !== "function") {
        resolve();
        return;
      }
      wx.requirePrivacyAuthorize({
        success: resolve,
        fail: reject
      });
    });
  },

  handlePrivacyAuthFail(err) {
    console.error("[address-edit] privacy authorize fail", err);
    wx.showModal({
      title: "请先同意隐私提示",
      content: "需要隐私授权才能使用定位和地图选点",
      confirmText: "我知道了"
    });
  },

  handleLocateFail(err) {
    console.error("[address-edit] getLocation fail", err);
    const errorMsg = err && err.errMsg ? err.errMsg : "";
    const lower = errorMsg.toLowerCase();

    if (lower.includes("privacy") || lower.includes("requiredprivateinfos")) {
      this.handlePrivacyAuthFail(err);
      return;
    }

    if (
      lower.includes("auth deny") ||
      lower.includes("auth denied") ||
      lower.includes("authorize") ||
      lower.includes("permission denied") ||
      lower.includes("system permission denied")
    ) {
      wx.showModal({
        title: "定位权限未开启",
        content: "请开启定位权限，或直接使用地图选点",
        confirmText: "去设置",
        cancelText: "地图选点",
        success: (res) => {
          if (res.confirm) {
            wx.openSetting();
            return;
          }
          this.onChooseLocationTap();
        }
      });
      return;
    }

    wx.showToast({
      title: "获取定位失败",
      icon: "none"
    });
  },

  onMapRegionChange(e) {
    if (!e) {
      return;
    }

    const now = Date.now();
    const causedBy = (e.causedBy || "").toLowerCase();
    const causedByUpdate = causedBy === "update";

    if (e.type === "begin") {
      if (
        !causedByUpdate &&
        !this.data.locationPicking &&
        !this.data.resolvingAddress &&
        !this.data.locating &&
        now >= this.ignoreRegionChangeUntil
      ) {
        this.setMapDraggingState(true);
      }
      return;
    }

    if (e.type !== "end") {
      return;
    }

    this.setMapDraggingState(false);

    if (this.data.locationPicking || this.data.resolvingAddress || this.data.locating) {
      return;
    }
    if (causedByUpdate) {
      return;
    }
    if (now < this.ignoreRegionChangeUntil) {
      return;
    }
    this.scheduleMapCenterResolve();
  },

  onMapTouchStart() {
    if (this.data.locationPicking || this.data.resolvingAddress || this.data.locating) {
      return;
    }
    this.setMapDraggingState(true);
  },

  onMapTouchEnd() {
    this.setMapDraggingState(false);
  },

  onMapTouchCancel() {
    this.setMapDraggingState(false);
  },

  scheduleMapCenterResolve() {
    this.clearRegionResolveTimer();
    this.regionResolveTimer = setTimeout(() => {
      this.resolveFromMapCenter();
    }, REGION_RESOLVE_DELAY);
  },

  clearRegionResolveTimer() {
    if (this.regionResolveTimer) {
      clearTimeout(this.regionResolveTimer);
      this.regionResolveTimer = null;
    }
  },

  setMapDraggingState(mapDragging) {
    const next = !!mapDragging;
    if (!!this.data.mapDragging === next) {
      return;
    }
    this.setData({
      mapDragging: next
    });
  },

  resolveFromMapCenter() {
    if (this.data.locationPicking) {
      return;
    }
    if (!this.mapCtx) {
      this.mapCtx = wx.createMapContext("addressMap", this);
    }
    if (!this.mapCtx) {
      return;
    }
    this.mapCtx.getCenterLocation({
      success: (res) => {
        const latitude = this.normalizeCoordinate(res.latitude);
        const longitude = this.normalizeCoordinate(res.longitude);
        if (latitude === null || longitude === null) {
          return;
        }
        if (!this.hasCoordinateChanged(latitude, longitude)) {
          return;
        }
        this.updateLocationByCoordinate(latitude, longitude, {
          forceAutoDetail: true,
          silentError: true
        });
      }
    });
  },

  onChooseLocationTap() {
    if (this.data.locationPicking) {
      return;
    }

    this.ensurePrivacyAuthorize()
      .then(() => {
        this.openChooseLocation();
      })
      .catch((err) => {
        this.handlePrivacyAuthFail(err);
      });
  },

  openChooseLocation() {
    this.clearRegionResolveTimer();
    this.ignoreRegionChangeUntil = Date.now() + 3000;

    const latitude = this.normalizeCoordinate(this.data.form.latitude);
    const longitude = this.normalizeCoordinate(this.data.form.longitude);
    const options = {
      success: (res) => {
        const nextLat = this.normalizeCoordinate(res.latitude);
        const nextLng = this.normalizeCoordinate(res.longitude);
        if (nextLat === null || nextLng === null) {
          wx.showToast({
            title: "选点失败，请重试",
            icon: "none"
          });
          return;
        }
        this.updateLocationByCoordinate(nextLat, nextLng, {
          forceAutoDetail: true,
          poiName: this.trimText(res.name),
          fallbackAddress: this.trimText(res.address),
          silentError: false
        });
      },
      fail: (err) => {
        this.handleChooseLocationFail(err);
      },
      complete: () => {
        this.ignoreRegionChangeUntil = Date.now() + 1200;
        this.setData({
          locationPicking: false
        });
      }
    };

    if (latitude !== null && longitude !== null) {
      options.latitude = latitude;
      options.longitude = longitude;
    }

    this.setData({
      locationPicking: true
    });

    try {
      wx.chooseLocation(options);
    } catch (err) {
      this.setData({
        locationPicking: false
      });
      this.handleChooseLocationFail(err || {});
    }
  },

  handleChooseLocationFail(err) {
    console.error("[address-edit] chooseLocation fail", err);
    const errorMsg = err && err.errMsg ? err.errMsg : "";
    const lower = errorMsg.toLowerCase();
    if (lower.includes("cancel")) {
      return;
    }
    if (
      lower.includes("auth deny") ||
      lower.includes("authorize") ||
      lower.includes("auth denied") ||
      lower.includes("permission denied") ||
      lower.includes("privacy")
    ) {
      this.handleLocateFail(err);
      return;
    }
    wx.showToast({
      title: "地图选点失败",
      icon: "none"
    });
  },

  hasCoordinateChanged(latitude, longitude) {
    const oldLat = this.normalizeCoordinate(this.data.form.latitude);
    const oldLng = this.normalizeCoordinate(this.data.form.longitude);
    if (oldLat === null || oldLng === null) {
      return true;
    }
    return (
      Math.abs(oldLat - latitude) > COORD_EPSILON ||
      Math.abs(oldLng - longitude) > COORD_EPSILON
    );
  },

  setMapCenter(latitude, longitude) {
    this.ignoreRegionChangeUntil = Date.now() + 700;
    this.setData({
      mapCenterLatitude: latitude,
      mapCenterLongitude: longitude,
      "form.latitude": latitude,
      "form.longitude": longitude
    });
  },

  updateLocationByCoordinate(latitude, longitude, options) {
    this.setMapCenter(latitude, longitude);
    this.resolveAddressByCoordinate(latitude, longitude, options || {});
  },

  resolveAddressByCoordinate(latitude, longitude, options) {
    const reqId = (this.lastResolveRequestId || 0) + 1;
    this.lastResolveRequestId = reqId;
    this.setData({
      resolvingAddress: true
    });

    userAddressApi
      .reverseGeocodeUserAddress({
        latitude,
        longitude
      })
      .then((resp) => {
        if (reqId !== this.lastResolveRequestId) {
          return;
        }
        if (resp.code !== 200 || !resp.data) {
          if (!options.silentError) {
            wx.showToast({
              title: resp.message || "地址解析失败",
              icon: "none"
            });
          }
          return;
        }

        const data = resp.data;
        const province = this.trimText(data.province);
        const city = this.trimText(data.city || (province.endsWith("市") ? province : ""));
        const district = this.trimText(data.district);
        const street = this.trimText(data.street);
        const locationText = [province, city, district, street].filter((item) => !!item).join("");

        const patch = {
          "form.province": province,
          "form.city": city,
          "form.district": district,
          "form.street": street,
          locationText
        };

        const currentDetail = this.trimText(this.data.form.detailedAddress);
        const autoDetail =
          this.trimText(options.poiName) ||
          this.trimText(options.fallbackAddress) ||
          this.trimText(data.street) ||
          this.trimText(data.fullAddress);
        let nextDetail = currentDetail;

        if (options.forceAutoDetail) {
          nextDetail = autoDetail || currentDetail;
        } else if (!options.keepCurrentDetail) {
          nextDetail = autoDetail || currentDetail;
        } else if (!currentDetail) {
          nextDetail = autoDetail;
        }

        if (nextDetail !== currentDetail) {
          patch["form.detailedAddress"] = nextDetail;
        }

        this.setData(patch);
      })
      .catch(() => {
        if (!options.silentError) {
          wx.showToast({
            title: "地址解析失败",
            icon: "none"
          });
        }
      })
      .finally(() => {
        if (reqId !== this.lastResolveRequestId) {
          return;
        }
        this.setData({
          resolvingAddress: false
        });
      });
  },

  onFieldChange(e) {
    const field = e.currentTarget.dataset.field;
    if (!field) {
      return;
    }
    this.setData({
      [`form.${field}`]: e.detail
    });
  },

  onAddressTypeChange(e) {
    const index = Number(e.detail.value || 0);
    this.setData({
      addressTypeIndex: index
    });
  },

  onDefaultChange(e) {
    this.setData({
      "form.isDefault": !!e.detail.value
    });
  },

  onSave() {
    if (this.data.saving) {
      return;
    }
    const payload = this.buildPayload();
    if (!payload) {
      return;
    }

    this.setData({
      saving: true
    });
    wx.showLoading({
      title: "保存中..."
    });

    const request = this.data.isEdit
      ? userAddressApi.updateUserAddress(payload)
      : userAddressApi.createUserAddress(payload);

    request
      .then((resp) => {
        if (resp.code === 200) {
          wx.showToast({
            title: this.data.isEdit ? "更新成功" : "新增成功",
            icon: "success"
          });
          setTimeout(() => {
            wx.navigateBack();
          }, 400);
          return;
        }
        wx.showToast({
          title: resp.message || "保存失败",
          icon: "none"
        });
      })
      .catch(() => {
        wx.showToast({
          title: "保存失败",
          icon: "none"
        });
      })
      .finally(() => {
        this.setData({
          saving: false
        });
        wx.hideLoading();
      });
  },

  buildPayload() {
    const form = this.data.form;
    const payload = {
      contactName: this.trimText(form.contactName),
      contactPhone: this.trimText(form.contactPhone),
      province: this.trimText(form.province),
      city: this.trimText(form.city),
      district: this.trimText(form.district),
      street: this.trimText(form.street),
      detailedAddress: this.trimText(form.detailedAddress),
      postalCode: this.trimText(form.postalCode),
      isDefault: form.isDefault ? 1 : 0,
      addressType: ADDRESS_TYPE_OPTIONS[this.data.addressTypeIndex]
        ? ADDRESS_TYPE_OPTIONS[this.data.addressTypeIndex].value
        : 1
    };

    if (!payload.contactName) {
      this.showValidateToast("请填写联系人");
      return null;
    }
    if (!payload.contactPhone) {
      this.showValidateToast("请填写联系电话");
      return null;
    }
    if (!/^1\d{10}$/.test(payload.contactPhone)) {
      this.showValidateToast("请输入正确手机号");
      return null;
    }
    if (!payload.province || !payload.city || !payload.district) {
      this.showValidateToast("请先通过地图选择地址");
      return null;
    }
    if (!payload.detailedAddress) {
      this.showValidateToast("请填写详细地址");
      return null;
    }

    const longitudeResult = this.parseCoordinate(form.longitude, "经度");
    if (!longitudeResult.ok) {
      return null;
    }
    payload.longitude = longitudeResult.value;

    const latitudeResult = this.parseCoordinate(form.latitude, "纬度");
    if (!latitudeResult.ok) {
      return null;
    }
    payload.latitude = latitudeResult.value;

    if (payload.longitude === null || payload.latitude === null) {
      this.showValidateToast("请先定位或地图选点");
      return null;
    }

    if (this.data.isEdit) {
      payload.id = this.data.addressId;
    }
    return payload;
  },

  parseCoordinate(value, name) {
    const text = this.trimText(value);
    if (!text) {
      return {
        ok: true,
        value: null
      };
    }
    const num = Number(text);
    if (!Number.isFinite(num)) {
      this.showValidateToast(`${name}格式不正确`);
      return {
        ok: false,
        value: null
      };
    }
    return {
      ok: true,
      value: num
    };
  },

  normalizeCoordinate(value) {
    const text = this.trimText(value);
    if (!text) {
      return null;
    }
    const num = Number(text);
    if (!Number.isFinite(num)) {
      return null;
    }
    return num;
  },

  trimText(value) {
    if (value === null || value === undefined) {
      return "";
    }
    return String(value).trim();
  },

  showValidateToast(title) {
    wx.showToast({
      title,
      icon: "none"
    });
  },

  normalizeAddressType(value) {
    if (value === 2 || value === 3) {
      return value;
    }
    return 1;
  },

  findTypeIndex(value) {
    const idx = ADDRESS_TYPE_OPTIONS.findIndex((item) => item.value === value);
    return idx >= 0 ? idx : 0;
  }
});
