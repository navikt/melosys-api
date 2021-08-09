package no.nav.melosys.service.dokument;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Medlemskapsperiode;
import no.nav.melosys.domain.Vilkaarsresultat;
import no.nav.melosys.domain.avgift.AvgiftsgrunnlagInfoNorge;
import no.nav.melosys.domain.avgift.AvgiftsgrunnlagInfoUtland;
import no.nav.melosys.domain.avgift.Trygdeavgiftsgrunnlag;
import no.nav.melosys.domain.behandlingsgrunnlag.data.MedfolgendeFamilie;
import no.nav.melosys.domain.brev.InnvilgelseBrevbestilling;
import no.nav.melosys.domain.folketrygden.FastsattTrygdeavgift;
import no.nav.melosys.domain.person.familie.IkkeOmfattetFamilie;
import no.nav.melosys.domain.person.familie.OmfattetFamilie;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.integrasjon.dokgen.dto.InnvilgelseFtrl;
import no.nav.melosys.integrasjon.dokgen.dto.innvilgelseftrl.*;
import no.nav.melosys.service.avgift.TrygdeavgiftsgrunnlagService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.behandlingsgrunnlag.BehandlingsgrunnlagService;
import no.nav.melosys.service.persondata.PersondataFasade;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.kodeverk.Vilkaar.FTRL_2_8_NÆR_TILKNYTNING_NORGE;

@Component
public class InnvilgelseFtrlMapper {

    private final PersondataFasade persondataFasade;
    private final TrygdeavgiftsgrunnlagService trygdeavgiftsgrunnlagService;
    private final BehandlingsresultatService behandlingsresultatService;
    private final AvklartefaktaService avklartefaktaService;
    private final BehandlingsgrunnlagService behandlingsgrunnlagService;

    public InnvilgelseFtrlMapper(PersondataFasade persondataFasade,
                                 TrygdeavgiftsgrunnlagService trygdeavgiftsgrunnlagService,
                                 BehandlingsresultatService behandlingsresultatService,
                                 AvklartefaktaService avklartefaktaService,
                                 BehandlingsgrunnlagService behandlingsgrunnlagService) {
        this.persondataFasade = persondataFasade;
        this.trygdeavgiftsgrunnlagService = trygdeavgiftsgrunnlagService;
        this.behandlingsresultatService = behandlingsresultatService;
        this.avklartefaktaService = avklartefaktaService;
        this.behandlingsgrunnlagService = behandlingsgrunnlagService;
    }

    public InnvilgelseFtrl map(InnvilgelseBrevbestilling brevbestilling) {
        var behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(brevbestilling.getBehandlingId());
        var medlemAvFolketrygden = behandlingsresultat.getMedlemAvFolketrygden();
        var trygdeavgiftsgrunnlag = trygdeavgiftsgrunnlagService.hentAvgiftsgrunnlag(brevbestilling.getBehandlingId());
        var avklarteMedfolgendeBarn = avklartefaktaService.hentAvklarteMedfølgendeBarn(brevbestilling.getBehandlingId());
        var avklarteMedfolgendeEktefelle = avklartefaktaService.hentAvklarteMedfølgendeEktefelle(brevbestilling.getBehandlingId());

        return new InnvilgelseFtrl(
            brevbestilling,
            medlemAvFolketrygden.getMedlemskapsperioder().stream().map(Periode::new).collect(Collectors.toList()),
            erFullstendigInnvilget(medlemAvFolketrygden.getMedlemskapsperioder()),
            hentSaerligBegrunnelse(behandlingsresultat),
            avklarteMedfolgendeEktefelle.finnes(),
            avklarteMedfolgendeBarn.finnes(),
            mapOmfattetFamilie(brevbestilling.getBehandlingId(), avklarteMedfolgendeEktefelle.getFamilieOmfattetAvNorskTrygd(), avklarteMedfolgendeBarn.barnOmfattetAvNorskTrygd),
            hentIkkeOmfattetBarn(brevbestilling.getBehandlingId(), avklarteMedfolgendeBarn.barnIkkeOmfattetAvNorskTrygd),
            hentIkkeOmfattetEktefelle(brevbestilling.getBehandlingId(), avklarteMedfolgendeEktefelle.getFamilieIkkeOmfattetAvNorskTrygd()),
            "arbeidsgivernavn", //TODO
            (avklartefaktaService.hentAlleAvklarteArbeidsland(brevbestilling.getBehandlingId()).iterator().next().getBeskrivelse()),
            false, //TODO Hvor finnes det oversikt over land med trygdeavtale?
            mapVurderingTrygdeavgift(trygdeavgiftsgrunnlag, medlemAvFolketrygden.getFastsattTrygdeavgift()),
            trygdeavgiftsgrunnlag.getLønnsforhold().getKode(),
            "", //TODO
            false, //TODO
            String.valueOf(LocalDate.now().getYear()),
            harLønnNorgeSkattepliktigNorge(trygdeavgiftsgrunnlag.getAvgiftsGrunnlagNorge()),
            harLønnUtlandSkattepliktigNorge(trygdeavgiftsgrunnlag.getAvgiftsGrunnlagUtland())
        );
    }

