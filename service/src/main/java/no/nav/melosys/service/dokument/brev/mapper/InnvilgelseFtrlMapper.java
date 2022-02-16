package no.nav.melosys.service.dokument.brev.mapper;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Stream;
import javax.transaction.Transactional;

import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Medlemskapsperiode;
import no.nav.melosys.domain.avgift.AvgiftsgrunnlagInfoNorge;
import no.nav.melosys.domain.avgift.AvgiftsgrunnlagInfoUtland;
import no.nav.melosys.domain.avgift.Trygdeavgiftsgrunnlag;
import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet;
import no.nav.melosys.domain.behandlingsgrunnlag.BehandlingsgrunnlagData;
import no.nav.melosys.domain.behandlingsgrunnlag.data.MedfolgendeFamilie;
import no.nav.melosys.domain.behandlingsgrunnlag.data.Soeknadsland;
import no.nav.melosys.domain.brev.InnvilgelseBrevbestilling;
import no.nav.melosys.domain.folketrygden.FastsattTrygdeavgift;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Avtaleland;
import no.nav.melosys.domain.kodeverk.InnvilgelsesResultat;
import no.nav.melosys.domain.kodeverk.Representerer;
import no.nav.melosys.domain.kodeverk.begrunnelser.Medfolgende_barn_begrunnelser;
import no.nav.melosys.domain.person.familie.OmfattetFamilie;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.integrasjon.dokgen.dto.InnvilgelseFtrl;
import no.nav.melosys.integrasjon.dokgen.dto.innvilgelseftrl.*;
import no.nav.melosys.service.avgift.TrygdeavgiftsgrunnlagService;
import no.nav.melosys.service.avklartefakta.AvklarteMedfolgendeFamilieService;
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService;
import no.nav.melosys.service.representant.RepresentantService;
import org.springframework.stereotype.Component;

import static java.util.Optional.ofNullable;
import static no.nav.melosys.domain.kodeverk.Vilkaar.FTRL_2_8_NÆR_TILKNYTNING_NORGE;
import static org.springframework.util.StringUtils.hasText;

@Component
public class InnvilgelseFtrlMapper {

    private final TrygdeavgiftsgrunnlagService trygdeavgiftsgrunnlagService;
    private final AvklarteVirksomheterService avklarteVirksomheterService;
    private final AvklarteMedfolgendeFamilieService avklarteMedfolgendeFamilieService;
    private final RepresentantService representantService;
    private final DokgenMapperDatahenter dokgenMapperDatahenter;

    public InnvilgelseFtrlMapper(TrygdeavgiftsgrunnlagService trygdeavgiftsgrunnlagService,
                                 AvklarteVirksomheterService avklarteVirksomheterService,
                                 AvklarteMedfolgendeFamilieService avklarteMedfolgendeFamilieService,
                                 RepresentantService representantService,
                                 DokgenMapperDatahenter dokgenMapperDatahenter) {
        this.trygdeavgiftsgrunnlagService = trygdeavgiftsgrunnlagService;
        this.avklarteVirksomheterService = avklarteVirksomheterService;
        this.avklarteMedfolgendeFamilieService = avklarteMedfolgendeFamilieService;
        this.representantService = representantService;
        this.dokgenMapperDatahenter = dokgenMapperDatahenter;
    }

