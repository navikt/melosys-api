package no.nav.melosys.saksflyt.steg.sed.sak;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.service.dokument.sed.EessiService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class OppdaterSaksrelasjonTest {

    @Mock
    private EessiService eessiService;

    private OppdaterSaksrelasjon oppdaterSaksrelasjon;

    @Before
    public void setup() {
        oppdaterSaksrelasjon = new OppdaterSaksrelasjon(eessiService);
    }

    @Test
    public void utfør() throws MelosysException {
        final String rinaSaksnummer = "rina123";
        final Long gsakSaksnummer = 123L;
        final String bucType = "LA_BUC_04";

        MelosysEessiMelding melosysEessiMelding = new MelosysEessiMelding();
        melosysEessiMelding.setRinaSaksnummer(rinaSaksnummer);
        melosysEessiMelding.setBucType(bucType);

        Fagsak fagsak = new Fagsak();
        fagsak.setGsakSaksnummer(gsakSaksnummer);
        Behandling behandling = new Behandling();
        behandling.setFagsak(fagsak);

        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, melosysEessiMelding);
        prosessinstans.setBehandling(behandling);

        oppdaterSaksrelasjon.utfør(prosessinstans);

        verify(eessiService).lagreSaksrelasjon(
            eq(gsakSaksnummer),
            eq(rinaSaksnummer),
            eq(bucType)
        );
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.SED_MOTTAK_FERDIGSTILL_JOURNALPOST);
    }
}