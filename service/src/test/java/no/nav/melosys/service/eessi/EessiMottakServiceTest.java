package no.nav.melosys.service.eessi;

import java.time.LocalDate;
import java.util.Collections;

import no.nav.melosys.domain.ProsessDataKey;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.domain.kodeverk.LovvalgsBestemmelser_883_2004;
import no.nav.melosys.eessi.avro.MelosysEessiMelding;
import no.nav.melosys.eessi.avro.Periode;
import no.nav.melosys.eessi.avro.Statsborgerskap;
import no.nav.melosys.service.saksflyt.ProsessinstansService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class EessiMottakServiceTest {

    @Captor
    private ArgumentCaptor<Prosessinstans> captor;

    @Mock
    private ProsessinstansService prosessinstansService;

    private EessiMottakService eessiMottakService;

    @Before
    public void setUp() {
        eessiMottakService = new EessiMottakService(prosessinstansService);
    }

    @Test
    public void behandleMottatMelding_sjekkAlleVerdierErSatt() {
        MelosysEessiMelding eessiMelding = hentMelosysEessiMelding();
        eessiMottakService.behandleMottattMelding(eessiMelding);

        verify(prosessinstansService).lagre(captor.capture());

        Prosessinstans prosessinstans = captor.getValue();
        assertThat(prosessinstans).isNotNull();
        assertThat(prosessinstans.getData()).isNotEmpty();

        SedDokument sedDokument = prosessinstans.getData(ProsessDataKey.SED_DOKUMENT, SedDokument.class);
        assertThat(sedDokument).isNotNull();
        assertThat(sedDokument.getLovvalgBestemmelse()).isEqualTo(LovvalgsBestemmelser_883_2004.FO_883_2004_ART12_1);
        assertThat(sedDokument.getPeriode()).isNotNull();
        assertThat(sedDokument.getPeriode().getFom()).isBeforeOrEqualTo(LocalDate.of(2020, 12, 12));
        assertThat(prosessinstans.getData(ProsessDataKey.AKTØR_ID)).isNotNull();
        assertThat(prosessinstans.getData(ProsessDataKey.JOURNALPOST_ID)).isNotNull();
        assertThat(prosessinstans.getData(ProsessDataKey.GSAK_SAK_ID)).isNotNull();
    }

    private MelosysEessiMelding hentMelosysEessiMelding() {
        MelosysEessiMelding melding = new MelosysEessiMelding();
        melding.setAktoerId("123");
        melding.setArtikkel("12_1");
        melding.setDokumentId("123321");
        melding.setErEndring(true);
        melding.setGsakSaksnummer(432432L);
        melding.setJournalpostId("j123");
        melding.setLovvalgsland("SE");
        melding.setPeriode(Periode.newBuilder().setFom("12-12-2020").setTom("12-12-2020").build());
        melding.setRinaSaksnummer("r123");
        melding.setSedId("s123");
        melding.setStatsborgerskap(
            Collections.singletonList(Statsborgerskap.newBuilder().setLandkode("SE").build()));
        return melding;
    }
}