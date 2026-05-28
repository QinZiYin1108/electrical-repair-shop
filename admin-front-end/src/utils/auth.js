export function getToken() {
  return localStorage.getItem('token');
}

export function setToken(token) {
  localStorage.setItem('token', token);
}

export function clearToken() {
  localStorage.removeItem('token');
}

export function getAdminRole() {
  const v = localStorage.getItem('adminRole');
  return v ? parseInt(v, 10) : null;
}

export function setAdminRole(role) {
  if (role != null) {
    localStorage.setItem('adminRole', String(role));
  } else {
    localStorage.removeItem('adminRole');
  }
}

export function getStoreId() {
  const token = localStorage.getItem('token');
  if (!token) return null;
  try {
    const payload = JSON.parse(atob(token.split('.')[1]));
    return payload.storeId || null;
  } catch (e) {
    return null;
  }
}

export function clearAuth() {
  localStorage.removeItem('token');
  localStorage.removeItem('adminRole');
}
