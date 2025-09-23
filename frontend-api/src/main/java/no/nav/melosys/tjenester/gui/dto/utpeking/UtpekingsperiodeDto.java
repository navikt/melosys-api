package no.nav.melosys.tjenester.gui.dto.utpeking;

import java.time.LocalDate;

import no.nav.melosys.domain.Utpekingsperiode;
import no.nav.melosys.domain.jpa.LovvalgBestemmelsekonverterer;
import no.nav.melosys.domain.kodeverk.Land_iso2;

public record UtpekingsperiodeDto(LocalDate fomDato,
                                  LocalDate tomDato,
                                  String lovvalgsbestemmelse,
                                  String tilleggsbestemmelse,
                                  String lovvalgsland) {
    private static final LovvalgBestemmelsekonverterer konverterer = new LovvalgBestemmelsekonverterer();

    public static UtpekingsperiodeDto av(Utpekingsperiode utpekingsperiode) {
        return new UtpekingsperiodeDto(
            utpekingsperiode.getFom(),
            utpekingsperiode.getTom(),
            utpekingsperiode.getBestemmelse() != null ? utpekingsperiode.getBestemmelse().name() : null,
            utpekingsperiode.getTilleggsbestemmelse() != null ? utpekingsperiode.getTilleggsbestemmelse().name() : null,
            utpekingsperiode.getLovvalgsland() != null ? utpekingsperiode.getLovvalgsland().name() : null
        );
    }

    public final Utpekingsperiode tilDomene() {
        return new Utpekingsperiode(
            fomDato,
            tomDato,
            enumVerdiEllerNull(Land_iso2.class, lovvalgsland),
            konverterer.convertToEntityAttribute(lovvalgsbestemmelse),
            konverterer.convertToEntityAttribute(tilleggsbestemmelse));
    }

    private static <E extends Enum<E>> E enumVerdiEllerNull(Class<E> enumKlasse, String nøkkel) {
        return nøkkel == null ? null : Enum.valueOf(enumKlasse, nøkkel);
    }
}
