package no.nav.melosys.saksflyt.agent.iv;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.ProsessDataKey;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.domain.kodeverk.Endretperioder;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import org.junit.Before;
import org.junit.Test;

import static no.nav.melosys.domain.ProsessSteg.IV_OPPDATER_RESULTAT;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class OppdaterAvklarteFaktaEndretPeriodeBegrunnelseTest {

    private OppdaterAvklarteFaktaEndretPeriodeBegrunnelse oppdaterAvklarteFaktaEndretPeriodeBegrunnelse;
    private AvklartefaktaService avklartefaktaService;

    @Before
    public void setUp() {
        avklartefaktaService = mock(AvklartefaktaService.class);
        oppdaterAvklarteFaktaEndretPeriodeBegrunnelse = new OppdaterAvklarteFaktaEndretPeriodeBegrunnelse(avklartefaktaService);
    }

    @Test
    public void utfør() throws FunksjonellException, TekniskException {
        long behandlingId = 34L;
        Endretperioder endretperiodeKode = Endretperioder.ARBEIDSFORHOLD_AVSLUTTET;

        Prosessinstans p = new Prosessinstans();
        Behandling behandling = new Behandling();
        behandling.setId(behandlingId);
        p.setBehandling(behandling);
        p.setData(ProsessDataKey.BEGRUNNELSEKODE, endretperiodeKode);

        oppdaterAvklarteFaktaEndretPeriodeBegrunnelse.utfør(p);

        verify(avklartefaktaService).leggTilEndretPeriodeAvklarteFakta(behandlingId, endretperiodeKode);
        assertThat(p.getSteg()).isEqualTo(IV_OPPDATER_RESULTAT);
    }
}