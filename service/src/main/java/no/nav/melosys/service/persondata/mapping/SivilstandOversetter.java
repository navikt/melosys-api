package no.nav.melosys.service.persondata.mapping;

import java.util.Collection;
import java.util.Comparator;

import no.nav.melosys.domain.person.Sivilstand;
import no.nav.melosys.domain.person.Sivilstandstype;
import no.nav.melosys.integrasjon.pdl.dto.HarMetadata;

public final class SivilstandOversetter {
    private SivilstandOversetter() {
    }

    public static Sivilstand oversettForRelatertVedSivilstand(
        Collection<no.nav.melosys.integrasjon.pdl.dto.person.Sivilstand> sivilstandListe) {
        return sivilstandListe.stream()
            .max(Comparator.comparing(HarMetadata::hentDatoSistRegistrert))
            .map(SivilstandOversetter::oversett)
            .orElse(null);
    }

    static Sivilstand oversett(no.nav.melosys.integrasjon.pdl.dto.person.Sivilstand sivilstand) {
        return new Sivilstand(
            Sivilstandstype.valueOf(sivilstand.type().name()),
            null,
            sivilstand.relatertVedSivilstand(),
            sivilstand.gyldigFraOgMed(),
            sivilstand.bekreftelsesdato(),
            sivilstand.metadata().master(),
            sivilstand.hentKilde(),
            sivilstand.metadata().historisk()
        );
    }
}
