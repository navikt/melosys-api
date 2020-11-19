package no.nav.melosys.tjenester.gui.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import no.nav.melosys.domain.dokument.person.Familiemedlem;
import no.nav.melosys.domain.dokument.person.Familierelasjon;
import no.nav.melosys.domain.dokument.person.Sivilstand;

import static java.time.temporal.ChronoUnit.YEARS;

public class FamiliemedlemDto {
    public String fnr;
    public String sammensattNavn;
    public Familierelasjon relasjonstype;
    public Long alder;
    public boolean borMedBruker;
    public Sivilstand sivilstand;
    public LocalDate sivilstandGyldighetsperiodeFom;
    public String fnrAnnenForelder;

    public FamiliemedlemDto(Familiemedlem familiemedlem) {
        fnr = familiemedlem.fnr;
        sammensattNavn = familiemedlem.navn;
        relasjonstype = familiemedlem.familierelasjon;
        alder = familiemedlem.fødselsdato == null ? null
            : YEARS.between(familiemedlem.fødselsdato, LocalDate.now());
        borMedBruker = familiemedlem.borMedBruker;
        sivilstand = familiemedlem.sivilstand;
        sivilstandGyldighetsperiodeFom = familiemedlem.sivilstandGyldighetsperiodeFom;
        fnrAnnenForelder = familiemedlem.fnrAnnenForelder;
    }

    static List<FamiliemedlemDto> avFamiliemedlemmer(List<Familiemedlem> familiemedlemmer) {
        return familiemedlemmer.stream()
            .map(FamiliemedlemDto::new)
            .collect(Collectors.toList());
    }
}
