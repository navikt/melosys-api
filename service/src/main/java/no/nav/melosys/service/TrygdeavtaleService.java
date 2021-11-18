package no.nav.melosys.service;

import java.util.*;
import java.util.stream.Collectors;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.InnvilgelsesResultat;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.domain.behandlingsgrunnlag.SoeknadTrygdeavtale;
import no.nav.melosys.domain.behandlingsgrunnlag.data.MedfolgendeFamilie;
import no.nav.melosys.domain.dokument.arbeidsforhold.ArbeidsforholdDokument;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Medlemskapstyper;
import no.nav.melosys.domain.kodeverk.Trygdedekninger;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_trygdeavtale_uk;
import no.nav.melosys.domain.person.familie.AvklarteMedfolgendeFamilie;
import no.nav.melosys.domain.person.familie.IkkeOmfattetFamilie;
import no.nav.melosys.domain.person.familie.OmfattetFamilie;
import no.nav.melosys.exception.TekniskException;
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
        AvklarteMedfolgendeFamilie lagreMedfolgendeFamilie = lagMedfolgendeFamilie(trygdeavtaleResultat);

        avklarteMedfolgendeFamilieService.lagreMedfolgendeFamilieSomAvklartefakta(behandlingId, lagreMedfolgendeFamilie);

        avklarteVirksomheterService.lagreVirksomheterSomAvklartefakta(trygdeavtaleResultat.virksomheter(), behandlingId);

        var behandlingsgrunnlag = behandlingsgrunnlagService.hentBehandlingsgrunnlag(behandlingId);
        SoeknadTrygdeavtale behandlingsgrunnlagdata = (SoeknadTrygdeavtale) behandlingsgrunnlag.getBehandlingsgrunnlagdata();

        var lovvalgsperiode = lagLovvalgsperiode(trygdeavtaleResultat, behandlingsgrunnlagdata);
        lovvalgsperiodeService.lagreLovvalgsperioder(behandlingId, List.of(lovvalgsperiode));
    }

    public AvklarteMedfolgendeFamilie lagMedfolgendeFamilie(TrygdeavtaleResultat trygdeavtaleResultat){
        return new AvklarteMedfolgendeFamilie(
            trygdeavtaleResultat.barn.stream()
                .filter(Familie::omfattet)
                .map(familieDto -> new OmfattetFamilie(familieDto.uuid())).collect(Collectors.toSet()),
            trygdeavtaleResultat.barn.stream()
                .filter(familie -> !familie.omfattet)
                .map(familieDto -> new IkkeOmfattetFamilie(familieDto.uuid(), familieDto.begrunnelseKode(), familieDto.begrunnelseFritekst())).collect(Collectors.toSet()));
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

    public record TrygdeavtaleResultat(
        List<String> virksomheter,
        String bestemmelse,
        List<Familie> barn,
        Familie ektefelle) {

        public static class Builder {
            private List<String> virksomheter;
            private String bestemmelse;
            private List<Familie> barn = new ArrayList<>();
            private Familie ektefelle;

            public Builder virksomheter(List<String> virksomheter) {
                this.virksomheter = virksomheter;
                return this;
            }

            public Builder bestemmelse(String bestemmelse) {
                this.bestemmelse = bestemmelse;
                return this;
            }

            public Builder barn(List<Familie> barn) {
                this.barn = barn;
                return this;
            }

            public Builder addBarn(String uuid, boolean omfattet, String begrunnelseKode, String begrunnelseFritekst) {
                this.barn.add(new Familie(uuid, omfattet, begrunnelseKode, begrunnelseFritekst));
                return this;
            }

            public Builder ektefelle(String uuid, boolean omfattet, String begrunnelseKode, String begrunnelseFritekst) {
                this.ektefelle = new Familie(uuid, omfattet, begrunnelseKode, begrunnelseFritekst);
                return this;
            }

            public TrygdeavtaleResultat build() {
                return new TrygdeavtaleResultat(
                    virksomheter,
                    bestemmelse,
                    barn,
                    ektefelle
                );
            }

        }
    }

    public record Familie(
        String uuid,
        boolean omfattet,
        String begrunnelseKode,
        String begrunnelseFritekst) {
    }

}
