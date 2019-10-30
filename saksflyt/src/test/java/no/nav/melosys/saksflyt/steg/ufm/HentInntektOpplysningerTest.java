package no.nav.melosys.saksflyt.steg.ufm;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.ProsessDataKey;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.domain.eessi.melding.Periode;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.saksflyt.felles.HentOpplysningerFelles;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class HentInntektOpplysningerTest {

    @Mock
    private HentOpplysningerFelles hentOpplysningerFelles;

    private HentInntektOpplysninger hentInntektOpplysninger;

    @Before
    public void setUp() throws Exception {
        hentInntektOpplysninger = new HentInntektOpplysninger(hentOpplysningerFelles);
    }

    @Test
    public void utførSteg() throws FunksjonellException, TekniskException {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setData(ProsessDataKey.BRUKER_ID, "123123");

        MelosysEessiMelding melosysEessiMelding = new MelosysEessiMelding();
        melosysEessiMelding.setPeriode(new Periode());
        melosysEessiMelding.getPeriode().setFom(LocalDate.now());
        melosysEessiMelding.getPeriode().setTom(LocalDate.now().plusMonths(1));
        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, melosysEessiMelding);

        Behandling behandling = new Behandling();
        behandling.setId(2L);

        prosessinstans.setBehandling(behandling);

        hentInntektOpplysninger.utfør(prosessinstans);

        verify(hentOpplysningerFelles).hentOgLagreInntektsopplysninger(eq(2L), eq("123123"));
    }
}