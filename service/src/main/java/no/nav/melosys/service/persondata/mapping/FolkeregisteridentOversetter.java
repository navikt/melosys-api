package no.nav.melosys.service.persondata.mapping;

import java.util.Collection;
import java.util.Comparator;

import no.nav.melosys.domain.person.Folkeregisteridentifikator;
import no.nav.melosys.integrasjon.pdl.dto.HarMetadata;

public final class FolkeregisteridentOversetter {
    private FolkeregisteridentOversetter() {
        throw new IllegalStateException("Ikke ment å bli instantiert");
    }

    public static Folkeregisteridentifikator oversett(
        Collection<no.nav.melosys.integrasjon.pdl.dto.person.Folkeregisteridentifikator> folkeregisteridentListe) {
        return folkeregisteridentListe.stream()
            .max(Comparator.comparing(HarMetadata::hentDatoSistRegistrert))
            .map(FolkeregisteridentOversetter::oversett)
            .orElse(null);
    }

    private static Folkeregisteridentifikator oversett(
        no.nav.melosys.integrasjon.pdl.dto.person.Folkeregisteridentifikator folkeregisterident) {
        return new Folkeregisteridentifikator(
            folkeregisterident.identifikasjonsnummer()
        );
    }
}
