package no.nav.melosys.domain.util;

import java.util.Arrays;

import no.nav.melosys.domain.Kodeverk;
import no.nav.melosys.exception.IkkeFunnetException;

public final class KodeverkUtils {

    private KodeverkUtils() {
        throw new UnsupportedOperationException();
    }

    public static <K extends Kodeverk> K dekod(Class<K> clazz, String kode) throws IkkeFunnetException {
        for (K k : clazz.getEnumConstants()) {
            if (k.getKode().equals(kode)) {
                return k;
            }
        }
        throw new IkkeFunnetException("Kodeverk med kode " + kode + " finnes ikke i " + clazz.getSimpleName());
    }

    public static <K extends Kodeverk> boolean erGyldigKode(Class<K> clazz, String kode) {
        try {
            dekod(clazz, kode);
            return true;
        } catch (IkkeFunnetException e) {
            return false;
        }
    }

    public static String[] hentAlleKoder(Class<? extends Kodeverk> clazz) {
        return Arrays.stream(clazz.getEnumConstants()).map(Kodeverk::getKode).toArray(String[]::new);
    }
}
