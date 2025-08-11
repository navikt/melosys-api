package no.nav.melosys.saksflyt.steg.sed;

import java.time.LocalDate;

import no.nav.melosys.domain.Anmodningsperiode;
import no.nav.melosys.domain.AnmodningsperiodeSvar;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.BehandlingTestFactory;
import no.nav.melosys.domain.eessi.Periode;
import no.nav.melosys.domain.eessi.SvarAnmodningUnntak;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.domain.kodeverk.Anmodningsperiodesvartyper;
import no.nav.melosys.saksflytapi.domain.ProsessDataKey;
import no.nav.melosys.saksflytapi.domain.ProsessStatus;
import no.nav.melosys.saksflytapi.domain.ProsessType;
import no.nav.melosys.saksflytapi.domain.Prosessinstans;
import no.nav.melosys.service.unntak.AnmodningsperiodeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class OpprettAnmodningsperiodeSvarTest {

    private OpprettAnmodningsperiodeSvar opprettAnmodningsperiodeSvar;

    @Captor
    private ArgumentCaptor<AnmodningsperiodeSvar> captor;

    @Mock
    private AnmodningsperiodeService anmodningsperiodeService;

    @BeforeEach
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
        Prosessinstans prosessinstans = Prosessinstans.builder()
            .medType(ProsessType.OPPRETT_SAK)
            .medStatus(ProsessStatus.KLAR)
            .medData(ProsessDataKey.EESSI_MELDING, hentMelosysEessiMelding(innvilgelse))
            .build();

        Behandling behandling = BehandlingTestFactory.builderWithDefaults()
            .medId(123L)
            .build();
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
