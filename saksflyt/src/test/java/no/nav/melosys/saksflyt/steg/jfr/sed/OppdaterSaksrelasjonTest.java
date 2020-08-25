package no.nav.melosys.saksflyt.steg.jfr.sed;

import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.ProsessType;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
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

    private final String rinaSaksnummer = "rina123";
    private final Long gsakSaksnummer = 123L;
    private final String bucType = "LA_BUC_04";

    private Prosessinstans prosessinstans;

    @Before
    public void setup() {
        oppdaterSaksrelasjon = new OppdaterSaksrelasjon(eessiService);

        MelosysEessiMelding melosysEessiMelding = new MelosysEessiMelding();
        melosysEessiMelding.setRinaSaksnummer(rinaSaksnummer);
        melosysEessiMelding.setBucType(bucType);

        prosessinstans = new Prosessinstans();
        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, melosysEessiMelding);
        prosessinstans.setData(ProsessDataKey.GSAK_SAK_ID, gsakSaksnummer);
    }

    @Test
    public void utfør() throws MelosysException {
        oppdaterSaksrelasjon.utfør(prosessinstans);

        verify(eessiService).lagreSaksrelasjon(
            eq(gsakSaksnummer),
            eq(rinaSaksnummer),
            eq(bucType)
        );
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.SED_MOTTAK_FERDIGSTILL_JOURNALPOST);
    }

    @Test
    public void utfør_prosessTypeOpprettNySak_forventSteg() throws MelosysException {
        prosessinstans.setType(ProsessType.OPPRETT_NY_SAK);

        oppdaterSaksrelasjon.utfør(prosessinstans);

        verify(eessiService).lagreSaksrelasjon(
            eq(gsakSaksnummer),
            eq(rinaSaksnummer),
            eq(bucType)
        );
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.SED_GENERELL_SAK_HENT_PERSON);
    }
}