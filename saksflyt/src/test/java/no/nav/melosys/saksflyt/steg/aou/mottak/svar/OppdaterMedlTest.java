package no.nav.melosys.saksflyt.steg.aou.mottak.svar;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.medl.KildedokumenttypeMedl;
import no.nav.melosys.integrasjon.medl.MedlFasade;
import no.nav.melosys.saksflyt.felles.OppdaterMedlFelles;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
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
    private OppdaterMedlFelles oppdaterMedlFelles;
    @Mock
    private MedlFasade medlFasade;
    @InjectMocks
    private OppdaterMedl oppdaterMedl;

    @Test
    public void utfør() throws FunksjonellException, TekniskException {
        when(oppdaterMedlFelles.hentLovvalgsperiode(any(Behandling.class))).thenReturn(new Lovvalgsperiode());

        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(new Behandling());
        oppdaterMedl.utfør(prosessinstans);

        verify(oppdaterMedlFelles).hentLovvalgsperiode(any(Behandling.class));
        verify(medlFasade).oppdaterPeriodeEndelig(any(Lovvalgsperiode.class), eq(KildedokumenttypeMedl.SED));
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.AOU_MOTTAK_SVAR_AVSLUTT_BEHANDLING);
    }
}