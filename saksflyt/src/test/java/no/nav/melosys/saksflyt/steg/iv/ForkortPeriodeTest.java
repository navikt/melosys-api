package no.nav.melosys.saksflyt.steg.iv;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.domain.kodeverk.Avklartefaktatyper;
import no.nav.melosys.domain.kodeverk.begrunnelser.Endretperiode;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import org.junit.Before;
import org.junit.Test;

import static no.nav.melosys.domain.saksflyt.ProsessSteg.IV_VALIDERING;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class ForkortPeriodeTest {

    private ForkortPeriode forkortPeriode;
    private AvklartefaktaService avklartefaktaService;

    @Before
    public void setUp() {
        avklartefaktaService = mock(AvklartefaktaService.class);
        forkortPeriode = new ForkortPeriode(avklartefaktaService);
    }

    @Test
    public void utfør() throws FunksjonellException, TekniskException {
        long behandlingId = 34L;
        Endretperiode endretperiodeKode = Endretperiode.ARBEIDSFORHOLD_AVSLUTTET;

        Prosessinstans p = new Prosessinstans();
        Behandling behandling = new Behandling();
        behandling.setId(behandlingId);
        p.setBehandling(behandling);
        p.setData(ProsessDataKey.BEGRUNNELSEKODE, endretperiodeKode);

        forkortPeriode.utfør(p);

        verify(avklartefaktaService).leggTilBegrunnelse(behandlingId, Avklartefaktatyper.AARSAK_ENDRING_PERIODE, endretperiodeKode.getKode());
        assertThat(p.getSteg()).isEqualTo(IV_VALIDERING);
    }
}