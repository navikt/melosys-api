package no.nav.melosys.service.dokument;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.avgift.AvgiftsgrunnlagInfoNorge;
import no.nav.melosys.domain.avgift.AvgiftsgrunnlagInfoUtland;
import no.nav.melosys.domain.avgift.Trygdeavgiftsgrunnlag;
import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet;
import no.nav.melosys.domain.behandlingsgrunnlag.data.MedfolgendeFamilie;
import no.nav.melosys.domain.brev.InnvilgelseBrevbestilling;
import no.nav.melosys.domain.folketrygden.FastsattTrygdeavgift;
import no.nav.melosys.domain.kodeverk.Avtaleland;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Representerer;
import no.nav.melosys.domain.person.familie.IkkeOmfattetFamilie;
import no.nav.melosys.domain.person.familie.OmfattetFamilie;
import no.nav.melosys.integrasjon.dokgen.dto.InnvilgelseFtrl;
import no.nav.melosys.integrasjon.dokgen.dto.innvilgelseftrl.*;
import no.nav.melosys.integrasjon.ereg.EregFasade;
import no.nav.melosys.service.LandvelgerService;
import no.nav.melosys.service.avgift.TrygdeavgiftsgrunnlagService;
import no.nav.melosys.service.avklartefakta.AvklarteMedfolgendeFamilieService;
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.persondata.PersondataFasade;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.kodeverk.Vilkaar.FTRL_2_8_NÆR_TILKNYTNING_NORGE;

@Component
public class InnvilgelseFtrlMapper {

    private final PersondataFasade persondataFasade;
    private final TrygdeavgiftsgrunnlagService trygdeavgiftsgrunnlagService;
    private final BehandlingsresultatService behandlingsresultatService;
    private final AvklarteVirksomheterService avklarteVirksomheterService;
    private final AvklarteMedfolgendeFamilieService avklarteMedfolgendeFamilieService;
    private final LandvelgerService landvelgerService;
    private final EregFasade eregFasade;

    public InnvilgelseFtrlMapper(PersondataFasade persondataFasade,
                                 TrygdeavgiftsgrunnlagService trygdeavgiftsgrunnlagService,
                                 BehandlingsresultatService behandlingsresultatService,
                                 AvklarteVirksomheterService avklarteVirksomheterService,
                                 AvklarteMedfolgendeFamilieService avklarteMedfolgendeFamilieService,
                                 LandvelgerService landvelgerService,
                                 @Qualifier("system") EregFasade eregFasade) {
        this.persondataFasade = persondataFasade;
        this.trygdeavgiftsgrunnlagService = trygdeavgiftsgrunnlagService;
        this.behandlingsresultatService = behandlingsresultatService;
        this.avklarteVirksomheterService = avklarteVirksomheterService;
        this.avklarteMedfolgendeFamilieService = avklarteMedfolgendeFamilieService;
        this.landvelgerService = landvelgerService;
        this.eregFasade = eregFasade;
    }

    public InnvilgelseFtrl map(InnvilgelseBrevbestilling brevbestilling) {
        long behandlingId = brevbestilling.getBehandlingId();
        var behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingId);
        var medlemAvFolketrygden = behandlingsresultat.getMedlemAvFolketrygden();
        var trygdeavgiftsgrunnlag = trygdeavgiftsgrunnlagService.hentAvgiftsgrunnlag(behandlingId);
        var avklarteMedfolgendeBarn = avklarteMedfolgendeFamilieService.hentAvklarteMedfølgendeBarn(behandlingId);
        var avklarteMedfolgendeEktefelle = avklarteMedfolgendeFamilieService.hentAvklartMedfølgendeEktefelle(behandlingId);

        //NOTE Henter i første versjon av FTRL kun en norsk arbeidsgiver og forventer ett registert arbeidsland
        AvklartVirksomhet norskeArbeidsgivere = avklarteVirksomheterService.hentNorskeArbeidsgivere(brevbestilling.getBehandling()).get(0);
        Landkoder arbeidsland = landvelgerService.hentArbeidsland(behandlingId);

