package no.nav.melosys.saksflyt.steg.sed;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.arkiv.ArkivDokument;
import no.nav.melosys.domain.arkiv.DokumentReferanse;
import no.nav.melosys.domain.arkiv.Journalpost;
import no.nav.melosys.domain.arkiv.Vedlegg;
import no.nav.melosys.domain.eessi.BucType;
import no.nav.melosys.domain.eessi.SedType;
import no.nav.melosys.domain.kodeverk.Land_iso2;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.saksflytapi.domain.ProsessDataKey;
import no.nav.melosys.saksflytapi.domain.ProsessStatus;
import no.nav.melosys.saksflytapi.domain.ProsessType;
import no.nav.melosys.saksflytapi.domain.Prosessinstans;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.dokument.brev.SedSomBrevService;
import no.nav.melosys.service.dokument.sed.EessiService;
import no.nav.melosys.service.sak.FagsakService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VideresendSoknadTest {
    @Mock
    private BehandlingsresultatService behandlingsresultatService;
    @Mock
    private EessiService eessiService;
    @Mock
    private JoarkFasade joarkFasade;
    @Mock
    private FagsakService fagsakService;
    @Mock
    private SedSomBrevService sedSomBrevService;

    private VideresendSoknad videresendSoknad;

    private Behandling behandling;
    private final Journalpost journalpost = new Journalpost("123");

    private static final String MOTTAKER_INSTITUSJON = "SE:123";

    @BeforeEach
    void setup() {
        videresendSoknad = new VideresendSoknad(eessiService, behandlingsresultatService,
            joarkFasade, fagsakService, sedSomBrevService);

        behandling = BehandlingTestFactory.builderWithDefaults()
            .medId(1L)
            .medInitierendeJournalpostId("123")
            .build();
        journalpost.setHoveddokument(new ArkivDokument());
        journalpost.getHoveddokument().setTittel("tittel på deg");
        journalpost.getHoveddokument().setDokumentId("44444");
    }

    @Test
    void utfør_vedleggFinnesIkke_forventFunksjonellException() {
        Prosessinstans prosessinstans = opprettProsessinstans().toBuilder()
            .medBehandling(BehandlingTestFactory.builderWithDefaults()
                .medId(1L)
                .medInitierendeJournalpostId(null)
                .build())
            .build();

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> videresendSoknad.utfør(prosessinstans))
            .withMessageContaining("Kan ikke videresende søknad uten vedlegg");
    }

    @Test
    void utfør_skalSendesUtlandErEessiKlar_senderSedIBuc3() {
        Prosessinstans prosessinstans = opprettProsessinstans();
        prosessinstans = prosessinstans.toBuilder()
            .medData(ProsessDataKey.EESSI_MOTTAKERE, List.of("SE:123"))
            .build();

        Behandling behandling = prosessinstans.getBehandling();
        Long behandlingID = 1L;
        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.setId(behandlingID);
        behandlingsresultat.setBehandling(behandling);

        final byte[] vedlegg = new byte[10];
        final var dokumentReferanse = new DokumentReferanse(behandling.getInitierendeJournalpostId(),
            journalpost.getHoveddokument().getDokumentId());
        prosessinstans = prosessinstans.toBuilder()
            .medData(ProsessDataKey.VEDLEGG_SED, Set.of(dokumentReferanse))
            .build();
        final Vedlegg forventetVedlegg = new Vedlegg(vedlegg, "tittel");
        when(eessiService.lagEessiVedlegg(any(), anyCollection())).thenReturn(Set.of(forventetVedlegg));
        when(behandlingsresultatService.hentBehandlingsresultat(anyLong())).thenReturn(behandlingsresultat);

        videresendSoknad.utfør(prosessinstans);

        verify(eessiService).opprettOgSendSed(eq(behandlingID), eq(List.of(MOTTAKER_INSTITUSJON)), eq(BucType.LA_BUC_03),
            argThat(collection -> collection.contains(forventetVedlegg)), isNull());
        assertThat(prosessinstans.getData(ProsessDataKey.DISTRIBUERBAR_JOURNALPOST_ID)).isNull();
    }

    @Test
    void utfør_skalSendesUtlandErIkkeEessiKlar_senderA008SomBrev() {
        Prosessinstans prosessinstans = opprettProsessinstans();
        UUID prosessinstansUuid = UUID.randomUUID();
        prosessinstans = prosessinstans.toBuilder()
            .medId(prosessinstansUuid)
            .build();
        Behandling behandling = prosessinstans.getBehandling();
        String opprettetJournalpostID = "532523";

        byte[] vedlegg = new byte[10];
        prosessinstans = prosessinstans.toBuilder()
            .medData(ProsessDataKey.VEDLEGG_SED,
                Set.of(new DokumentReferanse(behandling.getInitierendeJournalpostId(), journalpost.getHoveddokument().getDokumentId())))
            .build();

        when(joarkFasade.hentJournalpost(behandling.getInitierendeJournalpostId())).thenReturn(journalpost);
        when(joarkFasade.hentDokument(behandling.getInitierendeJournalpostId(), journalpost.getHoveddokument().getDokumentId()))
            .thenReturn(vedlegg);
        when(sedSomBrevService.lagJournalpostForSendingAvSedSomBrev(any(SedType.class), any(Land_iso2.class), any(), any(), eq(prosessinstansUuid.toString())))
            .thenReturn(opprettetJournalpostID);

        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.setId(1L);
        behandlingsresultat.setBehandling(behandling);
        when(behandlingsresultatService.hentBehandlingsresultat(anyLong())).thenReturn(behandlingsresultat);

        when(fagsakService.hentFagsak(any())).thenReturn(lagFagsak());

        videresendSoknad.utfør(prosessinstans);

        verify(sedSomBrevService)
            .lagJournalpostForSendingAvSedSomBrev(eq(SedType.A008), any(Land_iso2.class), eq(behandling), anyList(), eq(prosessinstansUuid.toString()));
        assertThat(prosessinstans.getData(ProsessDataKey.DISTRIBUERBAR_JOURNALPOST_ID)).isEqualTo(opprettetJournalpostID);
        assertThat(prosessinstans.getData(ProsessDataKey.DISTRIBUER_MOTTAKER_LAND, Landkoder.class)).isEqualTo(Landkoder.SE);
    }

    private Prosessinstans opprettProsessinstans() {
        behandling.setFagsak(lagFagsak());

        return Prosessinstans.builder()
            .medType(ProsessType.OPPRETT_SAK)
            .medStatus(ProsessStatus.KLAR)
            .medBehandling(behandling)
            .build();
    }

    private static Fagsak lagFagsak() {
        return FagsakTestFactory.builder()
            .medGsakSaksnummer()
            .medTrygdemyndighet()
            .medBruker()
            .build();
    }
}
