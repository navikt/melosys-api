package no.nav.melosys.tjenester.gui.dto;

import java.util.Set;
import java.util.stream.Collectors;

import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.dokument.person.PersonDokument;

public class SoeknadTilleggDataDto {

    public Set<OrganisasjonDokument> organisasjoner;
    public Set<PersonDto> personer;

    public SoeknadTilleggDataDto(Set<OrganisasjonDokument> organisasjoner,
                                 Set<PersonDokument> personer) {
        this.organisasjoner = organisasjoner;

        this.personer = personer.stream()
                .map(PersonDto::new)
                .collect(Collectors.toSet());
    }
}
