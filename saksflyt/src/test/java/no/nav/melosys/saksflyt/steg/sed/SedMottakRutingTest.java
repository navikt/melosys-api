package no.nav.melosys.saksflyt.steg.sed;

import java.util.Collections;

import no.nav.melosys.domain.ProsessDataKey;
import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.eessi.BehandleMottattSedInitialiserer;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class SedMottakRutingTest {

    @Mock
    private BehandleMottattSedInitialiserer behandleMottattSedInitialiserer;

    private SedMottakRuting sedMottakRuting;

    @Before
    public void setUp() {
        sedMottakRuting = new SedMottakRuting(Collections.singleton(behandleMottattSedInitialiserer));
        when(behandleMottattSedInitialiserer.gjelderSedType(any())).thenReturn(true);
    }

    @Test
    public void utfør_prosessStegBlirOppdatert_ingenFeil() throws Exception {
        doAnswer(invocationOnMock -> {
            Prosessinstans prosessinstans = invocationOnMock.getArgument(0);
            prosessinstans.setSteg(ProsessSteg.REG_UNNTAK_OPPRETT_SAK_OG_BEH);
            return null;
        }).when(behandleMottattSedInitialiserer).initialiserProsessinstans(any(Prosessinstans.class));

        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setSteg(sedMottakRuting.inngangsSteg());
        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, hentMelosysEessiMelding());

        sedMottakRuting.utfør(prosessinstans);

        verify(behandleMottattSedInitialiserer).initialiserProsessinstans(any());
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.REG_UNNTAK_OPPRETT_SAK_OG_BEH);
    }

    @Test(expected = TekniskException.class)
    public void utfør_prosessStegBlirIkkeOppdatert_kasterException() throws Exception {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setSteg(sedMottakRuting.inngangsSteg());
        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, hentMelosysEessiMelding());

        sedMottakRuting.utfør(prosessinstans);
    }

    private MelosysEessiMelding hentMelosysEessiMelding() {
        MelosysEessiMelding melosysEessiMelding = new MelosysEessiMelding();
        melosysEessiMelding.setSedType("A009");
        return melosysEessiMelding;
    }
}