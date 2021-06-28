package no.nav.melosys.service.persondata.mapping;

import java.util.Collection;
import java.util.Comparator;

import no.nav.melosys.domain.person.Foedsel;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.integrasjon.pdl.dto.HarMetadata;

public final class FoedselOversetter {
    private FoedselOversetter() {
        throw new IllegalStateException("Ikke ment å bli instantiert");
    }

    public static Foedsel oversett(Collection<no.nav.melosys.integrasjon.pdl.dto.person.Foedsel> fødselListe) {
        final var fødsel = fødselListe.stream().max(Comparator.comparing(HarMetadata::hentDatoSistRegistrert))
            .orElseThrow(() -> new FunksjonellException("Fødsel forventes tilgengelig på alle personer."));
        return new Foedsel(
            fødsel.foedselsdato(),
            fødsel.foedselsaar(),
            fødsel.foedeland(),
            fødsel.foedested()
        );
    }
}
