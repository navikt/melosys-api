package no.nav.melosys.service.kodeverk;

import java.time.LocalDate;
import java.util.*;

import no.nav.melosys.integrasjon.kodeverk.Kode;
import no.nav.melosys.integrasjon.kodeverk.KodeOppslag;
import no.nav.melosys.integrasjon.kodeverk.Kodeverk;
import no.nav.melosys.integrasjon.kodeverk.KodeverkRegister;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static no.nav.melosys.domain.FellesKodeverk.LANDKODER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KodeverkServiceTest {

    private static String BAK = "BAK", BAKVENDTLAND = "BAKVENDTLAND";
    private static String OPP = "OPP", OPPNEDLAND = "OPPNEDLAND";
    private Map<String, List<Kode>> landkoder = new HashMap<>();
    private KodeverkService kodeverkService;

    @Mock
    private KodeverkRegister kodeverkRegisterMock;
    @Mock
    private KodeOppslag kodeOppslagMock;

    @BeforeEach
    public void setup() {
        kodeverkService = new KodeverkService(kodeverkRegisterMock, kodeOppslagMock);
        landkoder.put(BAK, Collections.singletonList(new Kode(BAK, BAKVENDTLAND, LocalDate.MIN, LocalDate.MAX)));
        Kodeverk mockLandkoder = new Kodeverk(LANDKODER.getNavn(), landkoder);
        Mockito.when(kodeverkRegisterMock.hentKodeverk(LANDKODER.getNavn())).thenReturn(mockLandkoder);
    }

    @Test
    void dekodOgCache_altOK() {
        when(kodeOppslagMock.getTermFraKodeverk(eq(LANDKODER), eq(BAK), any(), any())).thenReturn(BAKVENDTLAND);
        // Sjekk opphenting av kodeverk...
        String res = kodeverkService.dekod(LANDKODER, BAK);
        assertThat(res).isEqualTo(BAKVENDTLAND);
        verify(kodeverkRegisterMock, atMost(1)).hentKodeverk(any());
    }

    @Test
    void hentGyldigeKoderForKodeverk_altOK() {
        LocalDate idag = LocalDate.now();
        landkoder.put(OPP, Arrays.asList(new Kode(OPP, BAKVENDTLAND, LocalDate.MIN, idag.minusDays(1)), new Kode(OPP, OPPNEDLAND, idag, LocalDate.MAX)));

        List<KodeDto> res = kodeverkService.hentGyldigeKoderForKodeverk(LANDKODER);

        assertThat(res).hasSize(2);
        assertThat(res.get(0).getKode()).isEqualTo(BAK);
        assertThat(res.get(1).getKode()).isEqualTo(OPP);
        assertThat(res.get(1).getTerm()).isEqualTo(OPPNEDLAND);
    }

    @Test
    void hentGyldigeKoderForKodeverk_ingenGyldigeTermerForOpp_kommerIkkeMedIListen() {
        LocalDate idag = LocalDate.now();
        landkoder.put(OPP, Arrays.asList(new Kode(OPP, BAKVENDTLAND, LocalDate.MIN, idag.minusDays(1)), new Kode(OPP, OPPNEDLAND, idag.plusDays(1), LocalDate.MAX)));

        List<KodeDto> res = kodeverkService.hentGyldigeKoderForKodeverk(LANDKODER);

        assertThat(res).hasSize(1).extracting(KodeDto::getKode).containsExactly(BAK);
    }
}
