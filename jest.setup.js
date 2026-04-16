// React 19 gates act() warnings behind this flag; enabling it matches how
// react-test-renderer expects to be driven from test environments.
globalThis.IS_REACT_ACT_ENVIRONMENT = true;
