import js from '@eslint/js'
import reactRefresh from 'eslint-plugin-react-refresh'
import reactHooks from 'eslint-plugin-react-hooks'
import tseslint from 'typescript-eslint'

export default tseslint.config(
    {
        ignores: ['dist', 'node_modules'],
    },
    js.configs.recommended,
    ...tseslint.configs.recommended,
    {
        plugins: {
            'react-refresh': reactRefresh,
            'react-hooks': reactHooks,
        },
        rules: {
            'react-refresh/only-export-components': 'warn',
            'react-hooks/rules-of-hooks': 'error',
            'react-hooks/exhaustive-deps': 'warn',
            '@typescript-eslint/no-explicit-any': 'off',
            '@typescript-eslint/no-unused-vars': ['warn', { argsIgnorePattern: '^_', varsIgnorePattern: '^_' }],
        },
    },
)
