package no.nav.melosys.tjenester.gui.graphql.mapping;

import java.time.LocalDate;

import no.nav.melosys.domain.person.Foedsel;
import no.nav.melosys.domain.person.Sivilstandstype;
import no.nav.melosys.domain.person.familie.Familiemedlem;
import no.nav.melosys.tjenester.gui.graphql.dto.FamiliemedlemDto;

import static java.time.temporal.ChronoUnit.YEARS;

public final class FamilemedlemTilDtoKonverter {
    private FamilemedlemTilDtoKonverter() {
    }

    public static FamiliemedlemDto tilDto(Familiemedlem familiemedlem) {
        return new FamiliemedlemDto(
            familiemedlem.navn().tilSammensattNavn(),
            familiemedlem.folkeregisteridentifikator().identifikasjonsnummer(),
            familiemedlem.familierelasjon(),
            familiemedlem.erBarn() ? Math.toIntExact(beregnAlder(familiemedlem.fødsel())) : null,
            familiemedlem.erBarn() ? familiemedlem.foreldreansvarstype() : null,
            familiemedlem.erBarn() ? hentFolkeregisteridentAnnenForelder(familiemedlem) : null,
            familiemedlem.erRelatertVedSivilstand() ? hentSivilstandstype(familiemedlem) : null,
            familiemedlem.erRelatertVedSivilstand() ? hentSivilstandGyldighetsperiodeFom(familiemedlem) : null
        );
    }

    private static long beregnAlder(Foedsel fødsel) {
        return fødsel.fødselsdato() != null ? YEARS.between(fødsel.fødselsdato(), LocalDate.now()) :
            LocalDate.now().getYear() - fødsel.fødselsår();
    }

    private static String hentFolkeregisteridentAnnenForelder(Familiemedlem familiemedlem) {
        return familiemedlem.folkeregisteridentAnnenForelder() != null ?
            familiemedlem.folkeregisteridentAnnenForelder().identifikasjonsnummer() : null;
    }

    private static Sivilstandstype hentSivilstandstype(Familiemedlem familiemedlem) {
        return familiemedlem.sivilstand() != null ? familiemedlem.sivilstand().type() : null;
    }


    private static LocalDate hentSivilstandGyldighetsperiodeFom(Familiemedlem familiemedlem) {
        return familiemedlem.sivilstand() != null ? familiemedlem.sivilstand().gyldigFraOgMed() : null;
    }
}
