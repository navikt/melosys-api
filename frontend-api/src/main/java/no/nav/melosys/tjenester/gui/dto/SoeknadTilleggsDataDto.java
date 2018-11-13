package no.nav.melosys.tjenester.gui.dto;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.dokument.person.PersonDokument;

public class SoeknadTilleggsDataDto {

    public Set<OrganisasjonDokument> organisasjoner = new HashSet<>();
    public Set<PersonDto> personer = new HashSet<>();

    public SoeknadTilleggsDataDto() {}

    public SoeknadTilleggsDataDto(Set<OrganisasjonDokument> organisasjoner,
                                  Set<PersonDokument> personer) {
        this.organisasjoner = organisasjoner;

        this.personer = personer.stream()
                .map(PersonDto::new)
                .collect(Collectors.toSet());
    }
}
