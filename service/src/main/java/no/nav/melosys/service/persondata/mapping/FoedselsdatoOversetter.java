package no.nav.melosys.service.persondata.mapping;

import java.util.Collection;
import java.util.Comparator;

import no.nav.melosys.domain.person.Foedselsdato;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.integrasjon.pdl.dto.HarMetadata;

public final class FoedselsdatoOversetter {
    private FoedselsdatoOversetter() {
        throw new IllegalStateException("Ikke ment å bli instantiert");
    }

    public static Foedselsdato oversett(Collection<no.nav.melosys.integrasjon.pdl.dto.person.Foedselsdato> fødselsdatoListe) {
        final var fødselsdato = fødselsdatoListe.stream().max(Comparator.comparing(HarMetadata::hentDatoSistRegistrert))
            .orElseThrow(() -> new FunksjonellException("Fødsel forventes tilgengelig på alle personer."));
        return new Foedselsdato(
            fødselsdato.foedselsdato(),
            fødselsdato.foedselsaar()
        );
    }
}
