package no.nav.melosys.service.trygdeavtale;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.domain.behandlingsgrunnlag.SoeknadTrygdeavtale;
import no.nav.melosys.domain.behandlingsgrunnlag.data.MedfolgendeFamilie;
import no.nav.melosys.domain.dokument.arbeidsforhold.ArbeidsforholdDokument;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.kodeverk.InnvilgelsesResultat;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Medlemskapstyper;
import no.nav.melosys.domain.kodeverk.Trygdedekninger;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_trygdeavtale_uk;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.avklartefakta.AvklarteMedfolgendeFamilieService;
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService;
import no.nav.melosys.service.behandlingsgrunnlag.BehandlingsgrunnlagService;
import no.nav.melosys.service.registeropplysninger.RegisterOppslagService;
import org.springframework.stereotype.Service;

@Service
public class TrygdeavtaleService {

    private final RegisterOppslagService registerOppslagService;
    private final BehandlingsgrunnlagService behandlingsgrunnlagService;
    private final AvklarteMedfolgendeFamilieService avklarteMedfolgendeFamilieService;
    private final AvklarteVirksomheterService avklarteVirksomheterService;
    private final LovvalgsperiodeService lovvalgsperiodeService;

    public TrygdeavtaleService(RegisterOppslagService registerOppslagService,
                               BehandlingsgrunnlagService behandlingsgrunnlagService,
                               AvklarteMedfolgendeFamilieService avklarteMedfolgendeFamilieService,
                               AvklarteVirksomheterService avklarteVirksomheterService,
                               LovvalgsperiodeService lovvalgsperiodeService) {
        this.registerOppslagService = registerOppslagService;
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
            .findFirst().orElse(registerOppslagService.hentOrganisasjon(orgnr)).getNavn();
    }

    public List<MedfolgendeFamilie> hentFamiliemedlemmer(Behandling behandling) {
        return behandling.getBehandlingsgrunnlag().getBehandlingsgrunnlagdata().personOpplysninger.medfolgendeFamilie;
    }

    public void overførResultat(long behandlingId, TrygdeavtaleResultat trygdeavtaleResultat) {
        avklarteMedfolgendeFamilieService.lagreMedfolgendeFamilieSomAvklartefakta(behandlingId, trygdeavtaleResultat.familie());
        avklarteVirksomheterService.lagreVirksomheterSomAvklartefakta(trygdeavtaleResultat.virksomheter(), behandlingId);

        SoeknadTrygdeavtale behandlingsgrunnlagdata =
            (SoeknadTrygdeavtale) behandlingsgrunnlagService.hentBehandlingsgrunnlag(behandlingId).getBehandlingsgrunnlagdata();

        lovvalgsperiodeService.lagreLovvalgsperioder(behandlingId, List.of(lagLovvalgsperiode(trygdeavtaleResultat, behandlingsgrunnlagdata)));
    }

    private Lovvalgsperiode lagLovvalgsperiode(TrygdeavtaleResultat trygdeavtaleResultat, SoeknadTrygdeavtale behandlingsgrunnlagdata) {
        var lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setFom(behandlingsgrunnlagdata.periode.getFom());
        lovvalgsperiode.setTom(behandlingsgrunnlagdata.periode.getTom());

        lovvalgsperiode.setInnvilgelsesresultat(InnvilgelsesResultat.INNVILGET);
        if (behandlingsgrunnlagdata.soeknadsland.landkoder.size() != 1) {
            throw new TekniskException("Forventet ett land i behandlingsgrunnlagdata soeknadsland.landkoder, men fant: "
                + behandlingsgrunnlagdata.soeknadsland.landkoder);
        }
        Landkoder lovvalgsland = Landkoder.valueOf(behandlingsgrunnlagdata.soeknadsland.landkoder.get(0));
        lovvalgsperiode.setLovvalgsland(lovvalgsland);
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_trygdeavtale_uk.valueOf(trygdeavtaleResultat.bestemmelse()));
        lovvalgsperiode.setMedlemskapstype(Medlemskapstyper.PLIKTIG);
        lovvalgsperiode.setDekning(Trygdedekninger.FULL_DEKNING_FTRL); // Skal bli renamet til FULL_DEKNING av fag
        return lovvalgsperiode;
    }
}
