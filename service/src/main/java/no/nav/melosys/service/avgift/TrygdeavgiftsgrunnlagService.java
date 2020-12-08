package no.nav.melosys.service.avgift;

import java.util.Set;

import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.avgift.Avgiftsgrunnlag;
import no.nav.melosys.domain.avgift.OppdaterAvgiftsgrunnlagRequest;
import no.nav.melosys.domain.folketrygden.FastsattTrygdeavgift;
import no.nav.melosys.domain.folketrygden.MedlemAvFolketrygden;
import no.nav.melosys.domain.kodeverk.Avklartefaktatyper;
import no.nav.melosys.domain.kodeverk.Trygdeavgift_typer;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static no.nav.melosys.domain.kodeverk.Avklartefaktatyper.*;
import static no.nav.melosys.domain.kodeverk.Loenn_forhold.*;
import static no.nav.melosys.domain.kodeverk.Vurderingsutfall_trygdeavgift_norsk_inntekt.NORSK_INNTEKT_INGEN_TRYGDEAVGIFT_NAV;
import static no.nav.melosys.domain.kodeverk.Vurderingsutfall_trygdeavgift_norsk_inntekt.NORSK_INNTEKT_TRYGDEAVGIFT_NAV;
import static no.nav.melosys.domain.kodeverk.Vurderingsutfall_trygdeavgift_utenlandsk_inntekt.UTENLANDSK_INNTEKT_INGEN_TRYGDEAVGIFT_NAV;
import static no.nav.melosys.domain.kodeverk.Vurderingsutfall_trygdeavgift_utenlandsk_inntekt.UTENLANDSK_INNTEKT_TRYGDEAVGIFT_NAV;

@Service
public class TrygdeavgiftsgrunnlagService {

    private static final Set<Avklartefaktatyper> AVKLARTE_FAKTA_KODER = Set.of(
        LØNN_NORGE_SKATTEPLIKTIG_NORGE, LØNN_NORGE_ARBEIDSGIVERAVGIFT, LØNN_NORGE_SÆRLIG_AVGIFTS_GRUPPE,
        LØNN_UTL_SKATTEPLIKTIG_NORGE, LØNN_UTL_ARBEIDSGIVERAVGIFT, LØNN_UTL_SÆRLIG_AVGIFTS_GRUPPE
    );

    private final BehandlingsresultatService behandlingsresultatService;

    public TrygdeavgiftsgrunnlagService(BehandlingsresultatService behandlingsresultatService) {
        this.behandlingsresultatService = behandlingsresultatService;
    }

    @Transactional(rollbackFor = MelosysException.class)
    public Avgiftsgrunnlag oppdaterAvgiftsgrunnlag(long behandlingsresultatID, OppdaterAvgiftsgrunnlagRequest req) throws FunksjonellException {
        valider(req);
        Behandlingsresultat behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingsresultatID);
        behandlingsresultat.getAvklartefakta().removeIf(a -> AVKLARTE_FAKTA_KODER.contains(a.getType()));
        behandlingsresultat.getAvklartefakta().addAll(req.tilAvklartefakta());

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

    private boolean skalInnkreveAvgiftLønnNorge(OppdaterAvgiftsgrunnlagRequest req) {
        return (req.getLønnsforhold() == LØNN_FRA_NORGE || req.getLønnsforhold() == DELT_LØNN) &&
            req.getAvgiftsGrunnlagNorge().erAvgiftspliktig();
    }

    private boolean skalInnkreveAvgiftLønnUtland(OppdaterAvgiftsgrunnlagRequest req) {
        return (req.getLønnsforhold() == LØNN_FRA_UTLANDET || req.getLønnsforhold() == DELT_LØNN) &&
            req.getAvgiftsGrunnlagUtland().erAvgiftspliktig();
    }

    private void valider(OppdaterAvgiftsgrunnlagRequest req) throws FunksjonellException {
        if (req.getLønnsforhold() == null) {
            throw new FunksjonellException("Lønnsforhold ikke oppgitt");
        }

        if ((req.getLønnsforhold() == LØNN_FRA_NORGE || req.getLønnsforhold() == DELT_LØNN) && req.getAvgiftsGrunnlagNorge() == null) {
            throw new FunksjonellException("Mangler informasjon om lønn fra Norge");
        }
        if ((req.getLønnsforhold() == LØNN_FRA_UTLANDET || req.getLønnsforhold() == DELT_LØNN) && req.getAvgiftsGrunnlagUtland() == null) {
            throw new FunksjonellException("Mangler informasjon om lønn fra utlandet");
        }
    }

    @Transactional(readOnly = true)
    public Avgiftsgrunnlag hentAvgiftsgrunnlag(long behandlingresultatID) throws IkkeFunnetException {
        return Avgiftsgrunnlag.av(behandlingsresultatService.hentBehandlingsresultat(behandlingresultatID));

    }
}
