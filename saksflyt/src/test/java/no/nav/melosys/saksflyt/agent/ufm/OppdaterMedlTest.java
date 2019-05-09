package no.nav.melosys.saksflyt.agent.ufm;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.integrasjon.medl.KildedokumenttypeMedl;
import no.nav.melosys.integrasjon.medl.MedlFasade;
import no.nav.melosys.saksflyt.felles.OppdaterMedlFelles;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class OppdaterMedlTest {

    @Mock
    private MedlFasade medlFasade;
    @Mock
    private OppdaterMedlFelles oppdaterMedlFelles;

    private OppdaterMedl oppdaterMedl;

    @Before
    public void setUp() {
        oppdaterMedl = new OppdaterMedl(medlFasade, oppdaterMedlFelles);
    }

    @Test
    public void utfør() throws Exception {
        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        when(oppdaterMedlFelles.hentLovvalgsperiode(any(Behandling.class))).thenReturn(lovvalgsperiode);

        Behandling behandling = new Behandling();
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);

        oppdaterMedl.utfør(prosessinstans);

        verify(oppdaterMedlFelles).hentLovvalgsperiode(eq(behandling));
        verify(medlFasade).oppdaterPeriodeEndelig(eq(lovvalgsperiode), eq(KildedokumenttypeMedl.SED));
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.REG_UNNTAK_AVSLUTT_BEHANDLING);
    }
}