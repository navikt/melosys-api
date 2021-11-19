package no.nav.melosys.tjenester.gui.graphql.dto;

import java.util.List;

import no.nav.melosys.domain.person.KjoennType;

public record PersonopplysningerDto(
    List<BostedsadresseDto> bostedsadresser,
    String folkeregisteridentifikator,
    List<FolkeregisterpersonstatusDto> folkeregisterpersonstatuser,
    KjoennType kjoenn,
    List<KontaktadresseDto> kontaktadresser,
    NavnDto navn,
    List<OppholdsadresseDto> oppholdsadresser,
    List<SivilstandDto> sivilstand,
    List<StatsborgerskapDto> statsborgerskap) {
}
