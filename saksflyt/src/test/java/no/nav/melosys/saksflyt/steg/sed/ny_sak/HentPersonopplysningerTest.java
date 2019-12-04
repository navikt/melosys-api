package no.nav.melosys.saksflyt.steg.sed.ny_sak;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.saksflyt.felles.HentOpplysningerFelles;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class HentPersonopplysningerTest {

    private HentPersonopplysninger hentPersonopplysninger;

    @Mock
    private HentOpplysningerFelles felles;

    @Before
    public void setup() {
        hentPersonopplysninger = new HentPersonopplysninger(felles);
    }

    @Test
    public void utfør() throws MelosysException {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setData(ProsessDataKey.AKTØR_ID, "123");
        prosessinstans.setBehandling(new Behandling());
        hentPersonopplysninger.utfør(prosessinstans);

        verify(felles).hentOgLagrePersonopplysninger(eq("123"), any(Behandling.class));
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.SED_GENERELL_SAK_OPPRETT_OPPGAVE);
    }
}