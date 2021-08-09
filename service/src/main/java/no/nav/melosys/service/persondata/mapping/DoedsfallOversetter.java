package no.nav.melosys.service.persondata.mapping;

import java.util.Collection;
import java.util.Comparator;

import no.nav.melosys.domain.person.Doedsfall;
import no.nav.melosys.integrasjon.pdl.dto.HarMetadata;

public final class DoedsfallOversetter {
    private DoedsfallOversetter() {
        throw new IllegalStateException("Ikke ment å bli instantiert");
    }

    public static Doedsfall oversett(Collection<no.nav.melosys.integrasjon.pdl.dto.person.Doedsfall> doedsfall) {
        return doedsfall.stream()
            .max(Comparator.comparing(HarMetadata::hentDatoSistRegistrert))
            .map(d -> new Doedsfall(d.doedsdato()))
            .orElse(null);
    }
}
