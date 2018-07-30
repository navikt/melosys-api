package no.nav.melosys.service.aktoer;

import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import static java.lang.Thread.sleep;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AktoerIdServiceTest {

    private AktoerIdService aktoerIdService;

    @Mock
    TpsFasade tpsFasade;

    private final String FNR_MOCK = "01018000000";
    private final String AKTØRID_MOCK = "1000100000000";

    @Before
    public void setUp() throws IkkeFunnetException {
        aktoerIdService = new AktoerIdService(tpsFasade);
        when(tpsFasade.hentAktørIdForIdent(FNR_MOCK)).thenReturn(AKTØRID_MOCK);
        aktoerIdService.onApplicationEvent(null);

        ReflectionTestUtils.setField(aktoerIdService, "millisMellomVåkneOpp", 1000);
        ReflectionTestUtils.setField(aktoerIdService, "millisLevetidICache", 1000);
    }

    @Test
    public void testAktørIdServiceUtenTømmingAvCache() throws IkkeFunnetException, InterruptedException {
        String aktørId = aktoerIdService.hentAktørIdForIdent(FNR_MOCK);
        assertEquals(AKTØRID_MOCK, aktørId);

        aktørId = aktoerIdService.hentAktørIdForIdent(FNR_MOCK);
        assertEquals(AKTØRID_MOCK, aktørId);

        verify(tpsFasade, times(1)).hentAktørIdForIdent(FNR_MOCK);
    }

    @Test
    public void testAktørIdServiceMedTømmingAvCache() throws IkkeFunnetException, InterruptedException {
        String aktørId = aktoerIdService.hentAktørIdForIdent(FNR_MOCK);
        assertEquals(AKTØRID_MOCK, aktørId);

        sleep(1500);

        aktørId = aktoerIdService.hentAktørIdForIdent(FNR_MOCK);
        assertEquals(AKTØRID_MOCK, aktørId);

        verify(tpsFasade, times(2)).hentAktørIdForIdent(FNR_MOCK);
    }
}