    @Transactional
    public InnvilgelseFtrl map(InnvilgelseBrevbestilling brevbestilling) {
        long behandlingId = brevbestilling.getBehandlingId();
        var behandlingsresultat = dokgenMapperDatahenter.hentBehandlingsresultat(behandlingId);
        var medlemAvFolketrygden = behandlingsresultat.getMedlemAvFolketrygden();
        var trygdeavgiftsgrunnlag = trygdeavgiftsgrunnlagService.hentAvgiftsgrunnlag(behandlingId);
        var avklarteMedfolgendeBarn = avklarteMedfolgendeFamilieService.hentAvklarteMedfølgendeBarn(behandlingId);
        var avklarteMedfolgendeEktefelle = avklarteMedfolgendeFamilieService.hentAvklartMedfølgendeEktefelle(behandlingId);

        //NOTE Henter i første versjon av FTRL kun en norsk arbeidsgiver og forventer ett registert arbeidsland
        AvklartVirksomhet norskeArbeidsgivere = avklarteVirksomheterService.hentNorskeArbeidsgivere(brevbestilling.getBehandling()).get(0);
        BehandlingsgrunnlagData behandlingsgrunnlagdata = behandlingsresultat.getBehandling().getBehandlingsgrunnlag().getBehandlingsgrunnlagdata();
        Soeknadsland soeknadsland = behandlingsgrunnlagdata.soeknadsland;
        String arbeidsland = soeknadsland.landkoder.get(0);

        Collection<Medlemskapsperiode> medlemskapsperioder = medlemAvFolketrygden.getMedlemskapsperioder();
        FastsattTrygdeavgift fastsattTrygdeavgift = medlemAvFolketrygden.getFastsattTrygdeavgift();
        return new InnvilgelseFtrl.Builder(brevbestilling)
            .perioder(medlemskapsperioder.stream().map(Periode::new).toList())
            .erFullstendigInnvilget(erFullstendigInnvilget(medlemskapsperioder))
            .ftrl_2_8_begrunnelse(hentSaerligBegrunnelse(behandlingsresultat))
            .vurderingMedlemskapEktefelle(avklarteMedfolgendeEktefelle.finnes())
            .vurderingLovvalgBarn(avklarteMedfolgendeBarn.finnes())
            .omfattetFamilie(mapOmfattetFamilie(behandlingId, avklarteMedfolgendeEktefelle.getFamilieOmfattetAvNorskTrygd(), avklarteMedfolgendeBarn.getFamilieOmfattetAvNorskTrygd()))
            .ikkeOmfattetEktefelle(mapIkkeOmfattetEktefelle(behandlingId, avklarteMedfolgendeEktefelle.getFamilieIkkeOmfattetAvNorskTrygd()))
            .ikkeOmfattetBarn(mapIkkeOmfattetBarn(behandlingId, avklarteMedfolgendeBarn.getFamilieIkkeOmfattetAvNorskTrygd()))
            .arbeidsgiverNavn(norskeArbeidsgivere.navn)
            .arbeidsland(dokgenMapperDatahenter.hentLandnavnFraLandkode(arbeidsland))
            .trygdeavtaleMedArbeidsland(harTrygdeavtaleMedArbeidsland(arbeidsland))
            .vurderingTrygdeavgift(mapVurderingTrygdeavgift(trygdeavgiftsgrunnlag, fastsattTrygdeavgift))
            .loennsforhold(trygdeavgiftsgrunnlag.getLønnsforhold().getKode())
            .arbeidsgiverFullmektigNavn(dokgenMapperDatahenter.hentFullmektigNavn(brevbestilling.getBehandling().getFagsak(), Representerer.ARBEIDSGIVER))
            .avgiftssatsAar(String.valueOf(LocalDate.now().getYear()))
            .loennNorgeSkattepliktig(harLønnNorgeSkattepliktigNorge(trygdeavgiftsgrunnlag.getAvgiftsGrunnlagNorge()))
            .loennUtlandSkattepliktig(harLønnUtlandSkattepliktigNorge(trygdeavgiftsgrunnlag.getAvgiftsGrunnlagUtland()))
            .build();
    }

    private boolean erFullstendigInnvilget(Collection<Medlemskapsperiode> medlemskapsperioder) {
        return medlemskapsperioder.stream()
            .allMatch(p -> p.getInnvilgelsesresultat() == InnvilgelsesResultat.INNVILGET);
    }

    private String hentSaerligBegrunnelse(Behandlingsresultat behandlingsresultat) {
        return behandlingsresultat.getVilkaarsresultater().stream()
            .findFirst()
            .filter(v -> v.getVilkaar() == FTRL_2_8_NÆR_TILKNYTNING_NORGE)
            .map(vilkaarsresultat -> vilkaarsresultat.getBegrunnelser().iterator().next().getKode())
            .orElse(null);
    }

    private List<FamiliemedlemInfo> mapOmfattetFamilie(long behandlingID, Set<OmfattetFamilie> omfattetEktefelle, Set<OmfattetFamilie> omfattetBarn) {
        Map<String, MedfolgendeFamilie> medfolgendeEktefelle = avklarteMedfolgendeFamilieService.hentMedfølgendEktefelle(behandlingID);
        Map<String, MedfolgendeFamilie> medfolgendeBarn = avklarteMedfolgendeFamilieService.hentMedfølgendeBarn(behandlingID);

        return Stream.concat(
            omfattetEktefelle.stream()
                .map(ektefelle -> tilFamiliemedlemInfo(medfolgendeEktefelle, ektefelle.getUuid())),
            omfattetBarn.stream()
                .map(barn -> tilFamiliemedlemInfo(medfolgendeBarn, barn.getUuid()))
        ).toList();
    }

    private List<IkkeOmfattetBarn> mapIkkeOmfattetBarn(long behandlingID, Set<no.nav.melosys.domain.person.familie.IkkeOmfattetFamilie> barnIkkeOmfattetAvNorskTrygd) {
        Map<String, MedfolgendeFamilie> medfoelgendeBarn = avklarteMedfolgendeFamilieService.hentMedfølgendeBarn(behandlingID);
        return barnIkkeOmfattetAvNorskTrygd.stream()
            .map(ikkeOmfattetBarn -> new IkkeOmfattetBarn(
                tilFamiliemedlemInfo(medfoelgendeBarn, ikkeOmfattetBarn.getUuid()),
                Medfolgende_barn_begrunnelser.valueOf(ikkeOmfattetBarn.getBegrunnelse())))
            .toList();
    }

