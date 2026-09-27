<script>
import { getWorkerAccountInfo } from '@/api/workerAccount';

export default {
  globalData: {
    workerIsLogin: false,
    workerInfo: null,
    workerHasUnread: false
  },
  onLaunch: function () {
    console.log('App Launch');
    this.checkLoginState();
  },
  onShow: function () {
    console.log('App Show');
  },
  onHide: function () {
    console.log('App Hide');
  },
  methods: {
    checkLoginState() {
      const token = uni.getStorageSync('workerToken');
      if (!token) {
        this.globalData.workerIsLogin = false;
        return;
      }
      // 验证 token 有效性
      getWorkerAccountInfo()
        .then((res) => {
          if (res && res.code === 200 && res.data) {
            this.globalData.workerIsLogin = true;
            this.globalData.workerInfo = res.data;
          } else {
            this.clearLoginState();
          }
        })
        .catch(() => {
          this.clearLoginState();
        });
    },
    clearLoginState() {
      this.globalData.workerIsLogin = false;
      this.globalData.workerInfo = null;
      uni.removeStorageSync('workerToken');
    }
  }
};
</script>

<style>
/*每个页面公共css */
</style>
