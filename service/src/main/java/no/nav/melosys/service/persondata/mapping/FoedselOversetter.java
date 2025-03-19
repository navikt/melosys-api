package no.nav.melosys.service.persondata.mapping;

import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;

import no.nav.melosys.domain.person.Foedsel;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.integrasjon.pdl.dto.HarMetadata;
import no.nav.melosys.integrasjon.pdl.dto.person.Foedested;
import no.nav.melosys.integrasjon.pdl.dto.person.Foedselsdato;

public final class FoedselOversetter {
    private FoedselOversetter() {
        throw new IllegalStateException("Ikke ment å bli instantiert");
    }

    public static Foedsel oversett(Collection<Foedested> fodestedListe,
                                   Collection<Foedselsdato> foedselsdatoListe){

        final var fødselsdato = foedselsdatoListe.stream().max(Comparator.comparing(HarMetadata::hentDatoSistRegistrert))
            .orElseThrow(() -> new FunksjonellException("Fødselsdato forventes tilgengelig på alle personer."));

        final var fødested = Optional.ofNullable(fodestedListe)
            .stream()
            .flatMap(Collection::stream)
            .findFirst()
            .orElse(null);

        return new Foedsel(
            fødselsdato.foedselsdato(),
            fødselsdato.foedselsaar(),
            fødested != null ? fødested.foedested() : null,
            fødested != null ? fødested.foedeland() : null
        );
    }
}
