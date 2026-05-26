import { ref } from 'vue';

const isDark = ref(false);

export function useTheme() {
  const toggleTheme = () => {
    isDark.value = !isDark.value;
    document.documentElement.setAttribute('data-theme', isDark.value ? 'dark' : 'light');
  };

  return {
    isDark,
    toggleTheme
  };
}

