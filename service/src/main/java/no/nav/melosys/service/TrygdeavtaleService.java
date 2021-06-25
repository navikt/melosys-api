package no.nav.melosys.service;

import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.registeropplysninger.RegisterOppslagService;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class TrygdeavtaleService {

    private final BehandlingService behandlingService;
    private final RegisterOppslagService registerOppslagService;

    public TrygdeavtaleService(BehandlingService behandlingService, RegisterOppslagService registerOppslagService) {
        this.behandlingService = behandlingService;
        this.registerOppslagService = registerOppslagService;
    }

    public Map<String, String> hentVirksomheter(long behandlingID) {
        var behandling = behandlingService.hentBehandling(behandlingID);
        var behandlingsgrunnlagData = behandling.getBehandlingsgrunnlag().getBehandlingsgrunnlagdata();
        var organisasjonDokumenter = behandling.hentOrganisasjonDokumenter();

        Map<String, String> orgIdOgNavn = new HashMap<>();

        orgIdOgNavn.putAll(behandling.hentArbeidsforholdDokument().hentOrgnumre().stream()
            .collect(Collectors.toMap(orgnr -> orgnr, orgnr -> finnNavnFraOrganisasjonsdokument(orgnr, organisasjonDokumenter))));
        orgIdOgNavn.putAll(behandlingsgrunnlagData.hentAlleOrganisasjonsnumre().stream()
            .collect(Collectors.toMap(orgnr -> orgnr, orgnr -> finnNavnFraOrganisasjonsdokument(orgnr, organisasjonDokumenter))));
        orgIdOgNavn.putAll(behandlingsgrunnlagData.hentUtenlandskeArbeidsgivereUuidOgNavn());

        return orgIdOgNavn;
    }

    private String finnNavnFraOrganisasjonsdokument(String orgnr, List<OrganisasjonDokument> organisasjonDokumenter) {
        return organisasjonDokumenter.stream()
            .filter(organisasjonDokument -> orgnr.equals(organisasjonDokument.getOrgnummer()))
            .findFirst().orElse(registerOppslagService.hentOrganisasjon(orgnr)).getNavn();
    }
}
