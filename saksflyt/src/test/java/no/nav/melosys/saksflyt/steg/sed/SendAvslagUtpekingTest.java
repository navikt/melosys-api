package no.nav.melosys.saksflyt.steg.sed;

import java.time.LocalDate;
import java.util.Set;

import io.getunleash.FakeUnleash;
import no.nav.melosys.domain.*;
import no.nav.melosys.domain.dokument.medlemskap.Periode;
import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.domain.eessi.melding.UtpekingAvvis;
import no.nav.melosys.domain.eessi.sed.SedDataDto;
import no.nav.melosys.integrasjon.eessi.EessiConsumer;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.saksflytapi.domain.ProsessDataKey;
import no.nav.melosys.saksflytapi.domain.Prosessinstans;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.dokument.sed.EessiService;
import no.nav.melosys.service.dokument.sed.SedDataGrunnlagFactory;
import no.nav.melosys.service.dokument.sed.bygger.SedDataBygger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SendAvslagUtpekingTest {
    @Mock
    private SedDataBygger sedDataBygger;
    @Mock
    private SedDataGrunnlagFactory sedDataGrunnlagFactory;
    @Mock
    private EessiConsumer eessiConsumer;
    @Mock
    private JoarkFasade joarkFasade;
    @Mock
    private BehandlingService behandlingService;
    @Mock
    private BehandlingsresultatService behandlingsresultatService;

    private SendAvslagUtpeking sendAvslagUtpeking;
    private EessiService eessiService;
    private Behandling behandling;

    private final FakeUnleash fakeUnleash = new FakeUnleash();

    @BeforeEach
    public void settOpp() {
        eessiService = new EessiService(behandlingService, behandlingsresultatService, eessiConsumer, joarkFasade,
            sedDataBygger, sedDataGrunnlagFactory, fakeUnleash);
        sendAvslagUtpeking = new SendAvslagUtpeking(eessiService);

        SedDokument sedDokument = new SedDokument();
        sedDokument.setLovvalgsperiode(new Periode(LocalDate.now(), LocalDate.now()));
        sedDokument.setRinaSaksnummer("rinaSaksnummer");

        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setType(SaksopplysningType.SEDOPPL);
        saksopplysning.setDokument(sedDokument);

        Fagsak fagsak = FagsakTestFactory.builder().medGsakSaksnummer().build();

        behandling = BehandlingTestBuilder.builderWithDefaults()
            .medId(1L)
            .medFagsak(fagsak)
            .medSaksopplysninger(Set.of(saksopplysning))
            .build();

        when(sedDataBygger.lagUtkast(any(), any(), any())).thenReturn(new SedDataDto());
        when(behandlingService.hentBehandlingMedSaksopplysninger(1L)).thenReturn(behandling);
        when(behandlingsresultatService.hentBehandlingsresultat(1L)).thenReturn(new Behandlingsresultat());
    }

    @Test
    void utfør() {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);
        prosessinstans.setData(ProsessDataKey.UTPEKING_AVVIS, new UtpekingAvvis(
            "begrunnelse", true,
            "DK", "fritekst"
        ));

        sendAvslagUtpeking.utfør(prosessinstans);

        verify(eessiConsumer).sendSedPåEksisterendeBuc(any(), any(), any());
    }
}
