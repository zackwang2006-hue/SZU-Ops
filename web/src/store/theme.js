import { defineStore } from 'pinia'

const THEME_KEY = 'szu_theme'
const THEMES = ['arco', 'material', 'dark']

export const useThemeStore = defineStore('theme', {
  state: () => ({
    theme: 'arco'
  }),
  actions: {
    setTheme(theme) {
      if (!THEMES.includes(theme)) theme = 'arco'
      this.theme = theme
      document.documentElement.dataset.theme = theme
      localStorage.setItem(THEME_KEY, theme)
    },
    initTheme() {
      const saved = localStorage.getItem(THEME_KEY)
      this.setTheme(THEMES.includes(saved) ? saved : 'arco')
    }
  }
})
