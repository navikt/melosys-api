package no.nav.melosys.tjenester.gui.dto;

import java.util.Set;

import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;

public class BehandlingsgrunnlagTilleggsData {

    private final Set<OrganisasjonDokument> organisasjoner;

    public BehandlingsgrunnlagTilleggsData(Set<OrganisasjonDokument> organisasjoner) {
        this.organisasjoner = organisasjoner;
    }

    public Set<OrganisasjonDokument> getOrganisasjoner() {
        return organisasjoner;
    }
}
