package no.nav.melosys.tjenester.gui.dto;

import java.util.Set;

import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.dokument.person.PersonDokument;

public class SoeknadTilleggDataDto {

    public Set<OrganisasjonDokument> organisasjoner;
    public Set<PersonDokument> personer;

    public SoeknadTilleggDataDto(Set<OrganisasjonDokument> organisasjoner, Set<PersonDokument> personer) {
        this.organisasjoner = organisasjoner;
        this.personer = personer;
    }
}
