package no.nav.melosys.saksflyt.steg.jfr;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.domain.arkiv.Journalpost;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.service.dokument.sed.EessiService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class OppdaterSaksrelasjonTest {

    @Mock
    private EessiService eessiService;
    @Mock
    private JoarkFasade joarkFasade;

    private OppdaterSaksrelasjon oppdaterSaksrelasjon;

    private static final String JOURNALPOST_ID = "123";

    @Before
    public void setup() {
        oppdaterSaksrelasjon = new OppdaterSaksrelasjon(joarkFasade, eessiService);
    }

    @Test
    public void utfør_journalpostErFraEessi_verifiserOppdaterSaksrelasjon() throws MelosysException {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setData(ProsessDataKey.JOURNALPOST_ID, JOURNALPOST_ID);

        Fagsak fagsak = new Fagsak();
        fagsak.setGsakSaksnummer(123L);
        Behandling behandling = new Behandling();
        behandling.setFagsak(fagsak);
        prosessinstans.setBehandling(behandling);

        Journalpost journalpost = new Journalpost(JOURNALPOST_ID);
        journalpost.setMottaksKanal("EESSI");
        when(joarkFasade.hentJournalpost(eq(JOURNALPOST_ID))).thenReturn(journalpost);

        MelosysEessiMelding melosysEessiMelding = new MelosysEessiMelding();
        melosysEessiMelding.setBucType("LA_BUC_04");
        melosysEessiMelding.setRinaSaksnummer("321323");
        when(eessiService.hentSedTilknyttetJournalpost(eq(JOURNALPOST_ID))).thenReturn(melosysEessiMelding);

        oppdaterSaksrelasjon.utfør(prosessinstans);

        verify(eessiService).lagreSaksrelasjon(
            eq(fagsak.getGsakSaksnummer()),
            eq(melosysEessiMelding.getRinaSaksnummer()),
            eq(melosysEessiMelding.getBucType())
        );
    }

    @Test
    public void utfør_journalpostIkkeFraEessi_verifiserOppdatererIkkeSaksrelasjon() throws MelosysException {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setData(ProsessDataKey.JOURNALPOST_ID, JOURNALPOST_ID);

        Journalpost journalpost = new Journalpost(JOURNALPOST_ID);
        journalpost.setMottaksKanal("flaskepost");
        when(joarkFasade.hentJournalpost(eq(JOURNALPOST_ID))).thenReturn(journalpost);

        oppdaterSaksrelasjon.utfør(prosessinstans);
        verify(eessiService, never()).lagreSaksrelasjon(any(), any(), any());
    }
}