    private IkkeOmfattetEktefelle mapIkkeOmfattetEktefelle(long behandlingId, Set<no.nav.melosys.domain.person.familie.IkkeOmfattetFamilie> ektefelleIkkeOmfattet) {
        return ektefelleIkkeOmfattet.stream()
            .findFirst()
            .map(ikkeOmfattet -> new IkkeOmfattetEktefelle(
                tilFamiliemedlemInfo(avklarteMedfolgendeFamilieService.hentMedfølgendEktefelle(behandlingId), ikkeOmfattet.getUuid()),
                ikkeOmfattet.getBegrunnelse()))
            .orElse(null);
    }

    private FamiliemedlemInfo tilFamiliemedlemInfo(Map<String, MedfolgendeFamilie> avklartMedfolgende, String uuid) {
        MedfolgendeFamilie medfolgendeFamilie = Optional.of(avklartMedfolgende.get(uuid))
            .orElseThrow(() -> new FunksjonellException("Avklart medfølgende familie " + uuid + " finnes ikke i behandlingsgrunnlaget"));
        String sammensattNavn = medfolgendeFamilie.getFnr() != null ? dokgenMapperDatahenter.hentSammensattNavn(medfolgendeFamilie.getFnr()) : medfolgendeFamilie.getNavn();
        return new FamiliemedlemInfo(sammensattNavn, medfolgendeFamilie.getFnr(), medfolgendeFamilie.utledIdentType());
    }

    private VurderingTrygdeavgift mapVurderingTrygdeavgift(Trygdeavgiftsgrunnlag trygdeavgiftsgrunnlag, FastsattTrygdeavgift fastsattTrygdeavgift) {
        TrygdeavgiftInfo norsk = null;
        TrygdeavgiftInfo utenlandsk = null;
        if (trygdeavgiftsgrunnlag.getAvgiftsGrunnlagNorge() != null) {
            AvgiftsgrunnlagInfoNorge avgiftsGrunnlagNorge = trygdeavgiftsgrunnlag.getAvgiftsGrunnlagNorge();
            norsk = new TrygdeavgiftInfo(
                ofNullable(fastsattTrygdeavgift.getAvgiftspliktigNorskInntektMnd()).orElse(0L),
                avgiftsGrunnlagNorge.erAvgiftspliktig(),
                avgiftsGrunnlagNorge.erSkattepliktig(),
                avgiftsGrunnlagNorge.betalerArbeidsgiverAvgift(),
                avgiftsGrunnlagNorge.getSærligAvgiftsgruppe() != null ? avgiftsGrunnlagNorge.getSærligAvgiftsgruppe().getKode() : null
            );
        }
        if (trygdeavgiftsgrunnlag.getAvgiftsGrunnlagUtland() != null) {
            AvgiftsgrunnlagInfoUtland avgiftsGrunnlagUtland = trygdeavgiftsgrunnlag.getAvgiftsGrunnlagUtland();
            utenlandsk = new TrygdeavgiftInfo(
                ofNullable(fastsattTrygdeavgift.getAvgiftspliktigUtenlandskInntektMnd()).orElse(0L),
                avgiftsGrunnlagUtland.erAvgiftspliktig(),
                avgiftsGrunnlagUtland.erSkattepliktig(),
                avgiftsGrunnlagUtland.betalerArbeidsgiverAvgift(),
                avgiftsGrunnlagUtland.getSærligAvgiftsgruppe() != null ? avgiftsGrunnlagUtland.getSærligAvgiftsgruppe().getKode() : null
            );
        }
        return new VurderingTrygdeavgift(
            norsk,
            utenlandsk,
            fastsattTrygdeavgift.getBetalesAv().getRolle() == Aktoersroller.BRUKER,
            hentRepresentantNavn(fastsattTrygdeavgift.getRepresentantNr())
        );
    }

    private boolean harLønnNorgeSkattepliktigNorge(AvgiftsgrunnlagInfoNorge avgiftsgrunnlagInfoNorge) {
        return avgiftsgrunnlagInfoNorge != null && avgiftsgrunnlagInfoNorge.erSkattepliktig();
    }

    private boolean harLønnUtlandSkattepliktigNorge(AvgiftsgrunnlagInfoUtland avgiftsgrunnlagInfoUtland) {
        return avgiftsgrunnlagInfoUtland != null && avgiftsgrunnlagInfoUtland.erSkattepliktig();
    }

    private boolean harTrygdeavtaleMedArbeidsland(String arbeidsland) {
        return Arrays.stream(Avtaleland.values()).anyMatch(a -> a.name().equals(arbeidsland));
    }

    private String hentRepresentantNavn(String representantNr) {
        if (hasText(representantNr)) {
            return representantService.hentRepresentant(representantNr).navn();
        }
        return null;
    }
}
