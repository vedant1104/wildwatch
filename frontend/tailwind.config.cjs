module.exports = {
  content: [
    './app/**/*.{js,ts,jsx,tsx}',
    './components/**/*.{js,ts,jsx,tsx}'
  ],
  theme: {
    extend: {
      colors: {
        cream: '#F3EEE1',
        ink: '#1C231E',
        canopyDark: '#1B2E18',
        canopyMid: '#2E4A27',
        canopyLight: '#4C7038',
        canopyVivid: '#5C8A3A',
        amber: '#E0A247',
        tiger: '#C6702B',
        river: '#2F7A78',
        riverLight: '#6FB2AB',
        card: '#EAE3D0',
        line: '#CDC2A3'
      },
      fontFamily: {
        serif: ['Georgia', 'serif'],
        ui: ['ui-sans-serif', 'system-ui', 'sans-serif']
      }
    }
  },
  plugins: [],
};
