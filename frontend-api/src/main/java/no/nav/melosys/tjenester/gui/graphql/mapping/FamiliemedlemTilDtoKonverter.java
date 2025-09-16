package no.nav.melosys.tjenester.gui.graphql.mapping;

import java.time.LocalDate;

import no.nav.melosys.domain.person.Foedsel;
import no.nav.melosys.domain.person.familie.Familiemedlem;
import no.nav.melosys.tjenester.gui.graphql.dto.FamiliemedlemDto;

import static java.time.temporal.ChronoUnit.YEARS;

public final class FamiliemedlemTilDtoKonverter {
    private FamiliemedlemTilDtoKonverter() {
    }

    public static FamiliemedlemDto tilDto(Familiemedlem familiemedlem) {
        return new FamiliemedlemDto(
            familiemedlem.navn().tilSammensattNavn(),
            familiemedlem.folkeregisteridentifikator().identifikasjonsnummer(),
            familiemedlem.familierelasjon(),
            familiemedlem.erBarn() ? Math.toIntExact(beregnAlder(familiemedlem.fødsel())) : null,
            familiemedlem.erBarn() ? familiemedlem.foreldreansvarstype() : null,
            familiemedlem.erBarn() ? hentFolkeregisteridentAnnenForelder(familiemedlem) : null,
            familiemedlem.erRelatertVedSivilstand() ? familiemedlem.sivilstand() : null
        );
    }

    private static long beregnAlder(Foedsel fødsel) {
        return fødsel.getFødselsdato() != null ? YEARS.between(fødsel.getFødselsdato(), LocalDate.now()) :
            LocalDate.now().getYear() - fødsel.getFødselsår();
    }

    private static String hentFolkeregisteridentAnnenForelder(Familiemedlem familiemedlem) {
        return familiemedlem.folkeregisteridentAnnenForelder() != null ?
            familiemedlem.folkeregisteridentAnnenForelder().identifikasjonsnummer() : null;
    }
}
