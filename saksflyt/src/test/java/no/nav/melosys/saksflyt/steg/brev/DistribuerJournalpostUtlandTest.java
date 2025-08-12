package no.nav.melosys.saksflyt.steg.brev;

import no.nav.melosys.domain.BehandlingTestFactory;
import no.nav.melosys.domain.adresse.StrukturertAdresse;
import no.nav.melosys.domain.arkiv.Distribusjonstype;
import no.nav.melosys.domain.kodeverk.Land_iso2;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.integrasjon.doksys.DoksysFasade;
import no.nav.melosys.saksflytapi.domain.*;
import no.nav.melosys.service.aktoer.UtenlandskMyndighetService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static no.nav.melosys.saksflyt.SaksflytTestUtils.lagUtenlandskMyndighet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DistribuerJournalpostUtlandTest {

    @Mock
    private DoksysFasade doksysFasade;
    @Mock
    private UtenlandskMyndighetService utenlandskMyndighetService;
    private DistribuerJournalpostUtland distribuerJournalpostUtland;

    @BeforeEach
    public void settOpp() {
        distribuerJournalpostUtland = new DistribuerJournalpostUtland(doksysFasade, utenlandskMyndighetService);
    }

    @Test
    void utfør_distribuerbarJournalpostOgMottakerSatt_distribuererJournalpost() {
        Prosessinstans prosessinstans = ProsessinstansTestFactory.builderWithDefaults()
            .medType(ProsessType.OPPRETT_SAK)
            .medStatus(ProsessStatus.KLAR)
            .medData(ProsessDataKey.DISTRIBUERBAR_JOURNALPOST_ID, "12345")
            .medData(ProsessDataKey.DISTRIBUER_MOTTAKER_LAND, Landkoder.SE)
            .medData(ProsessDataKey.DISTRIBUSJONSTYPE, Distribusjonstype.VEDTAK)
            .medBehandling(BehandlingTestFactory.builderWithDefaults().build())
            .build();
        when(utenlandskMyndighetService.hentUtenlandskMyndighet(eq(Land_iso2.SE))).thenReturn(lagUtenlandskMyndighet());

        distribuerJournalpostUtland.utfør(prosessinstans);

        ArgumentCaptor<StrukturertAdresse> captor = ArgumentCaptor.forClass(StrukturertAdresse.class);
        verify(doksysFasade).distribuerJournalpost(eq("12345"), captor.capture(), eq(Distribusjonstype.VEDTAK));

        StrukturertAdresse strukturertAdresse = captor.getValue();
        assertThat(strukturertAdresse).isNotNull();
        assertThat(strukturertAdresse.getGatenavn()).isEqualTo("Svenskegatan 38");
        assertThat(strukturertAdresse.getPostnummer()).isEqualTo("8080");
        assertThat(strukturertAdresse.getLandkode()).isEqualTo(Landkoder.SE.getKode());
    }

    @Test
    void utfør_distribuerJournalpostSattMottakerIkkeSatt_kasterFeil() {
        Prosessinstans prosessinstans = ProsessinstansTestFactory.builderWithDefaults()
            .medType(ProsessType.OPPRETT_SAK)
            .medStatus(ProsessStatus.KLAR)
            .medData(ProsessDataKey.DISTRIBUERBAR_JOURNALPOST_ID, "123")
            .build();
        assertThatExceptionOfType(IkkeFunnetException.class)
            .isThrownBy(() -> distribuerJournalpostUtland.utfør(prosessinstans))
            .withMessageContaining("mottakerland ikke er satt");
    }

    @Test
    void utfør_distribuerJournalpostIkkeSatt_distribuererIkkeJournalpost() {
        distribuerJournalpostUtland.utfør(ProsessinstansTestFactory.builderWithDefaults().build());
        verify(doksysFasade, never()).distribuerJournalpost(any(), any());
    }
}
