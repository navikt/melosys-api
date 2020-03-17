package no.nav.melosys.saksflyt.steg.afl.svar;

import java.time.LocalDate;
import java.util.Set;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.dokument.medlemskap.Periode;
import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.domain.eessi.melding.UtpekingAvvis;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.eessi.EessiConsumer;
import no.nav.melosys.integrasjon.eessi.dto.SedDataDto;
import no.nav.melosys.service.BehandlingService;
import no.nav.melosys.service.BehandlingsresultatService;
import no.nav.melosys.service.dokument.sed.SedDataGrunnlagFactory;
import no.nav.melosys.service.dokument.sed.bygger.SedDataBygger;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SendAvslagTest {

    @Mock
    private BehandlingService behandlingService;
    @Mock
    private BehandlingsresultatService behandlingsresultatService;
    @Mock
    private EessiConsumer eessiConsumer;
    @Mock
    private SedDataBygger sedDataBygger;
    @Mock
    private SedDataGrunnlagFactory sedDataGrunnlagFactory;
    private SendAvslag sendAvslag;

    private Behandling behandling;

    @Before
    public void settOpp() throws FunksjonellException, TekniskException {
        sendAvslag = new SendAvslag(
            behandlingService, behandlingsresultatService, eessiConsumer,
            sedDataBygger, sedDataGrunnlagFactory
        );

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
    }

    @Test
    public void utfør() throws MelosysException {
        UtpekingAvvis utpekingAvvis = new UtpekingAvvis(
            "begrunnelse", true,
            "DK", "fritekst"
        );

        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);
        prosessinstans.setData(ProsessDataKey.UTPEKING_AVVIS, utpekingAvvis);

        sendAvslag.utfør(prosessinstans);

        verify(eessiConsumer).sendSedPåEksisterendeBuc(any(), any(), any());
    }
}