        return new InnvilgelseFtrl(
            brevbestilling,
            medlemAvFolketrygden.getMedlemskapsperioder().stream().map(Periode::new).toList(),
            erFullstendigInnvilget(medlemAvFolketrygden.getMedlemskapsperioder()),
            hentSaerligBegrunnelse(behandlingsresultat),
            avklarteMedfolgendeEktefelle.finnes(),
            avklarteMedfolgendeBarn.finnes(),
            mapOmfattetFamilie(behandlingId, avklarteMedfolgendeEktefelle.getFamilieOmfattetAvNorskTrygd(), avklarteMedfolgendeBarn.barnOmfattetAvNorskTrygd),
            mapIkkeOmfattetBarn(behandlingId, avklarteMedfolgendeBarn.barnIkkeOmfattetAvNorskTrygd),
            mapIkkeOmfattetEktefelle(behandlingId, avklarteMedfolgendeEktefelle.getFamilieIkkeOmfattetAvNorskTrygd()),
            norskeArbeidsgivere.navn,
            arbeidsland.getBeskrivelse(),
            harTrygdeavtaleMedArbeidsland(arbeidsland),
            mapVurderingTrygdeavgift(trygdeavgiftsgrunnlag, medlemAvFolketrygden.getFastsattTrygdeavgift()),
            trygdeavgiftsgrunnlag.getLønnsforhold().getKode(),
            hentFullmektigNavnArbeidsgiver(brevbestilling.getBehandling().getFagsak()),
            brevbestilling.getBehandling().getFagsak().hentRepresentant(Representerer.BRUKER).isPresent(),
            String.valueOf(LocalDate.now().getYear()),
            harLønnNorgeSkattepliktigNorge(trygdeavgiftsgrunnlag.getAvgiftsGrunnlagNorge()),
            harLønnUtlandSkattepliktigNorge(trygdeavgiftsgrunnlag.getAvgiftsGrunnlagUtland())
        );
    }

    private boolean erFullstendigInnvilget(Collection<Medlemskapsperiode> medlemskapsperioder) {
        return medlemskapsperioder.stream()
            .filter(p -> p.getInnvilgelsesresultat() == InnvilgelsesResultat.INNVILGET).count() == medlemskapsperioder.size();
    }

    private String hentSaerligBegrunnelse(Behandlingsresultat behandlingsresultat) {
        Set<Vilkaarsresultat> vilkaarsresultater = behandlingsresultat.getVilkaarsresultater()
            .stream().filter(v -> v.getVilkaar().equals(FTRL_2_8_NÆR_TILKNYTNING_NORGE))
            .collect(Collectors.toSet());
        return vilkaarsresultater.isEmpty() ? null : vilkaarsresultater.iterator().next().getBegrunnelser().iterator().next().getKode();
    }

    private List<FamiliemedlemInfo> mapOmfattetFamilie(long behandlingID, Set<OmfattetFamilie> omfattetEktefelle, Set<OmfattetFamilie> omfattetBarn) {
        List<FamiliemedlemInfo> omfattetFamilie = new ArrayList<>();
        Map<String, MedfolgendeFamilie> medfolgendeEktefelle = avklarteMedfolgendeFamilieService.hentMedfølgendEktefelle(behandlingID);
        Map<String, MedfolgendeFamilie> avklarteMedfolgendeBarn = avklarteMedfolgendeFamilieService.hentMedfølgendeBarn(behandlingID);

        omfattetEktefelle.stream().map(omfattetEkte -> medfolgendeEktefelle.get(omfattetEkte.getUuid())).forEach(ektefelle -> {
            String sammensattNavn = ektefelle.fnr != null ? persondataFasade.hentSammensattNavn(ektefelle.fnr) : ektefelle.navn;
            omfattetFamilie.add(new FamiliemedlemInfo(sammensattNavn, ektefelle.fnr, IdentType.FNR));
        });

        omfattetBarn.stream().map(omfattetB -> avklarteMedfolgendeBarn.get(omfattetB.getUuid())).forEach(barn -> {
            String sammensattNavn = barn.fnr != null ? persondataFasade.hentSammensattNavn(barn.fnr) : barn.navn;
            omfattetFamilie.add(new FamiliemedlemInfo(sammensattNavn, barn.fnr, IdentType.FNR));
        });
        return omfattetFamilie;
    }

    private List<IkkeOmfattetBarn> mapIkkeOmfattetBarn(long behandlingID, Set<no.nav.melosys.domain.person.familie.IkkeOmfattetBarn> barnIkkeOmfattetAvNorskTrygd) {
        List<IkkeOmfattetBarn> ikkeOmfattet = new ArrayList<>();
        Map<String, MedfolgendeFamilie> medfoelgendeBarn = avklarteMedfolgendeFamilieService.hentMedfølgendeBarn(behandlingID);

        barnIkkeOmfattetAvNorskTrygd.forEach(ikkeOmfattetBarn -> {
            MedfolgendeFamilie barn = medfoelgendeBarn.get(ikkeOmfattetBarn.uuid);
            String sammensattNavn = barn.fnr != null ? persondataFasade.hentSammensattNavn(barn.fnr) : barn.navn;
            FamiliemedlemInfo familiemedlemInfo = new FamiliemedlemInfo(sammensattNavn, barn.fnr, IdentType.FNR);
            ikkeOmfattet.add(new IkkeOmfattetBarn(familiemedlemInfo, ikkeOmfattetBarn.begrunnelse));
        });
        return ikkeOmfattet;
    }

    private IkkeOmfattetEktefelle mapIkkeOmfattetEktefelle(long behandlingId, Set<no.nav.melosys.domain.person.familie.IkkeOmfattetFamilie> ektefelleIkkeOmfattet) {
        Map<String, MedfolgendeFamilie> medfolgendeEktefelle = avklarteMedfolgendeFamilieService.hentMedfølgendEktefelle(behandlingId);
        if (ektefelleIkkeOmfattet.isEmpty()) {
            return null;
        }

        IkkeOmfattetFamilie ikkeOmfattetEktefelle = ektefelleIkkeOmfattet.iterator().next();

        MedfolgendeFamilie ektefelle = medfolgendeEktefelle.get(ikkeOmfattetEktefelle.getUuid());
        String sammensattNavn = ektefelle.fnr != null ? persondataFasade.hentSammensattNavn(ektefelle.fnr) : ektefelle.navn;
        FamiliemedlemInfo familiemedlemInfo = new FamiliemedlemInfo(sammensattNavn, ektefelle.fnr, IdentType.FNR);
        return new IkkeOmfattetEktefelle(familiemedlemInfo, ikkeOmfattetEktefelle.getBegrunnelse());
    }

    private VurderingTrygdeavgift mapVurderingTrygdeavgift(Trygdeavgiftsgrunnlag trygdeavgiftsgrunnlag, FastsattTrygdeavgift fastsattTrygdeavgift) {
        TrygdeavgiftInfo norsk = null;
        TrygdeavgiftInfo utenlandsk = null;
        if (trygdeavgiftsgrunnlag.getAvgiftsGrunnlagNorge() != null) {
            AvgiftsgrunnlagInfoNorge avgiftsGrunnlagNorge = trygdeavgiftsgrunnlag.getAvgiftsGrunnlagNorge();
            norsk = new TrygdeavgiftInfo(
                fastsattTrygdeavgift.getAvgiftspliktigNorskInntektMnd(),
                avgiftsGrunnlagNorge.erAvgiftspliktig(),
                avgiftsGrunnlagNorge.erSkattepliktig(),
                avgiftsGrunnlagNorge.betalerArbeidsgiverAvgift(),
                avgiftsGrunnlagNorge.getSærligAvgiftsgruppe() != null ? avgiftsGrunnlagNorge.getSærligAvgiftsgruppe().getKode() : null
            );
        }
        if (trygdeavgiftsgrunnlag.getAvgiftsGrunnlagUtland() != null) {
            AvgiftsgrunnlagInfoUtland avgiftsGrunnlagUtland = trygdeavgiftsgrunnlag.getAvgiftsGrunnlagUtland();
            utenlandsk = new TrygdeavgiftInfo(
                fastsattTrygdeavgift.getAvgiftspliktigUtenlandskInntektMnd(),
                avgiftsGrunnlagUtland.erAvgiftspliktig(),
                avgiftsGrunnlagUtland.erSkattepliktig(),
                avgiftsGrunnlagUtland.betalerArbeidsgiverAvgift(),
                avgiftsGrunnlagUtland.getSærligAvgiftsgruppe() != null ? avgiftsGrunnlagUtland.getSærligAvgiftsgruppe().getKode() : null
            );
        }
        return new VurderingTrygdeavgift(norsk, utenlandsk);
    }

    private boolean harLønnNorgeSkattepliktigNorge(AvgiftsgrunnlagInfoNorge avgiftsgrunnlagInfoNorge) {
        return avgiftsgrunnlagInfoNorge != null && avgiftsgrunnlagInfoNorge.erSkattepliktig();
    }

    private boolean harLønnUtlandSkattepliktigNorge(AvgiftsgrunnlagInfoUtland avgiftsgrunnlagInfoUtland) {
        return avgiftsgrunnlagInfoUtland != null && avgiftsgrunnlagInfoUtland.erSkattepliktig();
    }

    private boolean harTrygdeavtaleMedArbeidsland(Landkoder arbeidsland) {
        for (Avtaleland a : Avtaleland.values()) {
            if (a.name().equals(arbeidsland.name())) {
                return true;
            }
        }
        return false;
    }

    private String hentFullmektigNavnArbeidsgiver(Fagsak fagsak) {
        return fagsak.hentRepresentant(Representerer.ARBEIDSGIVER)
            .map(aktoer -> eregFasade.hentOrganisasjonNavn(aktoer.getOrgnr()))
            .orElse(null);
    }
}
