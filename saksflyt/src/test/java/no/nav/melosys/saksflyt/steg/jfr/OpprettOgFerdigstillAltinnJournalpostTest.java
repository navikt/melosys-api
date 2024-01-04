package no.nav.melosys.saksflyt.steg.jfr;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.arkiv.ArkivDokument;
import no.nav.melosys.domain.arkiv.BrukerIdType;
import no.nav.melosys.domain.arkiv.Journalpost;
import no.nav.melosys.domain.arkiv.OpprettJournalpost;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Fullmaktstype;
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysninger;
import no.nav.melosys.domain.msm.AltinnDokument;
import no.nav.melosys.integrasjon.ereg.EregFasade;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.saksflytapi.domain.ProsessDataKey;
import no.nav.melosys.saksflytapi.domain.Prosessinstans;
import no.nav.melosys.service.altinn.AltinnSoeknadService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.persondata.PersondataFasade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OpprettOgFerdigstillAltinnJournalpostTest {
    @Mock
    private AltinnSoeknadService altinnSoeknadService;
    @Mock
    private BehandlingService behandlingService;
    @Mock
    private EregFasade eregFasade;
    @Mock
    private JoarkFasade joarkFasade;
    @Mock
    private PersondataFasade persondataFasade;

    private OpprettOgFerdigstillAltinnJournalpost opprettOgFerdigstillAltinnJournalpost;

    private final Prosessinstans prosessinstans = new Prosessinstans();
    private final Behandling behandling = lagBehandling();

    private final Aktoer bruker = new Aktoer();
    private final String ident = "00000000000";
    private final String saksnummer = "MEL-1231";

    @Captor
    private ArgumentCaptor<OpprettJournalpost> captor;

    @BeforeEach
    public void setup() {
        final String søknadID = "soknadid1";
        prosessinstans.setData(ProsessDataKey.MOTTATT_SOKNAD_ID, søknadID);

        opprettOgFerdigstillAltinnJournalpost = new OpprettOgFerdigstillAltinnJournalpost(
            altinnSoeknadService, behandlingService, eregFasade, joarkFasade, persondataFasade);

        AltinnDokument søknadDokument = new AltinnDokument(søknadID, "dokumentid1", "tittel1",
            AltinnDokument.AltinnDokumentType.SOKNAD.name(), "pdf", Instant.now());
        AltinnDokument fullmaktDokument = new AltinnDokument(søknadID, "dokumentid2", "tittel2",
            AltinnDokument.AltinnDokumentType.FULLMAKT.name(), "pdf", Instant.now());

        bruker.setRolle(Aktoersroller.BRUKER);
        bruker.setAktørId("3321231");

        Aktoer fullmektig = new Aktoer();
        fullmektig.setRolle(Aktoersroller.FULLMEKTIG);
        fullmektig.setOrgnr("fullmektigOrgnr");
        fullmektig.setFullmaktstyper(List.of(Fullmaktstype.FULLMEKTIG_SØKNAD, Fullmaktstype.FULLMEKTIG_ARBEIDSGIVER));

        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer(saksnummer);
        fagsak.setGsakSaksnummer(123L);
        fagsak.setAktører(Set.of(bruker, fullmektig));
        behandling.setFagsak(fagsak);
        fagsak.getBehandlinger().add(behandling);
        prosessinstans.setBehandling(behandling);

        var dokumenter = new ArrayList<AltinnDokument>();
        dokumenter.add(søknadDokument);
        dokumenter.add(fullmaktDokument);
        when(altinnSoeknadService.hentDokumenterTilknyttetSoknad(søknadID)).thenReturn(dokumenter);
        when(persondataFasade.hentFolkeregisterident(anyString())).thenReturn(ident);
        when(eregFasade.hentOrganisasjonNavn(anyString())).thenReturn("Fullmektig Avsender");
        when(joarkFasade.opprettJournalpost(any(OpprettJournalpost.class), anyBoolean())).thenReturn("journalpostid123");
    }

    @Test
    void utfør_journalpostBlirOpprettet_verifiser() {
        opprettOgFerdigstillAltinnJournalpost.utfør(prosessinstans);

        verify(persondataFasade).hentFolkeregisterident(anyString());
        verify(joarkFasade).opprettJournalpost(captor.capture(), eq(true));
        verify(behandlingService).lagre(behandling);

        OpprettJournalpost opprettJournalpost = captor.getValue();
        assertThat(opprettJournalpost)
            .extracting(Journalpost::getTema, Journalpost::getMottaksKanal,
                Journalpost::getSaksnummer, Journalpost::getBrukerId, Journalpost::getBrukerIdType)
            .containsExactly("MED", "ALTINN", saksnummer, ident, BrukerIdType.FOLKEREGISTERIDENT);
        assertThat(opprettJournalpost.getInnhold()).isNotEmpty();
        assertThat(opprettJournalpost.getHoveddokument())
            .extracting(ArkivDokument::getDokumentId, ArkivDokument::getTittel)
            .containsExactly(null, "Søknad om A1 for utsendte arbeidstakere i EØS/Sveits");
        assertThat(opprettJournalpost.getVedlegg())
            .hasSize(1)
            .flatExtracting(ArkivDokument::getTittel)
            .containsExactly("Fullmakt");
        assertThat(opprettJournalpost.getKorrespondansepartNavn()).isEqualTo("Fullmektig Avsender");
    }

    @Test
    void utfør_ingenFullmektigForBruker_avsenderNavnErArbeidsgiverOrganisasjonNavn() {
        Aktoer arbeidsgiver = new Aktoer();
        arbeidsgiver.setRolle(Aktoersroller.ARBEIDSGIVER);
        arbeidsgiver.setOrgnr("arbOrgnr");
        behandling.getFagsak().setAktører(Set.of(bruker, arbeidsgiver));

        when(eregFasade.hentOrganisasjonNavn(arbeidsgiver.getOrgnr())).thenReturn("Arbeidsgiver");

        opprettOgFerdigstillAltinnJournalpost.utfør(prosessinstans);

        verify(persondataFasade).hentFolkeregisterident(anyString());
        verify(joarkFasade).opprettJournalpost(captor.capture(), eq(true));
        verify(behandlingService).lagre(behandling);

        OpprettJournalpost opprettJournalpost = captor.getValue();

        assertThat(opprettJournalpost.getKorrespondansepartNavn()).isEqualTo("Arbeidsgiver");
    }


    private Behandling lagBehandling() {
        Behandling behandling = new Behandling();
        MottatteOpplysninger grunnlag = new MottatteOpplysninger();
        grunnlag.setOriginalData("Original Can't Touch This");
        behandling.setMottatteOpplysninger(grunnlag);
        return behandling;
    }
}
