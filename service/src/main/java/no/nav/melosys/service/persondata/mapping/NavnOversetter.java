package no.nav.melosys.service.persondata.mapping;

import java.util.Collection;
import java.util.Comparator;

import no.nav.melosys.domain.person.Navn;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.integrasjon.pdl.dto.HarMetadata;

public final class NavnOversetter {
    public static final String UKJENT = "UKJENT";

    private NavnOversetter() {
        throw new IllegalStateException("Ikke ment å bli instantiert");
    }

    public static Navn oversett(Collection<no.nav.melosys.integrasjon.pdl.dto.person.Navn> navnListe) {
        final var navn = navnListe.stream()
            .filter(HarMetadata::erIkkeHistorisk)
            .max(Comparator.comparing(HarMetadata::hentDatoSistRegistrert))
            .orElseThrow(() -> new FunksjonellException("Navn forventes tilgengelig på alle personer."));
        return new Navn(navn.fornavn(), navn.mellomnavn(), navn.etternavn());
    }

    public static String tilSammensattNavn(no.nav.melosys.integrasjon.pdl.dto.person.Navn navn) {
        return new Navn(navn.fornavn(), navn.mellomnavn(), navn.etternavn()).tilSammensattNavn();
    }
}
