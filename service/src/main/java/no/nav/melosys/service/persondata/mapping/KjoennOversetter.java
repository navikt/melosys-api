package no.nav.melosys.service.persondata.mapping;

import java.util.Collection;
import java.util.Comparator;

import no.nav.melosys.domain.person.KjoennType;
import no.nav.melosys.integrasjon.pdl.dto.HarMetadata;

public final class KjoennOversetter {
    private KjoennOversetter() {
        throw new IllegalStateException("Ikke ment å bli instantiert");
    }

    public static KjoennType oversett(Collection<no.nav.melosys.integrasjon.pdl.dto.person.Kjoenn> kjønnListe) {
        return kjønnListe.stream()
            .max(Comparator.comparing(HarMetadata::hentDatoSistRegistrert))
            .map(k -> KjoennType.valueOf(k.kjoenn().name()))
            .orElse(KjoennType.UKJENT);
    }
}
