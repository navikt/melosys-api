package no.nav.melosys.service.kodeverk;

import static no.nav.melosys.service.kodeverk.Kodeverk.LANDKODER;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.util.ReflectionTestUtils;

import no.nav.melosys.integrasjon.kodeverk.Kode;
import no.nav.melosys.integrasjon.kodeverk.Kodeverk;
import no.nav.melosys.integrasjon.kodeverk.KodeverkRegister;

@RunWith(SpringJUnit4ClassRunner.class)
public class KodeverkServiceTest {
    
    private static String BAK = "BAK", BAKVENDTLAND = "BAKVENDTLAND";

    private KodeverkService kodeverkService;
    
    @Mock
    private KodeverkRegister kodeverkRegisterMock;

    @Before
    public void setup() {
        kodeverkService = new KodeverkService(kodeverkRegisterMock);
        Map<String, List<Kode>> landkoder = new HashMap<>();
        landkoder.put(BAK, Arrays.asList(new Kode(BAK, BAKVENDTLAND, LocalDate.MIN, LocalDate.MAX)));
        Kodeverk mockLandkoder = new Kodeverk(LANDKODER.getNavn(), landkoder);
        Mockito.when(kodeverkRegisterMock.hentKodeverk(LANDKODER.getNavn())).thenReturn(mockLandkoder);
    }

    @Test
    public void testKodeverkService() {
        // Sjekk opphenting av kodeverk...
        String res = kodeverkService.dekod(LANDKODER, BAK, LocalDate.now());
        assertEquals(BAKVENDTLAND, res);
        // Sjekk opphenting fra cache...
        kodeverkService.dekod(LANDKODER, BAK, LocalDate.now());
        verify(kodeverkRegisterMock, atLeast(1)).hentKodeverk(any());
    }
    
}
