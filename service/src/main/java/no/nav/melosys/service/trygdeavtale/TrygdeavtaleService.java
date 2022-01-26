package no.nav.melosys.service.trygdeavtale;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.domain.behandlingsgrunnlag.data.MedfolgendeFamilie;
import no.nav.melosys.domain.dokument.arbeidsforhold.ArbeidsforholdDokument;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_trygdeavtale_uk;
import no.nav.melosys.domain.person.familie.AvklarteMedfolgendeFamilie;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.ereg.EregFasade;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.avklartefakta.AvklarteMedfolgendeFamilieService;
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import org.springframework.stereotype.Service;

import static no.nav.melosys.domain.kodeverk.InnvilgelsesResultat.INNVILGET;
import static no.nav.melosys.domain.kodeverk.Medlemskapstyper.PLIKTIG;
import static no.nav.melosys.domain.kodeverk.Trygdedekninger.FULL_DEKNING_FTRL;

@Service
public class TrygdeavtaleService {

    private final EregFasade eregFasade;
    private final AvklarteMedfolgendeFamilieService avklarteMedfolgendeFamilieService;
    private final AvklarteVirksomheterService avklarteVirksomheterService;
    private final LovvalgsperiodeService lovvalgsperiodeService;
    private final AvklartefaktaService avklartefaktaService;

    public TrygdeavtaleService(EregFasade eregFasade,
                               AvklarteMedfolgendeFamilieService avklarteMedfolgendeFamilieService,
                               AvklarteVirksomheterService avklarteVirksomheterService,
                               LovvalgsperiodeService lovvalgsperiodeService, AvklartefaktaService avklartefaktaService) {
        this.eregFasade = eregFasade;
        this.avklarteMedfolgendeFamilieService = avklarteMedfolgendeFamilieService;
        this.avklarteVirksomheterService = avklarteVirksomheterService;
        this.lovvalgsperiodeService = lovvalgsperiodeService;
        this.avklartefaktaService = avklartefaktaService;
    }

    public Map<String, String> hentVirksomheter(Behandling behandling) {
        var behandlingsgrunnlagData = behandling.getBehandlingsgrunnlag().getBehandlingsgrunnlagdata();
        var organisasjonDokumenter = behandling.hentOrganisasjonDokumenter();

        Map<String, String> orgIdOgNavn = new HashMap<>();

        orgIdOgNavn.putAll(
            ((ArbeidsforholdDokument) behandling.finnDokument(SaksopplysningType.ARBFORH).orElse(new ArbeidsforholdDokument()))
                .hentArbeidsgiverIDer().stream()
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

    public TrygdeavtaleResultat hentResultat(long behandlingId) {
        var familie = hentAvklarteMedfolgendeFamilie(behandlingId);
        var virksomhet = hentVirksomheter(behandlingId);
        var lovvalgsperiode = hentLovvalgsperiode(behandlingId);

        return new TrygdeavtaleResultat.Builder()
            .familie(familie)
            .virksomhet(virksomhet)
            .lovvalgsperiode(lovvalgsperiode)
            .build();
    }

    private String hentVirksomheter(long behandlingId) {
        var virksomheter = avklartefaktaService.hentAvklarteOrgnrOgUuid(behandlingId);
        if (virksomheter.size() > 1) {
            throw new TekniskException("Forventer kun 1 virksomhet for " + behandlingId);
        }
        return virksomheter.stream().findFirst().orElse(null);
    }

    private Lovvalgsperiode hentLovvalgsperiode(long behandlingId) {
        var lovvalgsperioder = lovvalgsperiodeService.hentLovvalgsperioder(behandlingId);
        if (lovvalgsperioder.size() > 1) {
            throw new TekniskException("Forventer kun 1 lovvalgsperiode for " + behandlingId);
        }
        return lovvalgsperioder.stream().findFirst().orElse(null);
    }

    private AvklarteMedfolgendeFamilie hentAvklarteMedfolgendeFamilie(long behandlingId) {
        var avklarteMedfølgendeBarn = avklarteMedfolgendeFamilieService.hentAvklarteMedfølgendeBarn(behandlingId);
        var avklarteMedfølgendeEktefelle = avklarteMedfolgendeFamilieService.hentAvklartMedfølgendeEktefelle(behandlingId);

        var omfattetFamilie = Stream.concat(avklarteMedfølgendeBarn.getFamilieOmfattetAvNorskTrygd().stream(),
            avklarteMedfølgendeEktefelle.getFamilieOmfattetAvNorskTrygd().stream()).collect(Collectors.toSet());
        var ikkeOmfattetFamilie = Stream.concat(avklarteMedfølgendeBarn.getFamilieIkkeOmfattetAvNorskTrygd().stream(),
            avklarteMedfølgendeEktefelle.getFamilieIkkeOmfattetAvNorskTrygd().stream()).collect(Collectors.toSet());

        return new AvklarteMedfolgendeFamilie(omfattetFamilie, ikkeOmfattetFamilie);
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
