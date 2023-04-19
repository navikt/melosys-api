package no.nav.melosys.service.avgift;

import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.avgift.OppdaterTrygdeavgiftsgrunnlagRequest;
import no.nav.melosys.domain.avgift.TrygdeavgiftsgrunnlagDeprecated;
import no.nav.melosys.domain.avklartefakta.Avklartefakta;
import no.nav.melosys.domain.folketrygden.FastsattTrygdeavgift;
import no.nav.melosys.domain.folketrygden.MedlemAvFolketrygden;
import no.nav.melosys.domain.kodeverk.Avklartefaktatyper;
import no.nav.melosys.domain.kodeverk.Trygdeavgift_typer;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static no.nav.melosys.domain.kodeverk.Avklartefaktatyper.*;
import static no.nav.melosys.domain.kodeverk.Loenn_forhold.*;
import static no.nav.melosys.domain.kodeverk.Vurderingsutfall_trygdeavgift_norsk_inntekt.NORSK_INNTEKT_INGEN_TRYGDEAVGIFT_NAV;
import static no.nav.melosys.domain.kodeverk.Vurderingsutfall_trygdeavgift_norsk_inntekt.NORSK_INNTEKT_TRYGDEAVGIFT_NAV;
import static no.nav.melosys.domain.kodeverk.Vurderingsutfall_trygdeavgift_utenlandsk_inntekt.UTENLANDSK_INNTEKT_INGEN_TRYGDEAVGIFT_NAV;
import static no.nav.melosys.domain.kodeverk.Vurderingsutfall_trygdeavgift_utenlandsk_inntekt.UTENLANDSK_INNTEKT_TRYGDEAVGIFT_NAV;

@Deprecated(since = "Skal fjernes med ny lagring av trygdeavgift: MELOSYS-5827")
@Service
public class TrygdeavgiftsgrunnlagServiceDeprecated {

    private static final Set<Avklartefaktatyper> AVKLARTE_FAKTA_KODER = Set.of(LØNN_FORHOLD_VIRKSOMHET,
        LØNN_NORGE_SKATTEPLIKTIG_NORGE, LØNN_NORGE_ARBEIDSGIVERAVGIFT, LØNN_NORGE_SÆRLIG_AVGIFTS_GRUPPE,
        LØNN_UTL_SKATTEPLIKTIG_NORGE, LØNN_UTL_ARBEIDSGIVERAVGIFT, LØNN_UTL_SÆRLIG_AVGIFTS_GRUPPE
    );

    private final BehandlingsresultatService behandlingsresultatService;

    public TrygdeavgiftsgrunnlagServiceDeprecated(BehandlingsresultatService behandlingsresultatService) {
        this.behandlingsresultatService = behandlingsresultatService;
    }

    @Deprecated(since = "Skal fjernes med ny lagring av trygdeavgift: MELOSYS-5827")
    @Transactional
    public TrygdeavgiftsgrunnlagDeprecated oppdaterAvgiftsgrunnlag(long behandlingsresultatID, OppdaterTrygdeavgiftsgrunnlagRequest req) {
        valider(req);
        Behandlingsresultat behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingsresultatID);
        oppdaterAvklartefakta(behandlingsresultat, req.tilAvklartefakta());

        MedlemAvFolketrygden medlemAvFolketrygden = hentEllerOpprettMedlemAvFolketrygden(behandlingsresultat);
        FastsattTrygdeavgift fastsattTrygdeavgift = hentEllerOpprettFastsattTrygdeavgift(medlemAvFolketrygden);
        fastsattTrygdeavgift.setTrygdeavgiftstype(Trygdeavgift_typer.FORELØPIG);

        medlemAvFolketrygden.setVurderingTrygdeavgiftNorskInntekt(
            skalInnkreveAvgiftLønnNorge(req)
                ? NORSK_INNTEKT_TRYGDEAVGIFT_NAV
                : NORSK_INNTEKT_INGEN_TRYGDEAVGIFT_NAV);

        medlemAvFolketrygden.setVurderingTrygdeavgiftUtenlandskInntekt(
            skalInnkreveAvgiftLønnUtland(req)
                ? UTENLANDSK_INNTEKT_TRYGDEAVGIFT_NAV
                : UTENLANDSK_INNTEKT_INGEN_TRYGDEAVGIFT_NAV);

