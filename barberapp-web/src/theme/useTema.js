import { createContext, useContext } from 'react';

export const ThemeContext = createContext(null);

export function useTema() {
    const contexto = useContext(ThemeContext);

    if (!contexto) {
        throw new Error('useTema deve ser usado dentro de ThemeProvider');
    }

    return contexto;
}
