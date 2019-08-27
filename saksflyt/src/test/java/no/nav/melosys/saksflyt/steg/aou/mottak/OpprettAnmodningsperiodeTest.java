package no.nav.melosys.saksflyt.steg.aou.mottak;

import java.time.LocalDate;
import java.util.Collections;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.ProsessDataKey;
import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.domain.eessi.melding.AnmodningUnntak;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.domain.eessi.melding.Periode;
import no.nav.melosys.domain.eessi.melding.Statsborgerskap;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.unntak.AnmodningsperiodeService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class OpprettAnmodningsperiodeTest {

    @Mock
    private AnmodningsperiodeService anmodningsperiodeService;
    @InjectMocks
    private OpprettAnmodningsperiode opprettAnmodningsperiode;

    @Test
    public void utfør() throws FunksjonellException, TekniskException {
        Prosessinstans prosessinstans = new Prosessinstans();
        Behandling behandling = new Behandling();
        behandling.setId(1L);
        prosessinstans.setBehandling(behandling);
        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, hentMelosysEessiMelding());

        opprettAnmodningsperiode.utfør(prosessinstans);

        verify(anmodningsperiodeService).lagreAnmodningsperioder(eq(1L), any());
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.AOU_MOTTAK_OPPRETT_PERIODE_MEDL);
    }

    private MelosysEessiMelding hentMelosysEessiMelding() {
        MelosysEessiMelding melding = new MelosysEessiMelding();
        melding.setAktoerId("123");
        melding.setArtikkel("12_1");
        melding.setDokumentId("123321");
        melding.setGsakSaksnummer(432432L);
        melding.setJournalpostId("j123");
        melding.setLovvalgsland("SE");

        Periode periode = new Periode();
        periode.setFom(LocalDate.of(2012, 12, 12));
        periode.setTom(LocalDate.of(2012, 12, 13));
        melding.setPeriode(periode);

        Statsborgerskap statsborgerskap = new Statsborgerskap();
        statsborgerskap.setLandkode("SE");

        melding.setRinaSaksnummer("r123");
        melding.setSedId("s123");
        melding.setStatsborgerskap(
            Collections.singletonList(statsborgerskap));
        melding.setSedType("A009");
        melding.setBucType("LA_BUC_04");

        AnmodningUnntak anmodningUnntak = new AnmodningUnntak();
        anmodningUnntak.setUnntakFraLovvalgsland("NO");
        anmodningUnntak.setUnntakFraLovvalgsbestemmelse("16_1");
        melding.setAnmodningUnntak(anmodningUnntak);

        return melding;
    }
}