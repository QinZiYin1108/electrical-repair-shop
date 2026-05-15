export function getItem(key) {
  const value = localStorage.getItem(key);
  try {
    return JSON.parse(value);
  } catch (e) {
    return value;
  }
}

export function setItem(key, value) {
  const val = typeof value === 'string' ? value : JSON.stringify(value);
  localStorage.setItem(key, val);
}

export function removeItem(key) {
  localStorage.removeItem(key);
}

