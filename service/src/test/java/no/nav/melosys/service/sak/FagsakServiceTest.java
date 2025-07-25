package no.nav.melosys.service.sak;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.kodeverk.*;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsaarsaktyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.repository.FagsakRepository;
import no.nav.melosys.service.SaksbehandlingDataFactory;
import no.nav.melosys.service.aktoer.KontaktopplysningService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.lovligekombinasjoner.LovligeKombinasjonerSaksbehandlingService;
import no.nav.melosys.service.persondata.PersondataFasade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static no.nav.melosys.domain.FagsakTestFactory.SAKSNUMMER;
import static no.nav.melosys.domain.kodeverk.Sakstemaer.MEDLEMSKAP_LOVVALG;
import static no.nav.melosys.domain.kodeverk.Sakstyper.EU_EOS;
import static no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema.*;
import static no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper.FØRSTEGANG;
import static no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper.NY_VURDERING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FagsakServiceTest {
    @Mock
    private FagsakRepository fagsakRepo;
    @Mock
    private BehandlingService behandlingService;
    @Mock
    private KontaktopplysningService kontaktopplysningService;
    @Mock
    private PersondataFasade persondataFasade;
    @Mock
    private LovligeKombinasjonerSaksbehandlingService lovligeKombinasjonerSaksbehandlingService;
    private FagsakService fagsakService;

    @BeforeEach
    public void setUp() {
        fagsakService = new FagsakService(
            fagsakRepo,
            behandlingService,
            kontaktopplysningService,
            persondataFasade,
                lovligeKombinasjonerSaksbehandlingService);
    }

    @Test
    void hentFagsak() {
        when(fagsakRepo.findBySaksnummer(anyString())).thenReturn(Optional.of(FagsakTestFactory.lagFagsak()));
        fagsakService.hentFagsak(SAKSNUMMER);
        verify(fagsakRepo).findBySaksnummer(SAKSNUMMER);
    }

    @Test
    void hentFagsakerMedAktør() {
        when(persondataFasade.hentAktørIdForIdent(any())).thenReturn("AKTOER_ID");
        fagsakService.hentFagsakerMedAktør(Aktoersroller.BRUKER, "FNR");
        verify(fagsakRepo).findByRolleAndAktør(Aktoersroller.BRUKER, "AKTOER_ID");
    }

    @Test
    void lagre() {
        Fagsak fagsak = FagsakTestFactory.lagFagsak();
        fagsakService.lagre(fagsak);
        verify(fagsakRepo).save(fagsak);
        assertThat(fagsak).isNotNull();
        assertThat(fagsak.getSaksnummer()).isNotEmpty();
    }

    @Test
    void nyFagsakOgBehandling() {
        Behandling behandling = mock(Behandling.class);
        String initierendeJournalpostId = "234";
        String initierendeDokumentId = "221234";
        doReturn(behandling).when(behandlingService).nyBehandling(any(), any(), any(), any(), anyString(), anyString(), any(), any(), anyString());

        OpprettSakRequest opprettSakRequest = new OpprettSakRequest.Builder()
            .medAktørID("123456789")
            .medSakstype(EU_EOS)
            .medSakstema(MEDLEMSKAP_LOVVALG)
            .medBehandlingstype(FØRSTEGANG)
            .medBehandlingstema(UTSENDT_ARBEIDSTAKER)
            .medInitierendeJournalpostId(initierendeJournalpostId)
            .medInitierendeDokumentId(initierendeDokumentId)
            .medArbeidsgiver("arbeidsgiver")
            .medFullmektig(new FullmektigDto("orgnr", null, List.of(Fullmaktstype.FULLMEKTIG_ARBEIDSGIVER)))
            .medBehandlingsårsaktype(Behandlingsaarsaktyper.FRITEKST)
            .medBehandlingsårsakFritekst("Fritekst")
            .build();


        Fagsak fagsak = fagsakService.nyFagsakOgBehandling(opprettSakRequest);


        verify(fagsakRepo).save(any(Fagsak.class));
        verify(behandlingService).nyBehandling(any(), eq(Behandlingsstatus.OPPRETTET), eq(FØRSTEGANG),
            eq(UTSENDT_ARBEIDSTAKER), eq(initierendeJournalpostId), eq(initierendeDokumentId), any(),
            eq(Behandlingsaarsaktyper.FRITEKST), eq("Fritekst"));
        assertThat(fagsak.getBehandlinger()).isNotEmpty();
        assertThat(fagsak.getType()).isEqualTo(EU_EOS);
        assertThat(fagsak.getTema()).isEqualTo(MEDLEMSKAP_LOVVALG);
        var lagretFullmektig = fagsak.finnFullmektig(Fullmaktstype.FULLMEKTIG_ARBEIDSGIVER);
        assertThat(lagretFullmektig).isNotNull()
            .extracting(Aktoer::getFagsak, Aktoer::getRolle, Aktoer::getOrgnr)
            .containsExactly(fagsak, Aktoersroller.FULLMEKTIG, "orgnr");
        assertThat(lagretFullmektig.getFullmakter()).flatExtracting(Fullmakt::getType).containsExactly(Fullmaktstype.FULLMEKTIG_ARBEIDSGIVER);
    }

    @Test
    void nyFagsakOgBehandling_kontaktPersonFinnes_KontaktOpplysningOpprettes() {
        when(behandlingService.nyBehandling(any(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(new Behandling());
        Kontaktopplysning kontaktopplysning = Kontaktopplysning.av("FullmektigOrgnr", "Kontaktperson", "Telefon", "Orgnr");
        OpprettSakRequest opprettSakRequest = new OpprettSakRequest.Builder()
            .medAktørID("123456789")
            .medSakstype(EU_EOS)
            .medSakstema(MEDLEMSKAP_LOVVALG)
            .medBehandlingstype(FØRSTEGANG)
            .medKontaktopplysninger(List.of(kontaktopplysning))
            .build();

        fagsakService.nyFagsakOgBehandling(opprettSakRequest);

        verify(kontaktopplysningService).lagEllerOppdaterKontaktopplysning(
            any(), eq("FullmektigOrgnr"), eq("Orgnr"), eq("Kontaktperson"), eq("Telefon")
        );
    }

    @Test
    void oppdaterFagsakOgBehandling() {
        Fagsak fagsak = lagFagsakMedBruker();
        fagsak.leggTilBehandling(SaksbehandlingDataFactory.lagBehandling());
        when(fagsakRepo.findBySaksnummer(SAKSNUMMER)).thenReturn(Optional.of(fagsak));

        fagsakService.oppdaterFagsakOgBehandling(fagsak.getSaksnummer(), Sakstyper.TRYGDEAVTALE, MEDLEMSKAP_LOVVALG, Behandlingstema.ARBEID_FLERE_LAND, NY_VURDERING, null, null);

        verify(fagsakRepo).save(fagsak);
        verify(lovligeKombinasjonerSaksbehandlingService).validerOpprettelseOgEndring(fagsak.getHovedpartRolle(), Sakstyper.TRYGDEAVTALE, MEDLEMSKAP_LOVVALG, Behandlingstema.ARBEID_FLERE_LAND, NY_VURDERING);
        verify(behandlingService).endreBehandling(fagsak.finnAktivBehandlingIkkeÅrsavregning().getId(), NY_VURDERING, ARBEID_FLERE_LAND, null, null);
    }

    @Test
    void oppdaterFagsakOgBehandling_ingenEndringPåTypeTema_validererIkke() {
        Fagsak fagsak = lagFagsakMedBruker();
        Behandling behandling = SaksbehandlingDataFactory.lagBehandling();
        fagsak.leggTilBehandling(behandling);
        when(fagsakRepo.findBySaksnummer(SAKSNUMMER)).thenReturn(Optional.of(fagsak));

        fagsakService.oppdaterFagsakOgBehandling(fagsak.getSaksnummer(), fagsak.getType(), fagsak.getTema(), behandling.getTema(), behandling.getType(), null, null);

        verifyNoInteractions(lovligeKombinasjonerSaksbehandlingService);
    }

    @Test
    void leggTilFjernAktørerForMyndighet() {
        Fagsak eksisterendeFagsak = lagFagsakMedAktørforMyndighet();
        when(fagsakRepo.findBySaksnummer(SAKSNUMMER)).thenReturn(Optional.of(eksisterendeFagsak));

        List<String> nyeInstitusjonsIder = Collections.singletonList("Ny institusjonsid");
        fagsakService.oppdaterMyndigheterForEuEos(SAKSNUMMER, nyeInstitusjonsIder);

        ArgumentCaptor<Fagsak> captor = ArgumentCaptor.forClass(Fagsak.class);
        verify(fagsakRepo).save(captor.capture());
        Fagsak oppdaterFagsak = captor.getValue();
        assertThat(oppdaterFagsak.getAktører().stream()
            .map(Aktoer::getInstitusjonID)
            .collect(Collectors.toList())).isSubsetOf(nyeInstitusjonsIder);
    }

    @Test
    void oppdaterMyndigheter_harBruker_fjernerIkkeBruker() {
        Fagsak eksisterendeFagsak = lagFagsakMedAktørforMyndighet();
        when(fagsakRepo.findBySaksnummer(SAKSNUMMER)).thenReturn(Optional.of(eksisterendeFagsak));

        Aktoer bruker = new Aktoer();
        bruker.setFagsak(eksisterendeFagsak);
        bruker.setRolle(Aktoersroller.BRUKER);
        bruker.setAktørId("1234");
        eksisterendeFagsak.leggTilAktør(bruker);

        List<String> nyeInstitusjonsIder = Collections.singletonList("Ny institusjonsid");
        fagsakService.oppdaterMyndigheterForEuEos(SAKSNUMMER, nyeInstitusjonsIder);

        ArgumentCaptor<Fagsak> captor = ArgumentCaptor.forClass(Fagsak.class);
        verify(fagsakRepo).save(captor.capture());
        Fagsak oppdaterFagsak = captor.getValue();

        assertThat(oppdaterFagsak.getAktører())
            .extracting(Aktoer::getRolle, Aktoer::getAktørId, Aktoer::getInstitusjonID)
            .containsExactlyInAnyOrder(
                tuple(Aktoersroller.BRUKER, "1234", null),
                tuple(Aktoersroller.TRYGDEMYNDIGHET, null, "Ny institusjonsid")
            );
    }

    @Test
    void avsluttFagsakOgBehandling_erAktiv_blirAvsluttet() {
        Fagsak fagsak = FagsakTestFactory.lagFagsak();
        Behandling behandling = BehandlingTestBuilder.builderWithDefaults()
            .medId(1L)
            .medStatus(Behandlingsstatus.UNDER_BEHANDLING)
            .medFagsak(fagsak)
            .build();

        fagsak.leggTilBehandling(behandling);
        fagsakService.avsluttFagsakOgBehandling(fagsak, Saksstatuser.LOVVALG_AVKLART);
        assertThat(fagsak.getStatus()).isEqualTo(Saksstatuser.LOVVALG_AVKLART);
        verify(fagsakRepo).save(fagsak);
        verify(behandlingService).avsluttBehandling(behandling.getId());
    }

    @Test
    void avsluttFagsakOgBehandling_behandlingTilhørerAnnenFagsak_kasterException() {
        Fagsak fagsak = FagsakTestFactory.lagFagsak();

        Behandling behandling = BehandlingTestBuilder.builderWithDefaults()
            .medId(1L)
            .medStatus(Behandlingsstatus.UNDER_BEHANDLING)
            .medFagsak(fagsak)
            .build();

        fagsak.leggTilBehandling(behandling);
        behandling.setFagsak(FagsakTestFactory.builder().saksnummer("MEL-annenId").build());

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> fagsakService.avsluttFagsakOgBehandling(fagsak, behandling, Saksstatuser.LOVVALG_AVKLART))
            .withMessageContaining("tilhører ikke fagsak");
    }

    @Test
    void avsluttFagsakOgBehandling_manglerBehandling_avslutterFagsak() {
        var fagsak = FagsakTestFactory.lagFagsak();

        fagsakService.avsluttFagsakOgBehandling(fagsak, Saksstatuser.AVSLUTTET);

        assertThat(fagsak.getStatus()).isEqualTo(Saksstatuser.AVSLUTTET);
        verify(fagsakRepo).save(fagsak);
        verify(behandlingService, never()).avsluttBehandling(anyLong());
    }

    @Test
    void hentFagsakerMedOrgNr_riktigSortering() {
        Behandling behandlingAktivRegistrertNaa = lagBehandling(1L, FØRSTEGANG, Behandlingsstatus.UNDER_BEHANDLING, Instant.now());
        Behandling behandlingInaktivRegistretNaa = lagBehandling(2L, FØRSTEGANG, Behandlingsstatus.AVSLUTTET, Instant.now());
        Behandling behandlingAktivRegistrertFoer = lagBehandling(3L, FØRSTEGANG, Behandlingsstatus.UNDER_BEHANDLING,
            Instant.now().minusSeconds(3600));
        Behandling behandlingInaktivRegistrertFoer = lagBehandling(4L, FØRSTEGANG, Behandlingsstatus.AVSLUTTET, Instant.now().minusSeconds(3600));
        Fagsak fagsak1 = lagFagsakMedAktørForVirksomhet();
        Fagsak fagsak2 = lagFagsakMedAktørForVirksomhet();
        Fagsak fagsak3 = lagFagsakMedAktørForVirksomhet();
        Fagsak fagsak4 = lagFagsakMedAktørForVirksomhet();
        fagsak1.leggTilBehandling(behandlingAktivRegistrertNaa);
        fagsak2.leggTilBehandling(behandlingAktivRegistrertFoer);
        fagsak3.leggTilBehandling(behandlingInaktivRegistretNaa);
        fagsak4.leggTilBehandling(behandlingInaktivRegistrertFoer);

        when(fagsakRepo.findByRolleAndOrgnr(Aktoersroller.VIRKSOMHET, "12345")).thenReturn(List.of(fagsak2, fagsak4, fagsak1, fagsak3));

        List<Fagsak> fagsakList = fagsakService.hentFagsakerMedOrgnr(Aktoersroller.VIRKSOMHET, "12345");

        assertThat(fagsakList).hasSize(4);
        assertThat(fagsakList.get(0).hentSistRegistrertBehandling()).isEqualTo(behandlingAktivRegistrertNaa);
        assertThat(fagsakList.get(1).hentSistRegistrertBehandling()).isEqualTo(behandlingAktivRegistrertFoer);
        assertThat(fagsakList.get(2).hentSistRegistrertBehandling()).isEqualTo(behandlingInaktivRegistretNaa);
        assertThat(fagsakList.get(3).hentSistRegistrertBehandling()).isEqualTo(behandlingInaktivRegistrertFoer);
    }

    @ParameterizedTest
    @EnumSource(Betalingstype.class)
    void lagreBetalingsvalgForPensjonisterIFagsak(Betalingstype betalingsvalg) {
        Fagsak fagsak = FagsakTestFactory.lagFagsak();

        when(fagsakRepo.findBySaksnummer(SAKSNUMMER)).thenReturn(Optional.of(fagsak));

        fagsakService.lagreBetalingsvalg(SAKSNUMMER, betalingsvalg);

        assertThat(fagsak.getBetalingsvalg()).isEqualTo(betalingsvalg);
    }

    private Fagsak lagFagsakMedAktørforMyndighet() {
        Fagsak fagsak = FagsakTestFactory.lagFagsak();

        Aktoer aktoer = new Aktoer();
        aktoer.setInstitusjonID("Gammel institusjonsid");
        aktoer.setFagsak(fagsak);
        aktoer.setRolle(Aktoersroller.TRYGDEMYNDIGHET);
        fagsak.leggTilAktør(aktoer);
        return fagsak;
    }

    private Fagsak lagFagsakMedAktørForVirksomhet() {
        Fagsak fagsak = FagsakTestFactory.lagFagsak();

        Aktoer aktoer = new Aktoer();
        aktoer.setOrgnr("12345");
        aktoer.setRolle(Aktoersroller.VIRKSOMHET);
        fagsak.leggTilAktør(aktoer);
        return fagsak;
    }

    private Fagsak lagFagsakMedBruker() {
        Fagsak fagsak = FagsakTestFactory.lagFagsak();

        Aktoer aktoer = new Aktoer();
        aktoer.setAktørId("12312");
        aktoer.setRolle(Aktoersroller.BRUKER);
        fagsak.leggTilAktør(aktoer);
        return fagsak;
    }

    private Behandling lagBehandling(long id, Behandlingstyper type, Behandlingsstatus status, Instant registrertDato) {
        var behandling = new Behandling();
        behandling.setId(id);
        behandling.setType(type);
        behandling.setStatus(status);
        behandling.setEndretDato(registrertDato);
        behandling.setRegistrertDato(registrertDato);
        return behandling;
    }
}
