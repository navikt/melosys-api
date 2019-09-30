package no.nav.melosys.saksflyt.steg.aou.inn.svar;

import no.nav.melosys.domain.*;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.medl.KildedokumenttypeMedl;
import no.nav.melosys.integrasjon.medl.MedlFasade;
import no.nav.melosys.integrasjon.medl.StatusaarsakMedl;
import no.nav.melosys.saksflyt.felles.OppdaterMedlFelles;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class OppdaterMedlTest {

    @Mock
    private OppdaterMedlFelles oppdaterMedlFelles;
    @Mock
    private MedlFasade medlFasade;

    private OppdaterMedl oppdaterMedl;

    @Before
    public void setup() {
        oppdaterMedl = new OppdaterMedl(oppdaterMedlFelles, medlFasade);
    }

    @Test
    public void utfør_lovvalgsperiodeInnvilget() throws FunksjonellException, TekniskException {
        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setInnvilgelsesresultat(InnvilgelsesResultat.INNVILGET);
        when(oppdaterMedlFelles.hentLovvalgsperiode(any(Behandling.class))).thenReturn(lovvalgsperiode);

        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(new Behandling());
        oppdaterMedl.utfør(prosessinstans);

        verify(oppdaterMedlFelles).hentLovvalgsperiode(any(Behandling.class));
        verify(medlFasade).oppdaterPeriodeEndelig(any(Lovvalgsperiode.class), eq(KildedokumenttypeMedl.SED));
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.AOU_MOTTAK_SVAR_SAK_OG_BEHANDLING_AVSLUTTET);
    }

    @Test
    public void utfør_lovvalgsperiodeAvvist() throws FunksjonellException, TekniskException {
        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setInnvilgelsesresultat(InnvilgelsesResultat.AVSLAATT);
        lovvalgsperiode.setMedlPeriodeID(1L);
        when(oppdaterMedlFelles.hentLovvalgsperiode(any(Behandling.class))).thenReturn(lovvalgsperiode);

        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(new Behandling());
        oppdaterMedl.utfør(prosessinstans);

        verify(oppdaterMedlFelles).hentLovvalgsperiode(any(Behandling.class));
        verify(medlFasade).avvisPeriode(anyLong(), eq(StatusaarsakMedl.AVVIST));
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.AOU_MOTTAK_SVAR_SAK_OG_BEHANDLING_AVSLUTTET);
    }
}