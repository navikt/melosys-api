package no.nav.melosys.service.avgift;

import java.util.Set;

import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.avgift.Avgiftsgrunnlag;
import no.nav.melosys.domain.avgift.AvgiftsgrunnlagInfo;
import no.nav.melosys.domain.avgift.OppdaterAvgiftsgrunnlagRequest;
import no.nav.melosys.domain.avklartefakta.Avklartefakta;
import no.nav.melosys.domain.folketrygden.MedlemAvFolketrygden;
import no.nav.melosys.domain.kodeverk.*;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static no.nav.melosys.domain.avklartefakta.Avklartefakta.IKKE_VALGT_FAKTA;
import static no.nav.melosys.domain.avklartefakta.Avklartefakta.VALGT_FAKTA;
import static no.nav.melosys.domain.kodeverk.Avklartefaktatyper.*;
import static no.nav.melosys.domain.kodeverk.Vurderingsutfall_trygdeavgift_norsk_inntekt.NORSK_INNTEKT_TRYGDEAVGIFT_NAV;
import static no.nav.melosys.domain.kodeverk.Vurderingsutfall_trygdeavgift_utenlandsk_inntekt.UTENLANDSK_INNTEKT_TRYGDEAVGIFT_NAV;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrygdeavgiftsgrunnlagServiceTest {

    @Mock
    private BehandlingsresultatService behandlingsresultatService;

    private TrygdeavgiftsgrunnlagService trygdeavgiftsgrunnlagService;

    private final long behandlingsresultatID = 223;


    @BeforeEach
    void setup() {
        trygdeavgiftsgrunnlagService = new TrygdeavgiftsgrunnlagService(behandlingsresultatService);
    }

    @Test
    void lagreAvgiftsinformasjon_lønnsforholdNull_kasterFeil() {
        final var request = new OppdaterAvgiftsgrunnlagRequest(null, null, null);
        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> trygdeavgiftsgrunnlagService.oppdaterAvgiftsgrunnlag(behandlingsresultatID, request))
            .withMessageContaining("Lønnsforhold");
    }

    @Test
    void lagreAvgiftsinformasjon_lønnFraNorgeMenIkkeOppgitt_kasterFeil() {
        final var request = new OppdaterAvgiftsgrunnlagRequest(Loenn_forhold.LØNN_FRA_NORGE, null, null);
        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> trygdeavgiftsgrunnlagService.oppdaterAvgiftsgrunnlag(behandlingsresultatID, request))
            .withMessageContaining("Mangler informasjon om lønn fra Norge");
    }

    @Test
    void lagreAvgiftsinformasjon_lønnFraUtlandMenIkkeOppgitt_kasterFeil() {
        final var request = new OppdaterAvgiftsgrunnlagRequest(Loenn_forhold.LØNN_FRA_UTLANDET, null, null);
        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> trygdeavgiftsgrunnlagService.oppdaterAvgiftsgrunnlag(behandlingsresultatID, request))
            .withMessageContaining("Mangler informasjon om lønn fra utland");
    }

    @Test
    void lagreAvgiftsinformasjon_kunAvgiftspliktigNorge_lagres() throws FunksjonellException {
        final var behandlingsresultat = lagBehandlingsresultat();
        when(behandlingsresultatService.hentBehandlingsresultat(behandlingsresultatID)).thenReturn(behandlingsresultat);
        final var request = new OppdaterAvgiftsgrunnlagRequest(
            Loenn_forhold.LØNN_FRA_NORGE,
            new AvgiftsgrunnlagInfo(true, false, null),
            null);
        trygdeavgiftsgrunnlagService.oppdaterAvgiftsgrunnlag(behandlingsresultatID, request);

        assertThat(behandlingsresultat.getAvklartefakta())
            .containsExactlyInAnyOrder(
                new Avklartefakta(LØNN_FORHOLD_VIRKSOMHET, null, Loenn_forhold.LØNN_FRA_NORGE.getKode()),
                new Avklartefakta(LØNN_NORGE_SKATTEPLIKTIG_NORGE, null, VALGT_FAKTA),
                new Avklartefakta(LØNN_NORGE_ARBEIDSGIVERAVGIFT, null, IKKE_VALGT_FAKTA),
                new Avklartefakta(LØNN_NORGE_SÆRLIG_AVGIFTS_GRUPPE, null, IKKE_VALGT_FAKTA)
            );

        assertThat(behandlingsresultat.getMedlemAvFolketrygden()).isNotNull()
            .extracting(
                MedlemAvFolketrygden::getVurderingTrygdeavgiftNorskInntekt,
                MedlemAvFolketrygden::getVurderingTrygdeavgiftUtenlandskInntekt,
                m -> m.getFastsattTrygdeavgift().getTrygdeavgiftstype()
            )
            .containsExactly(
                NORSK_INNTEKT_TRYGDEAVGIFT_NAV,
                Vurderingsutfall_trygdeavgift_utenlandsk_inntekt.UTENLANDSK_INNTEKT_INGEN_TRYGDEAVGIFT_NAV,
                Trygdeavgift_typer.FORELØPIG
            );
    }

    @Test
    void lagreAvgiftsinformasjon_kunAvgiftspliktigUtland_lagres() throws FunksjonellException {
        final var behandlingsresultat = lagBehandlingsresultat();
        when(behandlingsresultatService.hentBehandlingsresultat(behandlingsresultatID)).thenReturn(behandlingsresultat);
        final var request = new OppdaterAvgiftsgrunnlagRequest(
            Loenn_forhold.LØNN_FRA_UTLANDET,
            null,
            new AvgiftsgrunnlagInfo(false, false, null));
        trygdeavgiftsgrunnlagService.oppdaterAvgiftsgrunnlag(behandlingsresultatID, request);

        assertThat(behandlingsresultat.getAvklartefakta())
            .containsExactlyInAnyOrder(
                new Avklartefakta(LØNN_FORHOLD_VIRKSOMHET, null, Loenn_forhold.LØNN_FRA_UTLANDET.getKode()),
                new Avklartefakta(LØNN_UTL_SKATTEPLIKTIG_NORGE, null, IKKE_VALGT_FAKTA),
                new Avklartefakta(LØNN_UTL_ARBEIDSGIVERAVGIFT, null, IKKE_VALGT_FAKTA),
                new Avklartefakta(LØNN_UTL_SÆRLIG_AVGIFTS_GRUPPE, null, IKKE_VALGT_FAKTA)
            );

        assertThat(behandlingsresultat.getMedlemAvFolketrygden()).isNotNull()
            .extracting(
                MedlemAvFolketrygden::getVurderingTrygdeavgiftNorskInntekt,
                MedlemAvFolketrygden::getVurderingTrygdeavgiftUtenlandskInntekt,
                m -> m.getFastsattTrygdeavgift().getTrygdeavgiftstype()
            )
            .containsExactly(
                Vurderingsutfall_trygdeavgift_norsk_inntekt.NORSK_INNTEKT_INGEN_TRYGDEAVGIFT_NAV,
                UTENLANDSK_INNTEKT_TRYGDEAVGIFT_NAV,
                Trygdeavgift_typer.FORELØPIG
            );
    }

    @Test
    void lagreAvgiftsinformasjon_deltLønn_lagres() throws FunksjonellException {
        final var behandlingsresultat = lagBehandlingsresultat();
        when(behandlingsresultatService.hentBehandlingsresultat(behandlingsresultatID)).thenReturn(behandlingsresultat);
        final var request = new OppdaterAvgiftsgrunnlagRequest(
            Loenn_forhold.DELT_LØNN,
            new AvgiftsgrunnlagInfo(true, false, null),
            new AvgiftsgrunnlagInfo(false, false, null));
        trygdeavgiftsgrunnlagService.oppdaterAvgiftsgrunnlag(behandlingsresultatID, request);

        assertThat(behandlingsresultat.getAvklartefakta())
            .containsExactlyInAnyOrder(
                new Avklartefakta(LØNN_FORHOLD_VIRKSOMHET, null, Loenn_forhold.DELT_LØNN.getKode()),
                new Avklartefakta(LØNN_NORGE_SKATTEPLIKTIG_NORGE, null, VALGT_FAKTA),
                new Avklartefakta(LØNN_NORGE_ARBEIDSGIVERAVGIFT, null, IKKE_VALGT_FAKTA),
                new Avklartefakta(LØNN_NORGE_SÆRLIG_AVGIFTS_GRUPPE, null, IKKE_VALGT_FAKTA),
                new Avklartefakta(LØNN_UTL_SKATTEPLIKTIG_NORGE, null, IKKE_VALGT_FAKTA),
                new Avklartefakta(LØNN_UTL_ARBEIDSGIVERAVGIFT, null, IKKE_VALGT_FAKTA),
                new Avklartefakta(LØNN_UTL_SÆRLIG_AVGIFTS_GRUPPE, null, IKKE_VALGT_FAKTA)
            );

        assertThat(behandlingsresultat.getMedlemAvFolketrygden()).isNotNull()
            .extracting(
                MedlemAvFolketrygden::getVurderingTrygdeavgiftNorskInntekt,
                MedlemAvFolketrygden::getVurderingTrygdeavgiftUtenlandskInntekt,
                m -> m.getFastsattTrygdeavgift().getTrygdeavgiftstype()
            )
            .containsExactly(
                NORSK_INNTEKT_TRYGDEAVGIFT_NAV,
                UTENLANDSK_INNTEKT_TRYGDEAVGIFT_NAV,
                Trygdeavgift_typer.FORELØPIG
            );
    }

    @Test
    void lagreAvgiftsinformasjon_lønnFraNorgeErMisjonær_ikkeAvgiftspliktig() throws FunksjonellException {
        final var behandlingsresultat = lagBehandlingsresultat();
        when(behandlingsresultatService.hentBehandlingsresultat(behandlingsresultatID)).thenReturn(behandlingsresultat);
        final var request = new OppdaterAvgiftsgrunnlagRequest(
            Loenn_forhold.LØNN_FRA_NORGE,
            new AvgiftsgrunnlagInfo(true, false, Saerligeavgiftsgrupper.MISJONÆR),
            null);
        trygdeavgiftsgrunnlagService.oppdaterAvgiftsgrunnlag(behandlingsresultatID, request);

        assertThat(behandlingsresultat.getAvklartefakta())
            .containsExactlyInAnyOrder(
                new Avklartefakta(LØNN_FORHOLD_VIRKSOMHET, null, Loenn_forhold.LØNN_FRA_NORGE.getKode()),
                new Avklartefakta(LØNN_NORGE_SKATTEPLIKTIG_NORGE, null, VALGT_FAKTA),
                new Avklartefakta(LØNN_NORGE_ARBEIDSGIVERAVGIFT, null, IKKE_VALGT_FAKTA),
                new Avklartefakta(LØNN_NORGE_SÆRLIG_AVGIFTS_GRUPPE, Saerligeavgiftsgrupper.MISJONÆR.getKode(), VALGT_FAKTA)
            );

        assertThat(behandlingsresultat.getMedlemAvFolketrygden()).isNotNull()
            .extracting(
                MedlemAvFolketrygden::getVurderingTrygdeavgiftNorskInntekt,
                MedlemAvFolketrygden::getVurderingTrygdeavgiftUtenlandskInntekt,
                m -> m.getFastsattTrygdeavgift().getTrygdeavgiftstype()
            )
            .containsExactly(
                Vurderingsutfall_trygdeavgift_norsk_inntekt.NORSK_INNTEKT_INGEN_TRYGDEAVGIFT_NAV,
                Vurderingsutfall_trygdeavgift_utenlandsk_inntekt.UTENLANDSK_INNTEKT_INGEN_TRYGDEAVGIFT_NAV,
                Trygdeavgift_typer.FORELØPIG
            );
    }

    @Test
    void hentAvgiftsgrunnlag_medAvklarteFakta_validerMapping() throws IkkeFunnetException {
        final var behandlingsresultat = lagBehandlingsresultat();
        when(behandlingsresultatService.hentBehandlingsresultat(behandlingsresultatID)).thenReturn(behandlingsresultat);
        behandlingsresultat.getAvklartefakta().addAll(Set.of(
            new Avklartefakta(LØNN_FORHOLD_VIRKSOMHET, null, Loenn_forhold.DELT_LØNN.getKode()),
            new Avklartefakta(LØNN_NORGE_SKATTEPLIKTIG_NORGE, null, VALGT_FAKTA),
            new Avklartefakta(LØNN_NORGE_ARBEIDSGIVERAVGIFT, null, IKKE_VALGT_FAKTA),
            new Avklartefakta(LØNN_NORGE_SÆRLIG_AVGIFTS_GRUPPE, Saerligeavgiftsgrupper.MISJONÆR.getKode(), VALGT_FAKTA),
            new Avklartefakta(LØNN_UTL_SKATTEPLIKTIG_NORGE, null, IKKE_VALGT_FAKTA),
            new Avklartefakta(LØNN_UTL_ARBEIDSGIVERAVGIFT, null, IKKE_VALGT_FAKTA),
            new Avklartefakta(LØNN_UTL_SÆRLIG_AVGIFTS_GRUPPE, null, IKKE_VALGT_FAKTA)
        ));
        behandlingsresultat.setMedlemAvFolketrygden(new MedlemAvFolketrygden());
        behandlingsresultat.getMedlemAvFolketrygden().setVurderingTrygdeavgiftUtenlandskInntekt(UTENLANDSK_INNTEKT_TRYGDEAVGIFT_NAV);
        behandlingsresultat.getMedlemAvFolketrygden().setVurderingTrygdeavgiftNorskInntekt(NORSK_INNTEKT_TRYGDEAVGIFT_NAV);

        assertThat(trygdeavgiftsgrunnlagService.hentAvgiftsgrunnlag(behandlingsresultatID))
            .extracting(
                Avgiftsgrunnlag::getLønnsforhold,
                a -> a.getAvgiftsGrunnlagNorge().getBetalerArbeidsgiverAvgift(),
                a -> a.getAvgiftsGrunnlagNorge().getErSkattepliktig(),
                a -> a.getAvgiftsGrunnlagNorge().getSærligAvgiftsgruppe(),
                a -> a.getAvgiftsGrunnlagNorge().getVurderingTrygdeavgiftNorskInntekt(),
                a -> a.getAvgiftsGrunnlagUtland().getBetalerArbeidsgiverAvgift(),
                a -> a.getAvgiftsGrunnlagUtland().getErSkattepliktig(),
                a -> a.getAvgiftsGrunnlagUtland().getSærligAvgiftsgruppe(),
                a -> a.getAvgiftsGrunnlagUtland().getVurderingTrygdeavgiftUtenlandskInntekt()
            ).containsExactly(
                Loenn_forhold.DELT_LØNN,
                false,
                true,
                Saerligeavgiftsgrupper.MISJONÆR,
                NORSK_INNTEKT_TRYGDEAVGIFT_NAV,
                false,
                false,
                null,
                UTENLANDSK_INNTEKT_TRYGDEAVGIFT_NAV
        );
    }

    private Behandlingsresultat lagBehandlingsresultat() {
        return new Behandlingsresultat();
    }

}