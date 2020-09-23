package no.nav.melosys.saksflyt.steg.afl.svar;

import java.time.LocalDate;
import java.util.Set;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.domain.dokument.medlemskap.Periode;
import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.domain.eessi.melding.UtpekingAvvis;
import no.nav.melosys.domain.eessi.sed.SedDataDto;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.eessi.EessiConsumer;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.dokument.sed.EessiService;
import no.nav.melosys.service.dokument.sed.SedDataGrunnlagFactory;
import no.nav.melosys.service.dokument.sed.bygger.SedDataBygger;
import no.nav.melosys.service.eessi.SedGrunnlagMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static no.nav.melosys.domain.saksflyt.ProsessSteg.AFL_SVAR_AVSLUTT_BEHANDLING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SemdAvslagUtpekingTest {

    @Mock
    private SedDataBygger sedDataBygger;
    @Mock
    private SedDataGrunnlagFactory sedDataGrunnlagFactory;
    @Mock
    private EessiConsumer eessiConsumer;
    @Mock
    private BehandlingService behandlingService;
    @Mock
    private BehandlingsresultatService behandlingsresultatService;
    @Mock
    private SedGrunnlagMapper sedGrunnlagMapper;

    private SemdAvslagUtpeking semdAvslagUtpeking;
    private EessiService eessiService;
    private Behandling behandling;

    @Before
    public void settOpp() throws FunksjonellException, TekniskException {
        eessiService = new EessiService(
            sedDataBygger, sedDataGrunnlagFactory,
            eessiConsumer, behandlingService, behandlingsresultatService, sedGrunnlagMapper
        );
        semdAvslagUtpeking = new SemdAvslagUtpeking(eessiService);

        SedDokument sedDokument = new SedDokument();
        sedDokument.setLovvalgsperiode(new Periode(LocalDate.now(), LocalDate.now()));
        sedDokument.setRinaSaksnummer("rinaSaksnummer");

        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setType(SaksopplysningType.SEDOPPL);
        saksopplysning.setDokument(sedDokument);

        behandling = new Behandling();
        behandling.setId(1L);
        behandling.setSaksopplysninger(Set.of(saksopplysning));

        when(sedDataBygger.lagUtkast(any(), any(), any())).thenReturn(new SedDataDto());
        when(behandlingService.hentBehandling(1L)).thenReturn(behandling);
    }

    @Test
    public void utfør() throws MelosysException {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);
        prosessinstans.setData(ProsessDataKey.UTPEKING_AVVIS, new UtpekingAvvis(
            "begrunnelse", true,
            "DK", "fritekst"
        ));

        semdAvslagUtpeking.utfør(prosessinstans);

        verify(eessiConsumer).sendSedPåEksisterendeBuc(any(), any(), any());

        assertThat(prosessinstans.getSteg()).isEqualTo(AFL_SVAR_AVSLUTT_BEHANDLING);
    }
}