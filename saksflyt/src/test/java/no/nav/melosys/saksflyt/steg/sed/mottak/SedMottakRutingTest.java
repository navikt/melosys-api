package no.nav.melosys.saksflyt.steg.sed.mottak;

import java.util.Collections;
import java.util.Optional;

import no.nav.melosys.domain.eessi.SedType;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.service.dokument.sed.EessiService;
import no.nav.melosys.service.eessi.ruting.DefaultSedRuter;
import no.nav.melosys.service.eessi.ruting.SedRuterForSedTyper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SedMottakRutingTest {

    @Mock
    private SedRuterForSedTyper sedRuterForSedTyper;
    @Mock
    private DefaultSedRuter defaultSedRuter;
    @Mock
    private EessiService eessiService;

    private SedMottakRuting sedMottakRuting;

    private final long arkivsakID = 11L;

    @BeforeEach
    public void setUp() throws MelosysException {
        when(sedRuterForSedTyper.gjelderSedTyper()).thenReturn(Collections.singleton(SedType.A009));
        sedMottakRuting = new SedMottakRuting(Collections.singleton(sedRuterForSedTyper), defaultSedRuter, eessiService);
        when(eessiService.finnSakForRinasaksnummer(anyString())).thenReturn(Optional.of(arkivsakID));
    }

    @Test
    void utfør_sedTypeA009_sedRuterForSedTypeBlirKalt() throws MelosysException {
        MelosysEessiMelding melosysEessiMelding = hentMelosysEessiMelding(SedType.A009);
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, melosysEessiMelding);

        sedMottakRuting.utfør(prosessinstans);

        verify(sedRuterForSedTyper).rutSedTilBehandling(eq(prosessinstans), eq(arkivsakID));
        verify(defaultSedRuter, never()).rutSedTilBehandling(any(), any());
    }

    @Test
    void utfør_sedTypeX009_manuellBehandlerBlirKalt() throws MelosysException {
        MelosysEessiMelding melosysEessiMelding = hentMelosysEessiMelding(SedType.X009);
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, melosysEessiMelding);

        sedMottakRuting.utfør(prosessinstans);

        verify(sedRuterForSedTyper, never()).rutSedTilBehandling(any(), any());
        verify(defaultSedRuter).rutSedTilBehandling(eq(prosessinstans), eq(arkivsakID));
    }

    private MelosysEessiMelding hentMelosysEessiMelding(SedType sedType) {
        MelosysEessiMelding melosysEessiMelding = new MelosysEessiMelding();
        melosysEessiMelding.setSedType(sedType.name());
        melosysEessiMelding.setRinaSaksnummer("57483697");
        return melosysEessiMelding;
    }
}