        behandlingsresultatService.lagre(behandlingsresultat);
        return hentAvgiftsgrunnlag(behandlingsresultatID);
    }

    private void oppdaterAvklartefakta(Behandlingsresultat behandlingsresultat, Collection<Avklartefakta> oppdaterteAvklartefakta) {
        Map<Avklartefaktatyper, Avklartefakta> eksisterendeAvklartefaktaMap = behandlingsresultat.getAvklartefakta()
            .stream()
            .collect(Collectors.toMap(Avklartefakta::getType, a -> a));

        Set<Avklartefaktatyper> oppdaterteAvklartefaktatyper = oppdaterteAvklartefakta.stream()
            .map(Avklartefakta::getType)
            .collect(Collectors.toSet());

        for (Avklartefakta oppdatertAvklartfakta : oppdaterteAvklartefakta) {
            Optional.ofNullable(eksisterendeAvklartefaktaMap.get(oppdatertAvklartfakta.getType()))
                .ifPresentOrElse(
                    eksisterende -> {
                        eksisterende.setSubjekt(oppdatertAvklartfakta.getSubjekt());
                        eksisterende.setFakta(oppdatertAvklartfakta.getFakta());
                    },
                    () -> {
                        oppdatertAvklartfakta.setBehandlingsresultat(behandlingsresultat);
                        behandlingsresultat.getAvklartefakta().add(oppdatertAvklartfakta);
                    }

                );
        }

        behandlingsresultat.getAvklartefakta()
            .removeIf(a -> AVKLARTE_FAKTA_KODER.contains(a.getType()) && !oppdaterteAvklartefaktatyper.contains(a.getType()));
    }

    private MedlemAvFolketrygden hentEllerOpprettMedlemAvFolketrygden(Behandlingsresultat behandlingsresultat) {
        MedlemAvFolketrygden medlemAvFolketrygden = behandlingsresultat.getMedlemAvFolketrygden();
        if (medlemAvFolketrygden == null) {
            medlemAvFolketrygden = new MedlemAvFolketrygden();
            medlemAvFolketrygden.setBehandlingsresultat(behandlingsresultat);
            behandlingsresultat.setMedlemAvFolketrygden(medlemAvFolketrygden);
        }

        return medlemAvFolketrygden;
    }

    private FastsattTrygdeavgift hentEllerOpprettFastsattTrygdeavgift(MedlemAvFolketrygden medlemAvFolketrygden) {
        FastsattTrygdeavgift fastsattTrygdeavgift = medlemAvFolketrygden.getFastsattTrygdeavgift();
        if (fastsattTrygdeavgift == null) {
            fastsattTrygdeavgift = new FastsattTrygdeavgift();
            fastsattTrygdeavgift.setMedlemAvFolketrygden(medlemAvFolketrygden);
            medlemAvFolketrygden.setFastsattTrygdeavgift(fastsattTrygdeavgift);
        }

        return fastsattTrygdeavgift;
    }

    private boolean skalInnkreveAvgiftLønnNorge(OppdaterTrygdeavgiftsgrunnlagRequest req) {
        return (req.getLønnsforhold() == LØNN_FRA_NORGE || req.getLønnsforhold() == DELT_LØNN) &&
            req.getAvgiftsGrunnlagNorge().erAvgiftspliktig();
    }

    private boolean skalInnkreveAvgiftLønnUtland(OppdaterTrygdeavgiftsgrunnlagRequest req) {
        return (req.getLønnsforhold() == LØNN_FRA_UTLANDET || req.getLønnsforhold() == DELT_LØNN) &&
            req.getAvgiftsGrunnlagUtland().erAvgiftspliktig();
    }

    private void valider(OppdaterTrygdeavgiftsgrunnlagRequest req) {
        if (req.getLønnsforhold() == null) {
            throw new FunksjonellException("Lønnsforhold ikke oppgitt");
        }
        if (req.harLønnFraNorge()) {
            if (req.getAvgiftsGrunnlagNorge() == null) {
                throw new FunksjonellException("Mangler informasjon om lønn fra Norge");
            }
            req.getAvgiftsGrunnlagNorge().validerLovligeKominasjonerLønnFraNorge();
        }
        if (req.harLønnFraUtlandet()) {
            if (req.getAvgiftsGrunnlagUtland() == null) {
                throw new FunksjonellException("Mangler informasjon om lønn fra utlandet");
            }
            req.getAvgiftsGrunnlagUtland().validerLovligeKominasjonerLønnFraUtlandet();
        }
    }

    @Deprecated(since = "Skal fjernes med ny lagring av trygdeavgift: MELOSYS-5827")
    @Transactional(readOnly = true)
    public TrygdeavgiftsgrunnlagDeprecated hentAvgiftsgrunnlag(long behandlingresultatID) {
        return TrygdeavgiftsgrunnlagDeprecated.av(behandlingsresultatService.hentBehandlingsresultat(behandlingresultatID));

    }
}
