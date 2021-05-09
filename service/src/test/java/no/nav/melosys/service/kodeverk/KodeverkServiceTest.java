package no.nav.melosys.service.kodeverk;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import no.nav.melosys.integrasjon.kodeverk.Kode;
import no.nav.melosys.integrasjon.kodeverk.Kodeverk;
import no.nav.melosys.integrasjon.kodeverk.KodeverkRegister;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static no.nav.melosys.domain.FellesKodeverk.LANDKODER;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class KodeverkServiceTest {

    private static String BAK = "BAK", BAKVENDTLAND = "BAKVENDTLAND";
    private static String OPP = "OPP", OPPNEDLAND = "OPPNEDLAND";
    private Map<String, List<Kode>> landkoder = new HashMap<>();
    private KodeverkService kodeverkService;

    @Mock
    private KodeverkRegister kodeverkRegisterMock;

    @BeforeEach
    public void setup() {
        kodeverkService = new KodeverkService(kodeverkRegisterMock);
        landkoder.put(BAK, Arrays.asList(new Kode(BAK, BAKVENDTLAND, LocalDate.MIN, LocalDate.MAX)));
        Kodeverk mockLandkoder = new Kodeverk(LANDKODER.getNavn(), landkoder);
        Mockito.when(kodeverkRegisterMock.hentKodeverk(LANDKODER.getNavn())).thenReturn(mockLandkoder);
    }

    @Test
    public void dekodOgCache_altOK() {
        // Sjekk opphenting av kodeverk...
        String res = kodeverkService.dekod(LANDKODER, BAK, LocalDate.now());
        assertEquals(BAKVENDTLAND, res);
        verify(kodeverkRegisterMock, atMost(1)).hentKodeverk(any());
        // Sjekk opphenting fra cache...
        String cacheRes = kodeverkService.dekod(LANDKODER, BAK, LocalDate.now());
        verify(kodeverkRegisterMock, atMost(1)).hentKodeverk(any());
        assertEquals(res, cacheRes);
    }

    @Test
    public void hentGyldigeKoderForKodeverk_altOK() {
        LocalDate idag = LocalDate.now();
        landkoder.put(OPP, Arrays.asList(new Kode(OPP, BAKVENDTLAND, LocalDate.MIN, idag.minusDays(1)), new Kode(OPP, OPPNEDLAND, idag, LocalDate.MAX)));

        List<KodeDto> res = kodeverkService.hentGyldigeKoderForKodeverk(LANDKODER);

        assertEquals(2, res.size());
        assertEquals(BAK, res.get(0).getKode());
        assertEquals(OPP, res.get(1).getKode());
        assertEquals(OPPNEDLAND, res.get(1).getTerm());
    }

    @Test
    public void hentGyldigeKoderForKodeverk_ingenGyldigeTermerForOpp_kommerIkkeMedIListen() {
        LocalDate idag = LocalDate.now();
        landkoder.put(OPP, Arrays.asList(new Kode(OPP, BAKVENDTLAND, LocalDate.MIN, idag.minusDays(1)), new Kode(OPP, OPPNEDLAND, idag.plusDays(1), LocalDate.MAX)));

        List<KodeDto> res = kodeverkService.hentGyldigeKoderForKodeverk(LANDKODER);

        assertEquals(1, res.size());
        assertEquals(BAK, res.get(0).getKode());
    }

}
