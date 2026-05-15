const { showUploadErrorModal } = require('../../utils/uploadFeedback');

Page({
  data: {
    loading: true,
    editMode: false,
    user: {
      id: '',
      username: '',
      phone: '',
      email: '',
      avatarUrl: '',
      status: null
    },
    profile: {
      realName: '',
      gender: null,
      genderInput: '',
      profession: '',
      birthday: '',
      emergencyContact: '',
      emergencyPhone: ''
    },
    statusText: '',
    genderText: '',
    genderOptions: [
      { value: 1, label: '男' },
      { value: 2, label: '女' },
      { value: 3, label: '未知' }
    ],
    today: ''
  },

  onLoad() {
    const date = new Date();
    const y = date.getFullYear();
    const m = String(date.getMonth() + 1).padStart(2, '0');
    const d = String(date.getDate()).padStart(2, '0');
    this.setData({
      today: `${y}-${m}-${d}`
    });
  },

  onShow() {
    this.loadProfile();
  },

  onGenderPickerChange(e) {
    const index = e.detail.value;
    const option = this.data.genderOptions[index];
    if (!option) {
      return;
    }
    this.setData({
      'profile.gender': option.value,
      'profile.genderInput': String(option.value),
      genderText: option.label
    });
  },

  onBirthdayPickerChange(e) {
    const value = e.detail.value;
    this.setData({
      'profile.birthday': value
    });
  },

  loadProfile() {
    const userProfileApi = require('../../api/userProfile');
    userProfileApi
      .getUserProfileDetail()
      .then((resp) => {
        if (resp.code === 200 && resp.data) {
          const data = resp.data;
          const user = {
            id: data.id || '',
            username: data.username || '',
            phone: data.phone || '',
            email: data.email || '',
            avatarUrl: data.avatarUrl || '',
            status: data.status || null
          };
          const profile = {
            realName: data.realName || '',
            gender: data.gender || null,
            genderInput: data.gender != null ? String(data.gender) : '',
            profession: data.profession || '',
            birthday: data.birthday || '',
            emergencyContact: data.emergencyContact || '',
            emergencyPhone: data.emergencyPhone || ''
          };
          this.setData({
            user,
            profile,
            statusText: this.mapStatusText(user.status),
            genderText: this.mapGenderText(profile.gender),
            loading: false
          });
        } else {
          this.setData({
            loading: false
          });
        }
      })
      .catch(() => {
        this.setData({
          loading: false
        });
      });
  },

  onEditTap() {
    this.setData({
      editMode: true
    });
  },

  onSaveTap() {
    const userProfileApi = require('../../api/userProfile');
    const genderVal = this.parseGender(this.data.profile.genderInput);
    const payload = {
      username: this.data.user.username,
      phone: this.data.user.phone,
      realName: this.data.profile.realName,
      gender: genderVal,
      profession: this.data.profile.profession,
      birthday: this.data.profile.birthday,
      emergencyContact: this.data.profile.emergencyContact,
      emergencyPhone: this.data.profile.emergencyPhone
    };
    wx.showLoading({
      title: '保存中...'
    });
    userProfileApi
      .updateUserProfile(payload)
      .then((resp) => {
        if (resp.code === 200) {
          wx.showToast({
            title: '已保存',
            icon: 'success'
          });
          this.setData({
            editMode: false,
            'profile.gender': genderVal,
            genderText: this.mapGenderText(genderVal)
          });
        } else {
          wx.showToast({
            title: resp.message || '保存失败',
            icon: 'none'
          });
        }
      })
      .catch(() => {
        wx.showToast({
          title: '保存失败',
          icon: 'none'
        });
      })
      .finally(() => {
        wx.hideLoading();
      });
  },

  onFieldChange(e) {
    const field = e.currentTarget.dataset.field;
    const value = e.detail;
    if (!field) {
      return;
    }
    this.setData({
      [field]: value
    });
  },

  onChangeAvatar() {
    const userProfileApi = require('../../api/userProfile');
    wx.chooseImage({
      count: 1,
      sizeType: ['compressed'],
      sourceType: ['album', 'camera'],
      success: (res) => {
        const filePath = res.tempFilePaths[0];
        wx.showLoading({
          title: '上传中...'
        });
        userProfileApi
          .uploadUserAvatar(filePath)
          .then((resp) => {
            if (resp.code === 200 && resp.data) {
              this.setData({
                'user.avatarUrl': resp.data
              });
              wx.showToast({
                title: '头像已更新',
                icon: 'success'
              });
            } else {
              showUploadErrorModal(resp.message || '头像上传失败', {
                title: '头像上传失败',
                fallback: '头像上传失败'
              });
            }
          })
          .catch((error) => {
            showUploadErrorModal(error, {
              title: '头像上传失败',
              fallback: '头像上传失败'
            });
          })
          .finally(() => {
            wx.hideLoading();
          });
      }
    });
  },

  mapStatusText(status) {
    if (status === 1) {
      return '正常';
    }
    if (status === 2) {
      return '冻结';
    }
    if (status === 3) {
      return '注销申请中';
    }
    if (status === 4) {
      return '已注销';
    }
    return '未知';
  },

  mapGenderText(gender) {
    if (gender === 1) {
      return '男';
    }
    if (gender === 2) {
      return '女';
    }
    if (gender === 3) {
      return '未知';
    }
    return '';
  },

  parseGender(input) {
    if (!input) {
      return null;
    }
    const trimmed = String(input).trim();
    if (trimmed === '男') {
      return 1;
    }
    if (trimmed === '女') {
      return 2;
    }
    if (trimmed === '未知') {
      return 3;
    }
    const num = parseInt(trimmed, 10);
    if (num === 1 || num === 2 || num === 3) {
      return num;
    }
    return null;
  }
});
