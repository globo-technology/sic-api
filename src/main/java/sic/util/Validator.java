package sic.util;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

public class Validator {

    public static boolean esVacio(String campo) {
        if (campo == null) return true;
        return campo.equals("");
    }

    public static boolean esEmailValido(String cadena) {
        if (cadena == null || cadena.equals("")) {
            return false;
        } else {
            return ((Pattern.compile("^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@"
                    + "[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$")).matcher(cadena)).matches();
        }
    }

    /**
     * Compara dos fechas pasadas como Date
     *
     * @param fechaAnterior
     * @param fechaSiguiente
     * @return 0 si es igual, menor a 0 si fechaAnterior esta antes de
     * fechaSiguiente, mayor a 0 si fechaAnterior esta despues de fechaSiguiente
     */
    public static int compararFechas(Date fechaAnterior, Date fechaSiguiente) {
        Calendar anterior = Calendar.getInstance();
        Calendar siguiente = Calendar.getInstance();
        anterior.setTime(fechaAnterior);
        siguiente.setTime(fechaSiguiente);
        return siguiente.compareTo(anterior);
    }
    
    /**
     * Compara dos fechas teniendo en cuenta solo los dias sin hora
     * 
     * @param fechaAnterior
     * @param fechaSiguiente
     * @return -1 si la fechaAnterior esta antes de la fechaSiguiente, 0 si son iguales,
     * o 1 si la fechaAnterior esta despues de la fechaSiguiente
     */
    public static int compararDias(Date fechaAnterior, Date fechaSiguiente) {
        LocalDate diaAnterior = fechaAnterior.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate diaSiguiente = fechaSiguiente.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        return diaAnterior.compareTo(diaSiguiente);
    }
    
    public static boolean tieneDuplicados(long[] array) {
        Set<Long> set = new HashSet<>();
        for (long i : array) {
            if (set.contains(i)) {
                return true;
            }
            set.add(i);
        }
        return false;
    }
}
