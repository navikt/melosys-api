package no.nav.melosys.domain.util;

import no.nav.melosys.domain.Kodeverk;
import no.nav.melosys.exception.IkkeFunnetException;
import org.springframework.util.Assert;

public final class KodeverkUtils {

    private KodeverkUtils() {
        throw new UnsupportedOperationException();
    }

    public static <K extends Kodeverk> K hentKodeverk(Class<K> clazz, String kode) throws IkkeFunnetException {
        for (K k: clazz.getEnumConstants()) {
            if (k.getKode().equals(kode)) {
                return k;
            }
        }
        throw new IkkeFunnetException("Kodeverk med kode " + kode + " finnes ikke i " + clazz.getSimpleName());
    }

    public static <K extends Kodeverk> boolean erGyldigKode(Class<K> clazz, String kode) {
        Assert.notNull(clazz, "Kodeverk class må ikke være null");

        if (kode == null) {
            return false;
        }

        for (K k: clazz.getEnumConstants()) {
            if (k.getKode().equals(kode)) {
                return true;
            }
        }

        return false;
    }
}
