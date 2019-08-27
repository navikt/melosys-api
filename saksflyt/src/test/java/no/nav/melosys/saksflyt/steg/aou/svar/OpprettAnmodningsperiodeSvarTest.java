package no.nav.melosys.saksflyt.steg.aou.svar;

import java.time.LocalDate;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.domain.eessi.melding.Periode;
import no.nav.melosys.domain.eessi.melding.SvarAnmodningUnntak;
import no.nav.melosys.domain.kodeverk.Anmodningsperiodesvartyper;
import no.nav.melosys.service.unntak.AnmodningsperiodeService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class OpprettAnmodningsperiodeSvarTest {

    private OpprettAnmodningsperiodeSvar opprettAnmodningsperiodeSvar;

    @Captor
    private ArgumentCaptor<AnmodningsperiodeSvar> captor;

    @Mock
    private AnmodningsperiodeService anmodningsperiodeService;

    @Before
    public void setup() {
        opprettAnmodningsperiodeSvar = new OpprettAnmodningsperiodeSvar(anmodningsperiodeService);
        Anmodningsperiode anmodningsperiode = new Anmodningsperiode();
        ReflectionTestUtils.setField(anmodningsperiode, "id", 123L);
    }

    @Test
    public void utfør_mottattInnvilgelse_forventSvarMedTypeInnvilgelse() throws Exception {
        Prosessinstans prosessinstans = hentProsessinstans(true);
        opprettAnmodningsperiodeSvar.utfør(prosessinstans);

        verify(anmodningsperiodeService).lagreAnmodningsperiodeSvarForBehandling(anyLong(), captor.capture());

        AnmodningsperiodeSvar anmodningsperiodeSvar = captor.getValue();
        assertThat(anmodningsperiodeSvar.getAnmodningsperiodeSvarType()).isEqualTo(Anmodningsperiodesvartyper.INNVILGELSE);
        assertThat(anmodningsperiodeSvar.getAnmodningsperiode()).isNull();
    }

    @Test
    public void utfør_mottattDelvisInnvilgelse_forventSvarMedTypeInnvilgelse() throws Exception {
        Prosessinstans prosessinstans = hentProsessinstans(false);
        opprettAnmodningsperiodeSvar.utfør(prosessinstans);

        verify(anmodningsperiodeService).lagreAnmodningsperiodeSvarForBehandling(anyLong(), captor.capture());

        AnmodningsperiodeSvar anmodningsperiodeSvar = captor.getValue();
        assertThat(anmodningsperiodeSvar.getAnmodningsperiodeSvarType()).isEqualTo(Anmodningsperiodesvartyper.DELVIS_INNVILGELSE);
        assertThat(anmodningsperiodeSvar.getBegrunnelseFritekst()).isNotNull();
        assertThat(anmodningsperiodeSvar.getInnvilgetFom()).isNotNull();
        assertThat(anmodningsperiodeSvar.getInnvilgetTom()).isNotNull();
    }

    private Prosessinstans hentProsessinstans(boolean innvilgelse) {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setType(ProsessType.ANMODNING_OM_UNNTAK_SVAR);
        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, hentMelosysEessiMelding(innvilgelse));

        Behandling behandling = new Behandling();
        behandling.setId(123L);
        prosessinstans.setBehandling(behandling);
        return prosessinstans;
    }

    private MelosysEessiMelding hentMelosysEessiMelding(boolean innvilgelse) {
        MelosysEessiMelding melosysEessiMelding = new MelosysEessiMelding();
        melosysEessiMelding.setGsakSaksnummer(123L);
        SvarAnmodningUnntak svarAnmodningUnntak = new SvarAnmodningUnntak();
        svarAnmodningUnntak.setBegrunnelse("blabla fritekst");
        if (innvilgelse) {
            svarAnmodningUnntak.setBeslutning(SvarAnmodningUnntak.Beslutning.INNVILGELSE);
        } else {
            svarAnmodningUnntak.setDelvisInnvilgetPeriode(new Periode());
            svarAnmodningUnntak.getDelvisInnvilgetPeriode().setFom(LocalDate.of(2012, 12, 12));
            svarAnmodningUnntak.getDelvisInnvilgetPeriode().setTom(LocalDate.of(2012, 12, 12));
            svarAnmodningUnntak.setBeslutning(SvarAnmodningUnntak.Beslutning.DELVIS_INNVILGELSE);
        }
        melosysEessiMelding.setSvarAnmodningUnntak(svarAnmodningUnntak);

        return melosysEessiMelding;
    }
}