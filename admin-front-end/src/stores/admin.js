import { defineStore } from 'pinia';
import { getAdminRole } from '../utils/auth';

export const useAdminStore = defineStore('admin', {
  state: () => ({
    id: '',
    name: '',
    email: '',
    adminType: null,
    adminRole: getAdminRole(),
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
      if (payload.adminRole !== undefined) {
        this.adminRole = payload.adminRole;
      }
      this.accountStatus = payload.accountStatus ?? null;
      this.avatar = payload.avatarUrl || '';
      this.loaded = true;
    },
    setAdminRole(role) {
      this.adminRole = role;
    },
    clear() {
      this.id = '';
      this.name = '';
      this.email = '';
      this.adminType = null;
      this.adminRole = null;
      this.accountStatus = null;
      this.avatar = '';
      this.loaded = false;
    }
  }
});

