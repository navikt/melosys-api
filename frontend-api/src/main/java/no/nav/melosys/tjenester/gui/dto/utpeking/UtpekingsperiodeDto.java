package no.nav.melosys.tjenester.gui.dto.utpeking;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import no.nav.melosys.domain.Utpekingsperiode;
import no.nav.melosys.domain.jpa.LovvalgBestemmelsekonverterer;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.tjenester.gui.dto.periode.PeriodeDto;

public record UtpekingsperiodeDto(@JsonUnwrapped(suffix = "Dato") PeriodeDto periode,
                                  String lovvalgsbestemmelse,
                                  String tilleggsbestemmelse,
                                  String lovvalgsland) {
    private static final LovvalgBestemmelsekonverterer konverterer = new LovvalgBestemmelsekonverterer();

    public static UtpekingsperiodeDto av(Utpekingsperiode utpekingsperiode) {
        return new UtpekingsperiodeDto(
            new PeriodeDto(utpekingsperiode.getFom(), utpekingsperiode.getTom()),
            utpekingsperiode.getBestemmelse().name(),
            utpekingsperiode.getTilleggsbestemmelse() != null ? utpekingsperiode.getTilleggsbestemmelse().name() : null,
            utpekingsperiode.getLovvalgsland().name()
        );
    }

    public final Utpekingsperiode tilDomene() {
        return new Utpekingsperiode(
            periode.getFom(),
            periode.getTom(),
            enumVerdiEllerNull(Landkoder.class, lovvalgsland),
            konverterer.convertToEntityAttribute(lovvalgsbestemmelse),
            konverterer.convertToEntityAttribute(tilleggsbestemmelse));
    }

    private static <E extends Enum<E>> E enumVerdiEllerNull(Class<E> enumKlasse, String nøkkel) {
        return nøkkel == null ? null : Enum.valueOf(enumKlasse, nøkkel);
    }
}
