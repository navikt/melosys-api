package no.nav.melosys.service.unntaksperiode;

import java.util.HashSet;
import java.util.Set;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.kodeverk.IkkeGodkjentBegrunnelser;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.service.saksflyt.ProsessinstansService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class UnntaksperiodeServiceTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Mock
    private ProsessinstansService prosessinstansService;

    private UnntaksperiodeService unntaksperiodeService;

    @Before
    public void setUp() throws Exception {
        unntaksperiodeService = new UnntaksperiodeService(prosessinstansService);
    }

    @Test
    public void ikkeGodkjennPeriode_medBegrunnelser_ingenFeil() throws Exception {
        Set<String> begrunnelser = new HashSet<>();
        begrunnelser.add(IkkeGodkjentBegrunnelser.TREDJELANDSBORGER_IKKE_AVTALELAND.getKode());
        unntaksperiodeService.ikkeGodkjennPeriode(new Behandling(), begrunnelser, null);
        verify(prosessinstansService).opprettProsessinstansUnntaksperiodeAvvist(any(), anySet(), any());
    }

    @Test
    public void ikkeGodkjennPeriode_ingenBegrunnelser_forventException() throws Exception {
        expectedException.expect(FunksjonellException.class);
        expectedException.expectMessage("Ingen begrunnelser for avlag av periode");
        unntaksperiodeService.ikkeGodkjennPeriode(new Behandling(), new HashSet<>(), null);
    }

    @Test
    public void ikkeGodkjennPeriode_begrunnelseAnnetIngenFritekst_forventException() throws Exception {
        Set<String> begrunnelser = new HashSet<>();
        begrunnelser.add(IkkeGodkjentBegrunnelser.TREDJELANDSBORGER_IKKE_AVTALELAND.getKode());
        begrunnelser.add(IkkeGodkjentBegrunnelser.ANNET.getKode());

        expectedException.expect(FunksjonellException.class);
        expectedException.expectMessage("Begrunnelse " + IkkeGodkjentBegrunnelser.ANNET + " krever fritekst!");
        unntaksperiodeService.ikkeGodkjennPeriode(new Behandling(), begrunnelser, null);
    }
}