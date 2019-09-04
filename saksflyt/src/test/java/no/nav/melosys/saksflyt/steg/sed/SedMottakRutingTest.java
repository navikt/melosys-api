package no.nav.melosys.saksflyt.steg.sed;

import java.util.Collections;
import java.util.Optional;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.dokument.sed.EessiService;
import no.nav.melosys.service.eessi.AutomatiskSedBehandlingInitialiserer;
import no.nav.melosys.service.eessi.ManuellSedBehandlingInitialiserer;
import no.nav.melosys.service.eessi.RutingResultat;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class SedMottakRutingTest {

    @Mock
    private AutomatiskSedBehandlingInitialiserer automatiskSedBehandlingInitialiserer;
    @Mock
    private ManuellSedBehandlingInitialiserer manuellSedBehandlingInitialiserer;
    @Mock
    private EessiService eessiService;

    private SedMottakRuting sedMottakRuting;

    @Before
    public void setUp() throws MelosysException {
        sedMottakRuting = new SedMottakRuting(Collections.singleton(automatiskSedBehandlingInitialiserer), manuellSedBehandlingInitialiserer, eessiService);
        when(automatiskSedBehandlingInitialiserer.gjelderSedType(any(), any())).thenReturn(true);
        when(eessiService.finnSakForRinasaksnummer(anyString())).thenReturn(Optional.of(1L));
    }

    @Test
    public void utfør_resultatIngenBehandling_verifiserProsessStegJournalpost() throws Exception {
        doAnswer(invocationOnMock -> {
            Prosessinstans prosessinstans = invocationOnMock.getArgument(0);
            prosessinstans.setBehandling(new Behandling());
            return RutingResultat.INGEN_BEHANDLING;
        }).when(automatiskSedBehandlingInitialiserer).finnSakOgBestemRuting(any(Prosessinstans.class), anyLong());

        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setSteg(sedMottakRuting.inngangsSteg());
        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, hentMelosysEessiMelding());

        sedMottakRuting.utfør(prosessinstans);

        verify(automatiskSedBehandlingInitialiserer).finnSakOgBestemRuting(any(), anyLong());
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.SED_MOTTAK_FERDIGSTILL_JOURNALPOST);
    }

    @Test
    public void utfør_resultatOppdaterBehandling_verifiserProsessStegJournalpost() throws Exception {
        doAnswer(invocationOnMock -> {
            Prosessinstans prosessinstans = invocationOnMock.getArgument(0);
            prosessinstans.setBehandling(new Behandling());
            return RutingResultat.OPPDATER_BEHANDLING;
        }).when(automatiskSedBehandlingInitialiserer).finnSakOgBestemRuting(any(Prosessinstans.class), anyLong());

        doAnswer(invocationOnMock -> ProsessType.ANMODNING_OM_UNNTAK_SVAR)
            .when(automatiskSedBehandlingInitialiserer).hentAktuellProsessType();

        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setSteg(sedMottakRuting.inngangsSteg());
        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, hentMelosysEessiMelding());

        sedMottakRuting.utfør(prosessinstans);

        verify(automatiskSedBehandlingInitialiserer).finnSakOgBestemRuting(any(), anyLong());
        assertThat(prosessinstans.getType()).isEqualTo(ProsessType.ANMODNING_OM_UNNTAK_SVAR);
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.SED_MOTTAK_FERDIGSTILL_JOURNALPOST);
    }

    @Test
    public void utfør_resultatNyBehandling_verifiserProsessStegNyBehandling() throws Exception {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setSteg(sedMottakRuting.inngangsSteg());
        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, hentMelosysEessiMelding());

        when(automatiskSedBehandlingInitialiserer.finnSakOgBestemRuting(any(), anyLong())).thenReturn(RutingResultat.NY_BEHANDLING);
        when(automatiskSedBehandlingInitialiserer.hentAktuellProsessType()).thenReturn(ProsessType.REGISTRERING_UNNTAK);

        sedMottakRuting.utfør(prosessinstans);

        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.SED_MOTTAK_OPPRETT_NY_BEHANDLING);
    }

    @Test
    public void utfør_resultatNySak_verifiserProsessStegNyFagsakOgBehandling() throws Exception {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setSteg(sedMottakRuting.inngangsSteg());
        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, hentMelosysEessiMelding());

        when(automatiskSedBehandlingInitialiserer.finnSakOgBestemRuting(any(), anyLong())).thenReturn(RutingResultat.NY_SAK);
        when(automatiskSedBehandlingInitialiserer.hentAktuellProsessType()).thenReturn(ProsessType.REGISTRERING_UNNTAK);

        sedMottakRuting.utfør(prosessinstans);

        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.SED_MOTTAK_OPPRETT_FAGSAK_OG_BEH);
    }

    @Test(expected = TekniskException.class)
    public void utfør_resultatNullVerdi_verifiserKasterException() throws Exception {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setSteg(sedMottakRuting.inngangsSteg());
        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, hentMelosysEessiMelding());

        when(automatiskSedBehandlingInitialiserer.finnSakOgBestemRuting(any(), anyLong())).thenReturn(null);

        sedMottakRuting.utfør(prosessinstans);
    }

    private MelosysEessiMelding hentMelosysEessiMelding() {
        MelosysEessiMelding melosysEessiMelding = new MelosysEessiMelding();
        melosysEessiMelding.setSedType("A009");
        melosysEessiMelding.setRinaSaksnummer("57483697");
        return melosysEessiMelding;
    }
}