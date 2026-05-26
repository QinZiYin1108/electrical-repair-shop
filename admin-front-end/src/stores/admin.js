import { defineStore } from 'pinia';

export const useAdminStore = defineStore('admin', {
  state: () => ({
    id: '',
    name: '',
    email: '',
    adminType: null,
    accountStatus: null,
    avatar: '',
    loaded: false
  }),
  actions: {
    setInfo(payload) {
      this.id = payload.id || '';
      this.name = payload.username || '';
      this.email = payload.email || '';
      this.adminType = payload.adminType ?? null;
      this.accountStatus = payload.accountStatus ?? null;
      this.avatar = payload.avatarUrl || '';
      this.loaded = true;
    },
    clear() {
      this.id = '';
      this.name = '';
      this.email = '';
      this.adminType = null;
      this.accountStatus = null;
      this.avatar = '';
      this.loaded = false;
    }
  }
});

