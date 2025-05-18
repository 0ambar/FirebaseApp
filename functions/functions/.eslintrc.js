module.exports = {
  env: {
    es6: true,
    node: true,
  },
  parserOptions: {
    "ecmaVersion": 2018,
  },
  extends: [
    // Comentamos los extends para desactivar reglas
    // "eslint:recommended",
    // "google",
  ],
  rules: {
    // Desactivar todas las reglas poniendo "off"
    "no-restricted-globals": "off",
    "prefer-arrow-callback": "off",
    "quotes": "off",
    "object-curly-spacing": "off",
    "indent": "off",
    "comma-dangle": "off",
    "max-len": "off",
    "no-trailing-spaces": "off",
  },
  overrides: [
    {
      files: ["**/*.spec.*"],
      env: {
        mocha: true,
      },
      rules: {},
    },
  ],
  globals: {},
};
