package no.nav.melosys.saksflyt.steg.sed;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.BehandlingTestBuilder;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.FagsakTestFactory;
import no.nav.melosys.domain.arkiv.Journalpost;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.saksflytapi.domain.ProsessDataKey;
import no.nav.melosys.saksflytapi.domain.Prosessinstans;
import no.nav.melosys.service.dokument.sed.EessiService;
import no.nav.melosys.service.sak.FagsakService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OppdaterSaksrelasjonTest {
    @Mock
    private EessiService eessiService;
    @Mock
    private JoarkFasade joarkFasade;
    @Mock
    private FagsakService fagsakService;

    private OppdaterSaksrelasjon oppdaterSaksrelasjon;

    private static final String JOURNALPOST_ID = "123";

    @BeforeEach
    public void setup() {
        oppdaterSaksrelasjon = new OppdaterSaksrelasjon(joarkFasade, eessiService, fagsakService);
    }

    @Test
    void utfør_journalpostErFraEessi_verifiserOppdaterSaksrelasjon() {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setData(ProsessDataKey.JOURNALPOST_ID, JOURNALPOST_ID);

        Fagsak fagsak = FagsakTestFactory.builder().medGsakSaksnummer().build();
        Behandling behandling = BehandlingTestBuilder.builderWithDefaults().build();
        behandling.setFagsak(fagsak);
        prosessinstans.setBehandling(behandling);

        Journalpost journalpost = new Journalpost(JOURNALPOST_ID);
        journalpost.setMottaksKanal("EESSI");
        when(joarkFasade.hentJournalpost(JOURNALPOST_ID)).thenReturn(journalpost);

        MelosysEessiMelding melosysEessiMelding = new MelosysEessiMelding();
        melosysEessiMelding.setBucType("LA_BUC_04");
        melosysEessiMelding.setRinaSaksnummer("321323");
        when(eessiService.hentSedTilknyttetJournalpost(JOURNALPOST_ID)).thenReturn(melosysEessiMelding);

        oppdaterSaksrelasjon.utfør(prosessinstans);

        verify(eessiService).lagreSaksrelasjon(
            fagsak.getGsakSaksnummer(),
            melosysEessiMelding.getRinaSaksnummer(),
            melosysEessiMelding.getBucType()
        );
    }

    @Test
    void utfør_journalpostIkkeFraEessi_verifiserOppdatererIkkeSaksrelasjon() {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setData(ProsessDataKey.JOURNALPOST_ID, JOURNALPOST_ID);

        Journalpost journalpost = new Journalpost(JOURNALPOST_ID);
        journalpost.setMottaksKanal("flaskepost");
        when(joarkFasade.hentJournalpost(JOURNALPOST_ID)).thenReturn(journalpost);

        oppdaterSaksrelasjon.utfør(prosessinstans);
        verify(eessiService, never()).lagreSaksrelasjon(any(), any(), any());
    }

    @Test
    void utfør_eessiMeldingFinnesIData_verifiserOppdatererSaksrelasjon() {
        MelosysEessiMelding eessiMelding = new MelosysEessiMelding();
        eessiMelding.setRinaSaksnummer("12312");
        eessiMelding.setBucType("LA_BUC_06");

        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, eessiMelding);

        Fagsak fagsak = FagsakTestFactory.builder().medGsakSaksnummer().build();

        Behandling behandling = BehandlingTestBuilder.builderWithDefaults().build();
        behandling.setFagsak(fagsak);
        prosessinstans.setBehandling(behandling);

        oppdaterSaksrelasjon.utfør(prosessinstans);
        verify(eessiService).lagreSaksrelasjon(
            fagsak.getGsakSaksnummer(), eessiMelding.getRinaSaksnummer(), eessiMelding.getBucType()
        );
    }

    @Test
    void utfør_ingenBehandlingIngenArkivsakIDIProsessinstnas_henterArkivsakIDFraSaksnummerIFagsakServiceOppdatererSaksrelasjon() {
        final Fagsak fagsak = FagsakTestFactory.builder().medGsakSaksnummer().build();

        MelosysEessiMelding eessiMelding = new MelosysEessiMelding();
        eessiMelding.setRinaSaksnummer("12312");
        eessiMelding.setBucType("LA_BUC_06");

        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, eessiMelding);
        prosessinstans.setData(ProsessDataKey.SAKSNUMMER, fagsak.getSaksnummer());


        when(fagsakService.hentFagsak(fagsak.getSaksnummer())).thenReturn(fagsak);

        oppdaterSaksrelasjon.utfør(prosessinstans);
        verify(eessiService).lagreSaksrelasjon(
            fagsak.getGsakSaksnummer(), eessiMelding.getRinaSaksnummer(), eessiMelding.getBucType()
        );
    }
}
