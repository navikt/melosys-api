package no.nav.melosys.saksflyt.steg.afl;

import java.time.LocalDate;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.domain.eessi.melding.Periode;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.service.registeropplysninger.RegisteropplysningerFactory;
import no.nav.melosys.service.registeropplysninger.RegisteropplysningerRequest;
import no.nav.melosys.service.registeropplysninger.RegisteropplysningerService;
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
public class HentRegisteropplysningerTest {

    private HentRegisteropplysninger hentRegisteropplysninger;

    @Mock
    private RegisteropplysningerService registeropplysningerService;

    @Captor
    private ArgumentCaptor<RegisteropplysningerRequest> captor;

    @Before
    public void setup() {
        hentRegisteropplysninger = new HentRegisteropplysninger(new RegisteropplysningerFactory(), registeropplysningerService);
    }

    @Test
    public void utfør() throws MelosysException {
        final String fnr = "3333333";

        MelosysEessiMelding melosysEessiMelding = new MelosysEessiMelding();
        melosysEessiMelding.setPeriode(new Periode());
        melosysEessiMelding.getPeriode().setFom(LocalDate.now());
        melosysEessiMelding.getPeriode().setTom(LocalDate.now().plusYears(1));

        Behandling behandling = new Behandling();
        behandling.setId(1L);
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);
        prosessinstans.setData(ProsessDataKey.BRUKER_ID, fnr);
        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, melosysEessiMelding);

        hentRegisteropplysninger.utfør(prosessinstans);

        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.AFL_OPPRETT_BEHANDLINGSGRUNNLAG);

        verify(registeropplysningerService).hentOgLagreOpplysninger(captor.capture());

        RegisteropplysningerRequest registeropplysningerRequest = captor.getValue();
        assertThat(registeropplysningerRequest.getFnr()).isEqualTo(fnr);
        assertThat(registeropplysningerRequest.getFom()).isEqualTo(LocalDate.now());
        assertThat(registeropplysningerRequest.getTom()).isEqualTo(LocalDate.now().plusYears(1));
        assertThat(registeropplysningerRequest.getOpplysningstyper()).contains(
            SaksopplysningType.PERSOPL,
            SaksopplysningType.PERSHIST,
            SaksopplysningType.MEDL,
            SaksopplysningType.INNTK,
            SaksopplysningType.UTBETAL,
            SaksopplysningType.ARBFORH,
            SaksopplysningType.SOB_SAK,
            SaksopplysningType.ORG
        );
    }
}