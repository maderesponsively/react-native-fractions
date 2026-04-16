module.exports = {
  testEnvironment: 'node',
  moduleFileExtensions: ['ts', 'tsx', 'js', 'jsx', 'json'],
  testMatch: ['**/__tests__/**/*.test.[jt]s?(x)'],
  testPathIgnorePatterns: ['/node_modules/', '/android/', '/ios/', '/lib/'],
  transform: {
    '^.+\\.(js|jsx|ts|tsx)$': 'babel-jest',
  },
  transformIgnorePatterns: [
    'node_modules/(?!((jest-)?react-native|@react-native|react-native)/)',
  ],
  globals: {
    __DEV__: true,
    IS_REACT_ACT_ENVIRONMENT: true,
  },
  setupFiles: ['<rootDir>/jest.setup.js'],
  clearMocks: true,
  watchman: false,
  haste: {
    enableSymlinks: false,
  },
};
