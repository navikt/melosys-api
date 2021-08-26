package no.nav.melosys.service.persondata.mapping;

import java.util.Collection;
import java.util.Comparator;

import no.nav.melosys.integrasjon.pdl.dto.HarMetadata;
import no.nav.melosys.integrasjon.pdl.dto.person.Foreldreansvar;

public final class ForeldreansvarOversetter {
    private ForeldreansvarOversetter() {
    }

    public static String oversett(Collection<Foreldreansvar> foreldreansvar) {
        return foreldreansvar.stream().max(Comparator.comparing(HarMetadata::hentDatoSistRegistrert))
            .map(Foreldreansvar::ansvar)
            .orElse(null);
    }
}
