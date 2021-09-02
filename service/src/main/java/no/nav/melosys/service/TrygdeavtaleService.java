package no.nav.melosys.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.domain.behandlingsgrunnlag.data.MedfolgendeFamilie;
import no.nav.melosys.domain.dokument.arbeidsforhold.ArbeidsforholdDokument;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.service.registeropplysninger.RegisterOppslagService;
import org.springframework.stereotype.Service;

@Service
public class TrygdeavtaleService {

    private final RegisterOppslagService registerOppslagService;

    public TrygdeavtaleService(RegisterOppslagService registerOppslagService) {
        this.registerOppslagService = registerOppslagService;
    }

    public Map<String, String> hentVirksomheter(Behandling behandling) {
        var behandlingsgrunnlagData = behandling.getBehandlingsgrunnlag().getBehandlingsgrunnlagdata();
        var organisasjonDokumenter = behandling.hentOrganisasjonDokumenter();

        Map<String, String> orgIdOgNavn = new HashMap<>();

        orgIdOgNavn.putAll(
            ((ArbeidsforholdDokument) behandling.finnDokument(SaksopplysningType.ARBFORH).orElse(new ArbeidsforholdDokument()))
                .hentOrgnumre().stream()
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

    public List<MedfolgendeFamilie> hentFamiliemedlemmer(Behandling behandling) {
        return behandling.getBehandlingsgrunnlag().getBehandlingsgrunnlagdata().personOpplysninger.medfolgendeFamilie;
    }
}
