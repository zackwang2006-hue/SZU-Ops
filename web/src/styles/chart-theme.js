// ECharts 主题色板（与 DESIGN.md themes.*.chart 对应）
export const CHART_PALETTES = {
  arco: {
    color: ['#165DFF', '#00B42A', '#FF7D00', '#F53F3F', '#722ED1', '#0FC6C2'],
    text: '#4E5969',
    axisLine: '#E5E6EB',
    splitLine: '#F2F3F5',
    tooltipBg: '#FFFFFF',
    tooltipText: '#1D2129'
  },
  material: {
    color: ['#6750A4', '#006A6A', '#7D5700', '#BA1A1A', '#0061A4', '#984061'],
    text: '#49454F',
    axisLine: '#CAC4D0',
    splitLine: '#F3EDF7',
    tooltipBg: '#FFFFFF',
    tooltipText: '#1D1B20'
  },
  dark: {
    color: ['#58A6FF', '#3FB950', '#D29922', '#F85149', '#BC8CFF', '#39C5CF'],
    text: '#8B949E',
    axisLine: '#21262D',
    splitLine: '#21262D',
    tooltipBg: '#161B22',
    tooltipText: '#C9D1D9'
  }
}

export const getChartPalette = (theme) => CHART_PALETTES[theme] || CHART_PALETTES.arco
