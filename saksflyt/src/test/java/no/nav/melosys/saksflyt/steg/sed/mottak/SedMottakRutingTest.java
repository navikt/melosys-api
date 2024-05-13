package no.nav.melosys.saksflyt.steg.sed.mottak;

import java.util.Collections;
import java.util.Optional;

import no.nav.melosys.domain.arkiv.Journalpost;
import no.nav.melosys.domain.eessi.SedType;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.saksflytapi.domain.ProsessDataKey;
import no.nav.melosys.saksflytapi.domain.Prosessinstans;
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
    @Mock
    private JoarkFasade joarkFasade;

    private SedMottakRuting sedMottakRuting;

    private final long arkivsakID = 11L;
    private final Journalpost journalpost = new Journalpost("123");

    @BeforeEach
    public void setUp() {
        journalpost.setErFerdigstilt(false);
        sedMottakRuting = new SedMottakRuting(Collections.singleton(sedRuterForSedTyper), defaultSedRuter, eessiService, joarkFasade);
        when(joarkFasade.hentJournalpost(journalpost.getJournalpostId())).thenReturn(journalpost);
    }

    @Test
    void utfør_sedTypeA009_sedRuterForSedTypeBlirKalt() {
        when(sedRuterForSedTyper.gjelderSedTyper()).thenReturn(Collections.singleton(SedType.A009));
        when(eessiService.finnSakForRinasaksnummer(anyString())).thenReturn(Optional.of(arkivsakID));

        MelosysEessiMelding melosysEessiMelding = hentMelosysEessiMelding(SedType.A009);
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, melosysEessiMelding);

        sedMottakRuting.utfør(prosessinstans);

        verify(sedRuterForSedTyper).rutSedTilBehandling(prosessinstans, arkivsakID);
        verify(defaultSedRuter, never()).rutSedTilBehandling(any(), any());
    }

    @Test
    void utfør_sedTypeX009_manuellBehandlerBlirKalt() {
        when(sedRuterForSedTyper.gjelderSedTyper()).thenReturn(Collections.singleton(SedType.A009));
        when(eessiService.finnSakForRinasaksnummer(anyString())).thenReturn(Optional.of(arkivsakID));

        MelosysEessiMelding melosysEessiMelding = hentMelosysEessiMelding(SedType.X009);
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, melosysEessiMelding);

        sedMottakRuting.utfør(prosessinstans);

        verify(sedRuterForSedTyper, never()).rutSedTilBehandling(any(), any());
        verify(defaultSedRuter).rutSedTilBehandling(prosessinstans, arkivsakID);
    }

    @Test
    void utfør_journalpostFerdigstilt_behandlerIkkeVidere() {
        MelosysEessiMelding melosysEessiMelding = hentMelosysEessiMelding(SedType.A009);
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, melosysEessiMelding);
        journalpost.setErFerdigstilt(true);

        sedMottakRuting.utfør(prosessinstans);
        verify(sedRuterForSedTyper, never()).rutSedTilBehandling(any(), anyLong());
        verify(defaultSedRuter, never()).rutSedTilBehandling(any(), anyLong());
    }

    private MelosysEessiMelding hentMelosysEessiMelding(SedType sedType) {
        MelosysEessiMelding melosysEessiMelding = new MelosysEessiMelding();
        melosysEessiMelding.setSedType(sedType.name());
        melosysEessiMelding.setRinaSaksnummer("57483697");
        melosysEessiMelding.setJournalpostId(journalpost.getJournalpostId());
        return melosysEessiMelding;
    }
}
