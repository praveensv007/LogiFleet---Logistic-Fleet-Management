import {
  ArcElement, BarElement, CategoryScale, Chart as ChartJS, Filler, Legend, LinearScale, LineElement, PointElement, Tooltip,
} from 'chart.js';

ChartJS.register(ArcElement, BarElement, CategoryScale, Filler, Legend, LinearScale, LineElement, PointElement, Tooltip);
ChartJS.defaults.font.family = "'Inter', system-ui, sans-serif";
ChartJS.defaults.color = '#5b6478';
ChartJS.defaults.plugins.legend.labels.boxWidth = 12;

export const PALETTE = ['#1d3557', '#f4a261', '#2a9d8f', '#e76f51', '#457b9d', '#8d99ae', '#6a4c93', '#e9c46a', '#264653', '#b5838d', '#43aa8b'];