    private boolean erFullstendigInnvilget(Collection<Medlemskapsperiode> medlemskapsperioder) {
        return medlemskapsperioder.stream().filter(Medlemskapsperiode::erInnvilget).count() == medlemskapsperioder.size();
    }

    private String hentSaerligBegrunnelse(Behandlingsresultat behandlingsresultat) {
        Set<Vilkaarsresultat> vilkaarsresultater = behandlingsresultat.getVilkaarsresultater()
            .stream().filter(v -> v.getVilkaar().equals(FTRL_2_8_NÆR_TILKNYTNING_NORGE))
            .collect(Collectors.toSet());
        return vilkaarsresultater.isEmpty() ? null : vilkaarsresultater.iterator().next().getBegrunnelser().iterator().next().getKode();
    }

    private List<FamiliemedlemInfo> mapOmfattetFamilie(long behandlingID, Set<OmfattetFamilie> omfattetEktefelle, Set<OmfattetFamilie> omfattetBarn) {
        List<FamiliemedlemInfo> omfattetFamilie = new ArrayList<>();
        Map<String, MedfolgendeFamilie> medfoelgendeBarn = hentMedfølgendeBarn(behandlingID);
        Map<String, MedfolgendeFamilie> medfolgendeEktefelle = hentMedfølgendEktefelle(behandlingID);
        for (OmfattetFamilie omfattetEkte : omfattetEktefelle) {
            if (!medfolgendeEktefelle.containsKey(omfattetEkte.getUuid())) {
                throw new FunksjonellException("Avklart medfølgende ektefelle/samboer " + omfattetEkte.getUuid() + " finnes ikke i behandlingsgrunnlaget");
            }
            MedfolgendeFamilie ektefelle = medfolgendeEktefelle.get(omfattetEkte.getUuid());
            String sammensattNavn = ektefelle.fnr != null ? persondataFasade.hentSammensattNavn(ektefelle.fnr) : ektefelle.navn;
            omfattetFamilie.add(new FamiliemedlemInfo(sammensattNavn, ektefelle.fnr, IdentType.FNR));
        }
        for (OmfattetFamilie omfattetB : omfattetBarn) {
            if (!medfoelgendeBarn.containsKey(omfattetB.getUuid())) {
                throw new FunksjonellException("Avklart medfølgende barn " + omfattetB.getUuid() + " finnes ikke i behandlingsgrunnlaget");
            }
            MedfolgendeFamilie barn = medfolgendeEktefelle.get(omfattetB.getUuid());
            String sammensattNavn = barn.fnr != null ? persondataFasade.hentSammensattNavn(barn.fnr) : barn.navn;
            omfattetFamilie.add(new FamiliemedlemInfo(sammensattNavn, barn.fnr, IdentType.FNR));
        }
        return omfattetFamilie;
    }

    private List<IkkeOmfattetBarn> hentIkkeOmfattetBarn(long behandlingID, Set<no.nav.melosys.domain.person.familie.IkkeOmfattetBarn> barnIkkeOmfattetAvNorskTrygd) {
        List<IkkeOmfattetBarn> ikkeOmfattet = new ArrayList<>();
        Map<String, MedfolgendeFamilie> medfoelgendeBarn = hentMedfølgendeBarn(behandlingID);
        for (no.nav.melosys.domain.person.familie.IkkeOmfattetBarn ikkeOmfattetBarn : barnIkkeOmfattetAvNorskTrygd) {
            if (!medfoelgendeBarn.containsKey(ikkeOmfattetBarn.uuid)) {
                throw new FunksjonellException("Avklart medfølgende barn " + ikkeOmfattetBarn.uuid + " finnes ikke i behandlingsgrunnlaget");
            }
            MedfolgendeFamilie barn = medfoelgendeBarn.get(ikkeOmfattetBarn.uuid);
            String sammensattNavn = barn.fnr != null ? persondataFasade.hentSammensattNavn(barn.fnr) : barn.navn;
            FamiliemedlemInfo familiemedlemInfo = new FamiliemedlemInfo(sammensattNavn, barn.fnr, IdentType.FNR);
            ikkeOmfattet.add(new IkkeOmfattetBarn(familiemedlemInfo, ikkeOmfattetBarn.begrunnelse));
        }
        return ikkeOmfattet;
    }

