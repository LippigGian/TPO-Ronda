package com.example.ronda;

import com.google.android.material.textfield.TextInputEditText;

/**
 * Conversiones entre los widgets de dialog_filtros.xml y los valores de FiltrosHome.
 * Las usa HomeActivity (aplicar filtros al Home) y MisFavoritosActivity (editar una
 * búsqueda guardada), que reutilizan el mismo layout de diálogo.
 */
public final class FiltrosDialogHelper {
    private FiltrosDialogHelper() { }

    public static Double leerNumero(TextInputEditText campo) {
        String ingresado = campo.getText() != null ? campo.getText().toString().trim() : "";
        if (ingresado.isEmpty()) {
            return null;
        }
        try {
            return Double.parseDouble(ingresado);
        } catch (NumberFormatException error) {
            return null;
        }
    }

    public static String numeroSinDecimales(double valor) {
        return valor % 1 == 0 ? String.valueOf((long) valor) : String.valueOf(valor);
    }

    public static int chipDeEstado(String estado) {
        if ("NUEVO".equals(estado)) return R.id.chipEstadoNuevo;
        if ("COMO_NUEVO".equals(estado)) return R.id.chipEstadoComoNuevo;
        if ("USADO".equals(estado)) return R.id.chipEstadoUsado;
        return R.id.chipEstadoTodos;
    }

    public static String estadoDeChip(int chipId) {
        if (chipId == R.id.chipEstadoNuevo) return "NUEVO";
        if (chipId == R.id.chipEstadoComoNuevo) return "COMO_NUEVO";
        if (chipId == R.id.chipEstadoUsado) return "USADO";
        return null;
    }

    public static int chipDeRadio(double radioKm) {
        if (radioKm <= 5) return R.id.chipRadio5;
        if (radioKm >= 25) return R.id.chipRadio25;
        return R.id.chipRadio10;
    }

    public static double radioDeChip(int chipId) {
        if (chipId == R.id.chipRadio5) return 5.0;
        if (chipId == R.id.chipRadio25) return 25.0;
        return FiltrosHome.RADIO_KM_POR_DEFECTO;
    }
}
