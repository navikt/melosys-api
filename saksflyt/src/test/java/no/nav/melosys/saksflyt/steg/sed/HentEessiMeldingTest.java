package no.nav.melosys.saksflyt.steg.sed;

import no.nav.melosys.domain.ProsessDataKey;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.integrasjon.tps.TpsService;
import no.nav.melosys.service.dokument.sed.EessiService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class HentEessiMeldingTest {

    @Mock
    private EessiService eessiService;
    @Mock
    private TpsService tpsService;

    private HentEessiMelding hentEessiMelding;

    @Before
    public void setup() {
        hentEessiMelding = new HentEessiMelding(eessiService, tpsService);
    }

    @Test
    public void utfør() throws MelosysException {
        final String journalpostID = "jp123";
        final String brukerID = "bruker123";
        when(eessiService.hentSedTilknyttetJournalpost(journalpostID))
            .thenReturn(new MelosysEessiMelding());
        when(tpsService.hentAktørIdForIdent(brukerID))
            .thenReturn("aktør123");

        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setData(ProsessDataKey.JOURNALPOST_ID, journalpostID);
        prosessinstans.setData(ProsessDataKey.BRUKER_ID, brukerID);

        hentEessiMelding.utfør(prosessinstans);

        assertThat(prosessinstans.getData(ProsessDataKey.EESSI_MELDING, MelosysEessiMelding.class)).isNotNull();
        assertThat(prosessinstans.getData(ProsessDataKey.AKTØR_ID)).isNotEmpty();
    }
}