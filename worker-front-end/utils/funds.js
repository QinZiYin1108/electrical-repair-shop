export function safeToNumber(value) {
  const n = Number(value);
  return Number.isFinite(n) ? n : 0;
}

export function formatMoney(value) {
  return safeToNumber(value).toFixed(2);
}

export function formatFundTime(value) {
  const ts = safeToNumber(value);
  if (!ts) return '-';
  const date = new Date(ts);
  if (Number.isNaN(date.getTime())) return '-';
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, '0');
  const d = String(date.getDate()).padStart(2, '0');
  const h = String(date.getHours()).padStart(2, '0');
  const mm = String(date.getMinutes()).padStart(2, '0');
  return `${y}-${m}-${d} ${h}:${mm}`;
}

export function formatFundAmount(flowType, amount) {
  const type = safeToNumber(flowType);
  const n = formatMoney(amount);
  return `${type === 2 ? '-' : '+'}${n}`;
}

export function fundAmountClass(flowType) {
  return safeToNumber(flowType) === 2 ? 'is-expense' : 'is-income';
}

export function fundFlowDirection(flowType) {
  return safeToNumber(flowType) === 2 ? '支出' : '收入';
}