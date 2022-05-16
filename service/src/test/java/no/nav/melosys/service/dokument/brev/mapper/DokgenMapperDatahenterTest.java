package no.nav.melosys.service.dokument.brev.mapper;

import java.time.*;
import java.util.List;
import java.util.Set;

import no.finn.unleash.Unleash;
import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.arkiv.ArkivDokument;
import no.nav.melosys.domain.arkiv.Journalpost;
import no.nav.melosys.domain.brev.AvslagBrevbestilling;
import no.nav.melosys.domain.brev.DokgenBrevbestilling;
import no.nav.melosys.domain.brev.FritekstbrevBrevbestilling;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Avsendertyper;
import no.nav.melosys.domain.kodeverk.Representerer;
import no.nav.melosys.integrasjon.ereg.EregFasade;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.dokument.DokumentHentingSystemService;
import no.nav.melosys.service.kodeverk.KodeverkService;
import no.nav.melosys.service.persondata.PersondataFasade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.*;
import static no.nav.melosys.service.dokument.DokgenTestData.lagBehandling;
import static no.nav.melosys.service.dokument.DokgenTestData.lagFagsak;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DokgenMapperDatahenterTest {

    private final String ORGNR = "87654321";
    private final String FNR = "12345678901";
    private final String AKTOER_ID = "123";

    @Mock
    private EregFasade eregFasade;
    @Mock
    private BehandlingsresultatService behandlingsresultatService;
    @Mock
    private KodeverkService kodeverkService;
    @Mock
    private PersondataFasade persondataFasade;
    @Mock
    private Unleash unleash;
    @Mock
    private DokumentHentingSystemService dokumentHentingService;

    private DokgenMapperDatahenter dokgenMapperDatahenter;

    @BeforeEach
    void init() {
        dokgenMapperDatahenter = new DokgenMapperDatahenter(behandlingsresultatService, eregFasade, persondataFasade, dokumentHentingService, kodeverkService);
    }

    @Test
    void skalMappeTilAvslagbrevMedRiktigeMangelbrevdatoer() {
        LocalDate datoSeptember = LocalDate.of(2021, 9, 9);
        LocalDate datoOktober = LocalDate.of(2021, 10, 9);
        LocalDate datoDesember = LocalDate.of(2021, 12, 9);
        Behandling behandling = lagBehandling(lagFagsak(true));
        behandling.setRegistrertDato(LocalDateTime.of(2021, 10, 1, 0, 0).toInstant(ZoneOffset.UTC));

        DokgenBrevbestilling brevbestilling = new AvslagBrevbestilling.Builder()
            .medProduserbartdokument(AVSLAG_MANGLENDE_OPPLYSNINGER)
            .medBehandling(behandling)
            .medFritekst("Hei")
            .build();

        when(dokumentHentingService.hentDokumenter(any())).thenReturn(List.of(
            lagJournalpost(datoSeptember),
            lagJournalpost(datoDesember),
            lagJournalpost(datoOktober)));

        List<Instant> mangelbrevDatoer = dokgenMapperDatahenter.hentMangelbrevDatoer(brevbestilling);

        assertThat(mangelbrevDatoer)
            .hasSize(2)
            .containsExactly(
                datoOktober.atStartOfDay(ZoneId.systemDefault()).toInstant(),
                datoDesember.atStartOfDay(ZoneId.systemDefault()).toInstant()
            );
    }

    @Test
    void hentFullmektigNavn_fullmektigPerson_henterSammensattNavnPerson() {
        Fagsak fagsak = new Fagsak();
        fagsak.setAktører(Set.of(lagMottakerFullmektigPerson(), new Aktoer()));

        when(persondataFasade.hentSammensattNavn(FNR)).thenReturn("Etternavn, Fornavn");

        dokgenMapperDatahenter.hentFullmektigNavn(fagsak, Representerer.BRUKER);

        verify(persondataFasade).hentSammensattNavn(FNR);
    }

    @Test
    void hentFullmektigNavn_fullmektigOrg_henterNavnOrganisasjon() {
        Fagsak fagsak = new Fagsak();
        fagsak.setAktører(Set.of(lagMottakerFullmektigOrgnr(), new Aktoer()));

        when(eregFasade.hentOrganisasjonNavn(ORGNR)).thenReturn("Orgnavn");

        dokgenMapperDatahenter.hentFullmektigNavn(fagsak, Representerer.BRUKER);

        verify(eregFasade).hentOrganisasjonNavn(ORGNR);
    }

    @Test
    void hentPersondata_mottakerAktørID_brukerAktørID() {
        dokgenMapperDatahenter.hentPersondata(lagMottakerBruker());

        verify(persondataFasade).hentPerson(AKTOER_ID);
    }

    @Test
    void hentPersondata_mottakerPersonIdent_brukerPersonIdent() {
        dokgenMapperDatahenter.hentPersondata(lagMottakerFullmektigPerson());

        verify(persondataFasade).hentPerson(FNR);
    }

    private Journalpost lagJournalpost(LocalDate forsendelseJournalfoertDato) {
        Instant forsteDato = forsendelseJournalfoertDato.atStartOfDay(ZoneId.of("Europe/Paris")).toInstant();
        ArkivDokument arkivDokument = new ArkivDokument();
        arkivDokument.setTittel(MELDING_MANGLENDE_OPPLYSNINGER.getBeskrivelse());
        Journalpost journalpost = new Journalpost("1");
        journalpost.setHoveddokument(arkivDokument);
        journalpost.setForsendelseJournalfoert(forsteDato);
        journalpost.setAvsenderType(Avsendertyper.PERSON);

        return journalpost;
    }

    private Aktoer lagMottakerBruker() {
        Aktoer mottaker = new Aktoer();
        mottaker.setRolle(Aktoersroller.BRUKER);
        mottaker.setAktørId(AKTOER_ID);
        return mottaker;
    }

    private Aktoer lagMottakerFullmektigPerson() {
        Aktoer mottaker = new Aktoer();
        mottaker.setRolle(Aktoersroller.REPRESENTANT);
        mottaker.setRepresenterer(Representerer.BRUKER);
        mottaker.setPersonIdent(FNR);
        return mottaker;
    }

    private Aktoer lagMottakerFullmektigOrgnr() {
        Aktoer mottaker = new Aktoer();
        mottaker.setRolle(Aktoersroller.REPRESENTANT);
        mottaker.setRepresenterer(Representerer.BRUKER);
        mottaker.setOrgnr(ORGNR);
        return mottaker;
    }
}
