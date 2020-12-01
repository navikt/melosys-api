package no.nav.melosys.domain.util;

import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import no.nav.melosys.domain.kodeverk.Kodeverk;
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

    public static Collection<String> tilStringCollection(Kodeverk... kodeverkVerdier) {
        return Stream.of(kodeverkVerdier).map(Kodeverk::getKode).collect(Collectors.toSet());
    }
}
