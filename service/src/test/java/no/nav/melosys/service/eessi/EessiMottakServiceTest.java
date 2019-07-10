package no.nav.melosys.service.eessi;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;

import no.nav.melosys.domain.ProsessDataKey;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.service.kafka.model.MelosysEessiMelding;
import no.nav.melosys.service.kafka.model.Periode;
import no.nav.melosys.service.kafka.model.Statsborgerskap;
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

    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

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
    public void behandleMottatMelding() {
        MelosysEessiMelding eessiMelding = hentMelosysEessiMelding(LocalDate.now(), LocalDate.now().plusYears(1));
        eessiMottakService.behandleMottattMelding(eessiMelding);

        verify(prosessinstansService).lagre(captor.capture());

        Prosessinstans prosessinstans = captor.getValue();
        assertThat(prosessinstans).isNotNull();
        assertThat(prosessinstans.getData()).isNotEmpty();

        assertThat(prosessinstans.getData(ProsessDataKey.AKTØR_ID)).isNotEmpty();
        assertThat(prosessinstans.getData(ProsessDataKey.JOURNALPOST_ID)).isNotEmpty();
        assertThat(prosessinstans.getData(ProsessDataKey.GSAK_SAK_ID)).isNotEmpty();
        assertThat(prosessinstans.getData(ProsessDataKey.EESSI_MELDING)).isNotEmpty();
    }

    private MelosysEessiMelding hentMelosysEessiMelding(LocalDate fom, LocalDate tom) {
        MelosysEessiMelding melding = new MelosysEessiMelding();
        melding.setAktoerId("123");
        melding.setArtikkel("12_1");
        melding.setDokumentId("123321");
        melding.setGsakSaksnummer(432432L);
        melding.setJournalpostId("j123");
        melding.setLovvalgsland("SE");

        Periode periode = new Periode();
        periode.setFom(fom);
        periode.setTom(tom);
        melding.setPeriode(periode);

        Statsborgerskap statsborgerskap = new Statsborgerskap();
        statsborgerskap.setLandkode("SE");

        melding.setRinaSaksnummer("r123");
        melding.setSedId("s123");
        melding.setStatsborgerskap(
            Collections.singletonList(statsborgerskap));
        melding.setSedType("A009");
        melding.setBucType("LA_BUC_04");
        return melding;
    }
}