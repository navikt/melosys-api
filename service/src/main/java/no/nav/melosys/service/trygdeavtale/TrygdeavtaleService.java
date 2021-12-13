package no.nav.melosys.service.trygdeavtale;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.domain.behandlingsgrunnlag.data.MedfolgendeFamilie;
import no.nav.melosys.domain.dokument.arbeidsforhold.ArbeidsforholdDokument;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_trygdeavtale_uk;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.integrasjon.ereg.EregFasade;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.avklartefakta.AvklarteMedfolgendeFamilieService;
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService;
import no.nav.melosys.service.behandlingsgrunnlag.BehandlingsgrunnlagService;
import org.springframework.stereotype.Service;

import static no.nav.melosys.domain.kodeverk.InnvilgelsesResultat.INNVILGET;
import static no.nav.melosys.domain.kodeverk.Medlemskapstyper.PLIKTIG;
import static no.nav.melosys.domain.kodeverk.Trygdedekninger.FULL_DEKNING_FTRL;

@Service
public class TrygdeavtaleService {

    private final EregFasade eregFasade;
    private final BehandlingsgrunnlagService behandlingsgrunnlagService;
    private final AvklarteMedfolgendeFamilieService avklarteMedfolgendeFamilieService;
    private final AvklarteVirksomheterService avklarteVirksomheterService;
    private final LovvalgsperiodeService lovvalgsperiodeService;

    public TrygdeavtaleService(EregFasade eregFasade,
                               BehandlingsgrunnlagService behandlingsgrunnlagService,
                               AvklarteMedfolgendeFamilieService avklarteMedfolgendeFamilieService,
                               AvklarteVirksomheterService avklarteVirksomheterService,
                               LovvalgsperiodeService lovvalgsperiodeService) {
        this.eregFasade = eregFasade;
        this.behandlingsgrunnlagService = behandlingsgrunnlagService;
        this.avklarteMedfolgendeFamilieService = avklarteMedfolgendeFamilieService;
        this.avklarteVirksomheterService = avklarteVirksomheterService;
        this.lovvalgsperiodeService = lovvalgsperiodeService;
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
            .map(OrganisasjonDokument::getNavn)
            .findFirst().orElse(eregFasade.hentOrganisasjonNavn(orgnr));
    }

    public List<MedfolgendeFamilie> hentFamiliemedlemmer(Behandling behandling) {
        return behandling.getBehandlingsgrunnlag().getBehandlingsgrunnlagdata().personOpplysninger.medfolgendeFamilie;
    }

    public void overførResultat(long behandlingId, TrygdeavtaleResultat trygdeavtaleResultat) {
        avklarteMedfolgendeFamilieService.lagreMedfolgendeFamilieSomAvklartefakta(behandlingId, trygdeavtaleResultat.familie());
        avklarteVirksomheterService.lagreVirksomheterSomAvklartefakta(behandlingId, List.of(trygdeavtaleResultat.virksomhet()));
        lovvalgsperiodeService.lagreLovvalgsperioder(behandlingId, List.of(lagLovvalgsperiode(trygdeavtaleResultat)));
    }

    private Lovvalgsperiode lagLovvalgsperiode(TrygdeavtaleResultat trygdeavtaleResultat) {
        var lovvalgsperiode = new Lovvalgsperiode();

        lovvalgsperiode.setFom(trygdeavtaleResultat.lovvalgsperiodeFom());
        lovvalgsperiode.setTom(trygdeavtaleResultat.lovvalgsperiodeTom());
        lovvalgsperiode.setMedlemskapstype(PLIKTIG);
        lovvalgsperiode.setDekning(FULL_DEKNING_FTRL);
        lovvalgsperiode.setInnvilgelsesresultat(INNVILGET);
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_trygdeavtale_uk.valueOf(trygdeavtaleResultat.bestemmelse()));
        lovvalgsperiode.setLovvalgsland(Landkoder.NO);

        return lovvalgsperiode;
    }
}