    private IkkeOmfattetEktefelle hentIkkeOmfattetEktefelle(long behandlingId, Set<no.nav.melosys.domain.person.familie.IkkeOmfattetFamilie> ektefelleIkkeOmfattet) {
        Map<String, MedfolgendeFamilie> medfolgendeEktefelle = hentMedfølgendEktefelle(behandlingId);
        IkkeOmfattetFamilie ikkeOmfattetEktefelle = ektefelleIkkeOmfattet.iterator().next();
        if (!medfolgendeEktefelle.containsKey(ikkeOmfattetEktefelle.getUuid())) {
            throw new FunksjonellException("Avklart medfølgende ektefelle/samboer " + ikkeOmfattetEktefelle.getUuid() + " finnes ikke i behandlingsgrunnlaget");
        }

        MedfolgendeFamilie ektefelle = medfolgendeEktefelle.get(ikkeOmfattetEktefelle.getUuid());
        String sammensattNavn = ektefelle.fnr != null ? persondataFasade.hentSammensattNavn(ektefelle.fnr) : ektefelle.navn;
        FamiliemedlemInfo familiemedlemInfo = new FamiliemedlemInfo(sammensattNavn, ektefelle.fnr, IdentType.FNR);
        return new IkkeOmfattetEktefelle(familiemedlemInfo, ikkeOmfattetEktefelle.getBegrunnelse());
    }

    private Map<String, MedfolgendeFamilie> hentMedfølgendeBarn(long behandlingID) {
        var behandlingsgrunnlag = behandlingsgrunnlagService.hentBehandlingsgrunnlag(behandlingID);
        return behandlingsgrunnlag == null ? Collections.emptyMap()
            : behandlingsgrunnlag.getBehandlingsgrunnlagdata().hentMedfølgendeBarn();
    }

    private Map<String, MedfolgendeFamilie> hentMedfølgendEktefelle(long behandlingID) {
        var behandlingsgrunnlag = behandlingsgrunnlagService.hentBehandlingsgrunnlag(behandlingID);
        return behandlingsgrunnlag == null ? Collections.emptyMap()
            : behandlingsgrunnlag.getBehandlingsgrunnlagdata().hentMedfølgendeEktefelle();
    }

    private VurderingTrygdeavgift mapVurderingTrygdeavgift(Trygdeavgiftsgrunnlag trygdeavgiftsgrunnlag, FastsattTrygdeavgift fastsattTrygdeavgift) {
        TrygdeavgiftInfo norsk = null;
        TrygdeavgiftInfo utenlandsk = null;
        if (trygdeavgiftsgrunnlag.getAvgiftsGrunnlagNorge() != null) {
            norsk = new TrygdeavgiftInfo(
                fastsattTrygdeavgift.getAvgiftspliktigNorskInntektMnd(),
                trygdeavgiftsgrunnlag.getAvgiftsGrunnlagNorge().erAvgiftspliktig(),
                Avgiftskode.E.name(), //TODO Flytte
                trygdeavgiftsgrunnlag.getAvgiftsGrunnlagNorge().getSærligAvgiftsgruppe().getKode()
            );
        }
        if (trygdeavgiftsgrunnlag.getAvgiftsGrunnlagUtland() != null) {
            utenlandsk = new TrygdeavgiftInfo(
                fastsattTrygdeavgift.getAvgiftspliktigUtenlandskInntektMnd(),
                trygdeavgiftsgrunnlag.getAvgiftsGrunnlagUtland().erAvgiftspliktig(),
                Avgiftskode.E.name(), //TODO
                trygdeavgiftsgrunnlag.getAvgiftsGrunnlagUtland().getSærligAvgiftsgruppe().getKode()
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
}
