package no.nav.melosys.tjenester.gui.dto;

import java.util.List;
import java.util.stream.Collectors;

import no.nav.melosys.domain.dokument.person.Familiemedlem;
import no.nav.melosys.domain.dokument.person.Familierelasjon;

public class FamiliemedlemDto {
    public String fnr;
    public String sammensattNavn;
    public Familierelasjon relasjonstype;

    public FamiliemedlemDto(Familiemedlem familiemedlem) {
        fnr = familiemedlem.fnr;
        sammensattNavn = familiemedlem.navn;
        relasjonstype = familiemedlem.familierelasjon;
    }

    public static List<FamiliemedlemDto> avFamiliemedlemmer(List<Familiemedlem> familiemedlemmer) {
        return familiemedlemmer.stream()
            .map(FamiliemedlemDto::new)
            .collect(Collectors.toList());
    }
}
