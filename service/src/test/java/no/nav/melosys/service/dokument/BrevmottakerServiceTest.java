package no.nav.melosys.service.dokument;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.Sets;
import no.nav.melosys.domain.*;
import no.nav.melosys.domain.avgift.Trygdeavgiftsberegningsresultat;
import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet;
import no.nav.melosys.domain.brev.Mottaker;
import no.nav.melosys.domain.brev.Mottakerliste;
import no.nav.melosys.domain.dokument.arbeidsforhold.Arbeidsforhold;
import no.nav.melosys.domain.dokument.arbeidsforhold.ArbeidsforholdDokument;
import no.nav.melosys.domain.kodeverk.*;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.trygdeavtale.Lovvalgsbestemmelser_trygdeavtale_gb;
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysninger;
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysningerData;
import no.nav.melosys.domain.mottatteopplysninger.data.ForetakUtland;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.aktoer.UtenlandskMyndighetService;
import no.nav.melosys.service.avgift.TrygdeavgiftsberegningServiceDeprecated;
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static java.util.Collections.emptyList;
import static no.nav.melosys.domain.brev.NorskMyndighet.SKATTEETATEN;
import static no.nav.melosys.domain.kodeverk.Mottakerroller.*;
import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BrevmottakerServiceTest {
    @Mock
    private AvklarteVirksomheterService avklarteVirksomheterService;
    @Mock
    private UtenlandskMyndighetService utenlandskMyndighetService;
    @Mock
    private BehandlingsresultatService behandlingsresultatService;
    @Mock
    private TrygdeavgiftsberegningServiceDeprecated trygdeavgiftsberegningServiceDeprecated;
    @Mock
    private LovvalgsperiodeService lovvalgsperiodeService;
    @Mock
    private BehandlingService behandlingService;
    @Mock
    private Behandling behandling;

    private Behandlingsresultat behandlingsresultat;
    private BrevmottakerService brevmottakerService;

    @BeforeEach
    void setup() {
        brevmottakerService = new BrevmottakerService(
            avklarteVirksomheterService, utenlandskMyndighetService, behandlingsresultatService,
                trygdeavgiftsberegningServiceDeprecated, lovvalgsperiodeService, behandlingService);

        behandlingsresultat = new Behandlingsresultat();
        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1);
        behandlingsresultat.getLovvalgsperioder().add(lovvalgsperiode);
    }

    @Test
    void avklarMottakere_medFullmektigRepresentererOrg_feiler() {
        when(behandling.getFagsak()).thenReturn(lagFagsakMedFullmektigPerson(Representerer.ARBEIDSGIVER));

        assertThatThrownBy(() -> brevmottakerService.avklarMottakere(null, Mottaker.medRolle(FULLMEKTIG), behandling))
            .isInstanceOf(FunksjonellException.class)
            .hasMessage("Finner ikke fullmektig som representerer bruker");
    }

    @Test
    void avklarMottakere_medFullmektigRepresentererBruker_girFullmektigMottaker() {
        when(behandling.getFagsak()).thenReturn(lagFagsakMedFullmektigPerson(Representerer.BRUKER));

        List<Mottaker> mottakere = brevmottakerService.avklarMottakere(null, Mottaker.medRolle(BRUKER), behandling);

        assertThat(mottakere).hasSize(1);
        assertThat(mottakere.get(0).getRolle()).isEqualTo(FULLMEKTIG);
    }

    @Test
    void avklarMottakere_medBrukerRolleOgIkkeRegistretBruker_feiler() {
        when(behandling.getFagsak()).thenReturn(new Fagsak());

        assertThatThrownBy(() -> brevmottakerService.avklarMottakere(null, Mottaker.medRolle(BRUKER), behandling))
            .isInstanceOf(FunksjonellException.class)
            .hasMessage("Bruker er ikke registrert.");
    }

    @Test
    void avklarMottakere_medBrukerRolleUtenFullmektig_girBrukerMottaker() {
        when(behandling.getFagsak()).thenReturn(lagFagsakMedFullmektigOrg(null));

        List<Mottaker> mottakere = brevmottakerService.avklarMottakere(null, Mottaker.medRolle(BRUKER), behandling);

        assertThat(mottakere).hasSize(1);
        assertThat(mottakere.get(0).getRolle()).isEqualTo(BRUKER);
    }

    @Test
    void avklarMottakere_medBrukerRolleMedFullmektigOrg_girFullmektigMottaker() {
        when(behandling.getFagsak()).thenReturn(lagFagsakMedFullmektigOrg(Representerer.BRUKER));

        List<Mottaker> mottakere = brevmottakerService.avklarMottakere(null, Mottaker.medRolle(BRUKER), behandling);

        assertThat(mottakere).hasSize(1);
        assertThat(mottakere.get(0).getRolle()).isEqualTo(FULLMEKTIG);
    }

    @Test
    void avklarMottakere_medBrukerRolleMedFullmektigPerson_girFullmektigMottaker() {
        when(behandling.getFagsak()).thenReturn(lagFagsakMedFullmektigPerson(Representerer.BRUKER));

        List<Mottaker> mottakere = brevmottakerService.avklarMottakere(null, Mottaker.medRolle(BRUKER), behandling);

        assertThat(mottakere).hasSize(1);
        assertThat(mottakere.get(0).getRolle()).isEqualTo(FULLMEKTIG);
    }

    @Test
    void avklarMottakere_medVirksomhetRolleOgIngenVirksomhet_feiler() {
        var mottaker = Mottaker.medRolle(VIRKSOMHET);
        when(behandling.getFagsak()).thenReturn(new Fagsak());

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> brevmottakerService.avklarMottakere(null, mottaker, behandling))
            .withMessageContaining("Virksomhet er ikke registrert.");
    }

    @Test
    void avklarMottakere_medVirksomhetRolleOgVirksomhet_girVirksomhetMottaker() {
        Aktoer virksomhet = new Aktoer();
        virksomhet.setRolle(Aktoersroller.VIRKSOMHET);
        virksomhet.setOrgnr("orgnr");
        var fagsak = new Fagsak();
        fagsak.setAktører(Set.of(virksomhet));
        when(behandling.getFagsak()).thenReturn(fagsak);

        List<Mottaker> mottakere = brevmottakerService.avklarMottakere(null, Mottaker.medRolle(VIRKSOMHET), behandling);

        assertThat(mottakere)
            .hasSize(1)
            .contains(Mottaker.av(virksomhet));
    }

    @Test
    void avklarMottakere_medArbeidsgiverRolleOgIngenArbeidsgivere_feiler() {
        when(behandling.getFagsak()).thenReturn(lagFagsakMedFullmektigOrg(null));
        when(avklarteVirksomheterService.hentNorskeArbeidsgivendeOrgnumre(behandling)).thenReturn(Collections.emptySet());
        when(avklarteVirksomheterService.hentUtenlandskeVirksomheter(behandling)).thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> brevmottakerService.avklarMottakere(null, Mottaker.medRolle(ARBEIDSGIVER), behandling))
            .isInstanceOf(FunksjonellException.class)
            .hasMessage("Arbeidsgiver er ikke registrert.");
    }

    @Test
    void avklarMottakere_medArbeidsgiverRolle_girArbeidsgiverMottakere() {
        when(avklarteVirksomheterService.hentNorskeArbeidsgivendeOrgnumre(behandling)).thenReturn(Sets.newHashSet("123456789", "987654321"));
        when(behandling.getFagsak()).thenReturn(lagFagsakMedFullmektigOrg(null));

        List<Mottaker> arbeidsgivere = brevmottakerService.avklarMottakere(null, Mottaker.medRolle(ARBEIDSGIVER), behandling);

        assertThat(arbeidsgivere)
            .flatExtracting(Mottaker::getOrgnr)
            .containsExactlyInAnyOrder("123456789", "987654321");
    }

    @Test
    void avklarMottakere_medBareUtenlandskeArbeidsgivere_girIngenMottakere() {
        when(behandling.getFagsak()).thenReturn(lagFagsakMedFullmektigOrg(null));
        when(avklarteVirksomheterService.hentNorskeArbeidsgivendeOrgnumre(behandling)).thenReturn(Collections.emptySet());
        when(avklarteVirksomheterService.hentUtenlandskeVirksomheter(behandling))
            .thenReturn(Collections.singletonList(new AvklartVirksomhet(new ForetakUtland())));

        List<Mottaker> arbeidsgivere = brevmottakerService.avklarMottakere(null, Mottaker.medRolle(ARBEIDSGIVER), behandling);
        assertThat(arbeidsgivere).isEmpty();
    }

    @Test
    void avklarMottakere_medArbeidsgiverRolleIkkeKunAvklarteVirksomheterOgIngenArbeidsgivere_girTomListe() {
        when(behandling.getFagsak()).thenReturn(lagFagsakMedFullmektigOrg(null));
        when(behandling.getMottatteOpplysninger()).thenReturn(lagMottatteOpplysninger(null, null));
        when(behandling.finnArbeidsforholdDokument()).thenReturn(Optional.of(lagArbeidsforholdDokument(null)));

        List<Mottaker> arbeidsgivere = brevmottakerService.avklarMottakere(null, Mottaker.medRolle(ARBEIDSGIVER), behandling, false, false);

        assertThat(arbeidsgivere).isEmpty();
    }

    @Test
    void avklarMottakere_medArbeidsgiverRolleIkkeKunAvklarteVirksomheter_girArbeidsgiverMottakere() {
        when(behandling.getFagsak()).thenReturn(lagFagsakMedFullmektigOrg(null));
        when(behandling.getMottatteOpplysninger()).thenReturn(lagMottatteOpplysninger("987654321", null));
        when(behandling.finnArbeidsforholdDokument()).thenReturn(Optional.of(lagArbeidsforholdDokument("123456789")));

        List<Mottaker> arbeidsgivere = brevmottakerService.avklarMottakere(null, Mottaker.medRolle(ARBEIDSGIVER), behandling, false, false);

        assertThat(arbeidsgivere)
            .flatExtracting(Mottaker::getOrgnr)
            .containsExactlyInAnyOrder("123456789", "987654321");
    }

    @Test
    void avklarMottakere_medBareUtenlandskeArbeidsgivereIkkeKunAvklarteVirksomheter_girIngenMottakere() {
        when(behandling.getFagsak()).thenReturn(lagFagsakMedFullmektigOrg(null));
        when(behandling.getMottatteOpplysninger()).thenReturn(lagMottatteOpplysninger(null, "uuid"));
        when(behandling.finnArbeidsforholdDokument()).thenReturn(Optional.of(lagArbeidsforholdDokument(null)));

        List<Mottaker> arbeidsgivere = brevmottakerService.avklarMottakere(null, Mottaker.medRolle(ARBEIDSGIVER), behandling, false, false);
        assertThat(arbeidsgivere).isEmpty();
    }

    @Test
    void avklarMottakere_medArbeidsgiverRolleOgFullmektigForBruker_girArbeidsgiverMottakere() {
        when(avklarteVirksomheterService.hentNorskeArbeidsgivendeOrgnumre(behandling)).thenReturn(Sets.newHashSet("123456789", "987654321"));
        when(behandling.getFagsak()).thenReturn(lagFagsakMedFullmektigOrg(Representerer.BRUKER));

        List<Mottaker> arbeidsgivere = brevmottakerService.avklarMottakere(null, Mottaker.medRolle(ARBEIDSGIVER), behandling);

        assertThat(arbeidsgivere)
            .flatExtracting(Mottaker::getOrgnr)
            .containsExactlyInAnyOrder("123456789", "987654321");
    }

    @Test
    void avklarMottakere_medArbeidsgiverRolleOgFullmektigForArbeidsgiver_girFullmektigMottakere() {
        when(behandling.getFagsak()).thenReturn(lagFagsakMedFullmektigOrg(Representerer.ARBEIDSGIVER));

        List<Mottaker> arbeidsgivere = brevmottakerService.avklarMottakere(null, Mottaker.medRolle(ARBEIDSGIVER), behandling);

        assertThat(arbeidsgivere)
            .flatExtracting(Mottaker::getAktørId, Mottaker::getPersonIdent, Mottaker::getOrgnr)
            .containsExactly(null, null, "REP-ORGNR");
    }

    @Test
    void avklarMottakere_medArbeidsgiverRolleOgFullmektigForBegge_girFullmektigMottakere() {
        when(behandling.getFagsak()).thenReturn(lagFagsakMedFullmektigPerson(Representerer.BEGGE));

        List<Mottaker> arbeidsgivere = brevmottakerService.avklarMottakere(null, Mottaker.medRolle(ARBEIDSGIVER), behandling);

        assertThat(arbeidsgivere)
            .flatExtracting(Mottaker::getAktørId, Mottaker::getPersonIdent, Mottaker::getOrgnr)
            .containsExactly(null, "REP-FNR", null);
    }

    @Test
    void avklarMottakere_art12_1_CZerReservertFraA1_forventerIngenMottaker() {
        when(utenlandskMyndighetService.lagUtenlandskeMyndigheterFraBehandling(behandling)).thenReturn(Collections.singletonMap(lagUtenlandskMyndighet(), lagMottakerUtenlandskMyndighet()));
        when(behandlingsresultatService.hentBehandlingsresultat(anyLong())).thenReturn(behandlingsresultat);

        List<Mottaker> myndigheter = brevmottakerService.avklarMottakere(Produserbaredokumenter.ATTEST_A1, Mottaker.medRolle(UTENLANDSK_TRYGDEMYNDIGHET), behandling);
        assertThat(myndigheter).isEmpty();
    }

    @Test
    void avklarMottakere_art_11_4_2_CZerReservertFraA1_forventerIngenMottaker() {
        when(utenlandskMyndighetService.lagUtenlandskeMyndigheterFraBehandling(behandling)).thenReturn(Collections.singletonMap(lagUtenlandskMyndighet(), lagMottakerUtenlandskMyndighet()));
        when(behandlingsresultatService.hentBehandlingsresultat(anyLong())).thenReturn(behandlingsresultat);

        behandlingsresultat.hentLovvalgsperiode().setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_4_2);
        List<Mottaker> myndigheter = brevmottakerService.avklarMottakere(Produserbaredokumenter.ATTEST_A1, Mottaker.medRolle(UTENLANDSK_TRYGDEMYNDIGHET), behandling);
        assertThat(myndigheter).isEmpty();
    }

    @Test
    void avklarMottakere_A001_CZerReservertFraA1_forventerMyndighetMottaker() {
        when(utenlandskMyndighetService.lagUtenlandskeMyndigheterFraBehandling(behandling)).thenReturn(Collections.singletonMap(lagUtenlandskMyndighet(), lagMottakerUtenlandskMyndighet()));

        List<Mottaker> myndigheter = brevmottakerService.avklarMottakere(Produserbaredokumenter.ANMODNING_UNNTAK, Mottaker.medRolle(UTENLANDSK_TRYGDEMYNDIGHET), behandling);
        assertThat(myndigheter)
            .flatExtracting(Mottaker::getInstitusjonID)
            .containsExactly("CZ:SZUC10416");
    }

    @Test
    void gittMalIkkeRegistret_skalKasteFeil() {
        assertThatExceptionOfType(IkkeFunnetException.class)
            .isThrownBy(() -> brevmottakerService.hentMottakerliste(ATTEST_A1, 123))
            .withMessage("Mangler mapping av mottakere for ATTEST_A1");
    }

    @Test
    void gittForvaltningsmelding_skalHovedmottakerVæreBruker() {
        assertThat(brevmottakerService.hentMottakerliste(MELDING_FORVENTET_SAKSBEHANDLINGSTID, 123))
            .extracting(
                Mottakerliste::getHovedMottaker,
                Mottakerliste::getKopiMottakere,
                Mottakerliste::getFasteMottakere
            )
            .containsExactly(
                BRUKER,
                emptyList(),
                emptyList()
            );

        verifyNoInteractions(trygdeavgiftsberegningServiceDeprecated);
    }

    @Test
    void gittMangelbrevBruker_skalHovedmottakerVæreBruker() {
        assertThat(brevmottakerService.hentMottakerliste(MANGELBREV_BRUKER, 123))
            .extracting(
                Mottakerliste::getHovedMottaker,
                Mottakerliste::getKopiMottakere,
                Mottakerliste::getFasteMottakere
            )
            .containsExactly(
                BRUKER,
                emptyList(),
                emptyList()
            );

        verifyNoInteractions(trygdeavgiftsberegningServiceDeprecated);
    }

    @Test
    void gittMangelbrevArbeidsgiver_skalHovedmottakerVæreArbeidsgiverMedKopi() {
        when(behandlingService.hentBehandlingMedSaksopplysninger(123L)).thenReturn(behandling);
        when(behandling.getFagsak()).thenReturn(lagFagsakMedFullmektigOrg(null));

        assertThat(brevmottakerService.hentMottakerliste(MANGELBREV_ARBEIDSGIVER, 123))
            .extracting(
                Mottakerliste::getHovedMottaker,
                Mottakerliste::getKopiMottakere,
                Mottakerliste::getFasteMottakere
            )
            .containsExactly(
                ARBEIDSGIVER,
                List.of(BRUKER),
                emptyList()
            );

        verify(trygdeavgiftsberegningServiceDeprecated).finnBeregningsresultat(anyLong());
    }

    @Test
    void gittVedtakFtrl2_8UtenFullmektigIkkeSelvbetalende_skalHovedmottakerVæreBrukerMedKopier() {
        initMocksForFtrlVedtaksbrev(null, 10000, false);

        assertThat(brevmottakerService.hentMottakerliste(INNVILGELSE_FOLKETRYGDLOVEN_2_8, 123))
            .extracting(
                Mottakerliste::getHovedMottaker,
                Mottakerliste::getKopiMottakere,
                Mottakerliste::getFasteMottakere
            )
            .containsExactly(
                BRUKER,
                List.of(ARBEIDSGIVER),
                List.of(SKATTEETATEN)
            );

        verify(trygdeavgiftsberegningServiceDeprecated).finnBeregningsresultat(anyLong());
    }

    @Test
    void gittVedtakFtrl2_8UtenFullmektigSelvbetalende_skalHovedmottakerVæreBrukerMedKopier() {
        initMocksForFtrlVedtaksbrev(null, 10000, true);

        assertThat(brevmottakerService.hentMottakerliste(INNVILGELSE_FOLKETRYGDLOVEN_2_8, 123))
            .isNotNull()
            .extracting(
                Mottakerliste::getHovedMottaker,
                Mottakerliste::getKopiMottakere,
                Mottakerliste::getFasteMottakere
            )
            .containsExactly(
                BRUKER,
                emptyList(),
                List.of(SKATTEETATEN)
            );

        verify(trygdeavgiftsberegningServiceDeprecated).finnBeregningsresultat(anyLong());
    }

    @Test
    void gittVedtakFtrl2_8FullmektigIkkeSelvbetalende_skalHovedmottakerVæreBrukerMedKopier() {
        initMocksForFtrlVedtaksbrev(Representerer.BRUKER, 10000, false);

        assertThat(brevmottakerService.hentMottakerliste(INNVILGELSE_FOLKETRYGDLOVEN_2_8, 123))
            .isNotNull()
            .extracting(
                Mottakerliste::getHovedMottaker,
                Mottakerliste::getKopiMottakere,
                Mottakerliste::getFasteMottakere
            )
            .containsExactly(
                BRUKER,
                List.of(BRUKER, ARBEIDSGIVER),
                List.of(SKATTEETATEN)
            );

        verify(trygdeavgiftsberegningServiceDeprecated).finnBeregningsresultat(anyLong());
    }

    @Test
    void gittVedtakFtrl2_8FullmektigSelvbetalende_skalHovedmottakerVæreBrukerMedKopier() {
        initMocksForFtrlVedtaksbrev(Representerer.BRUKER, 10000, true);

        Mottakerliste actual = brevmottakerService.hentMottakerliste(INNVILGELSE_FOLKETRYGDLOVEN_2_8, 123);
        assertThat(actual)
            .isNotNull()
            .extracting(
                Mottakerliste::getHovedMottaker,
                Mottakerliste::getKopiMottakere,
                Mottakerliste::getFasteMottakere
            )
            .containsExactly(
                BRUKER,
                List.of(BRUKER),
                List.of(SKATTEETATEN)
            );

        verify(trygdeavgiftsberegningServiceDeprecated).finnBeregningsresultat(anyLong());
    }

    @Test
    void gittVedtakFtrl2_8FullmektigIkkeSelvbetalendeIkkeInntekt_skalHovedmottakerVæreBrukerMedKopier() {
        initMocksForFtrlVedtaksbrev(Representerer.BRUKER, 0, false);

        assertThat(brevmottakerService.hentMottakerliste(INNVILGELSE_FOLKETRYGDLOVEN_2_8, 123))
            .isNotNull()
            .extracting(
                Mottakerliste::getHovedMottaker,
                Mottakerliste::getKopiMottakere,
                Mottakerliste::getFasteMottakere
            )
            .containsExactly(
                BRUKER,
                List.of(BRUKER, ARBEIDSGIVER),
                emptyList()
            );

        verify(trygdeavgiftsberegningServiceDeprecated).finnBeregningsresultat(anyLong());
    }

    @Test
    void gittInnvilgelsesbrevUK_skalHovedmottakerVæreBrukerMedKopier() {
        var lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setBestemmelse(Lovvalgsbestemmelser_trygdeavtale_gb.UK_ART6_1);
        when(behandlingService.hentBehandlingMedSaksopplysninger(123L)).thenReturn(behandling);
        when(behandling.getFagsak()).thenReturn(lagFagsakMedFullmektigOrg(null));
        when(lovvalgsperiodeService.hentLovvalgsperiode(anyLong())).thenReturn(lovvalgsperiode);

        assertThat(brevmottakerService.hentMottakerliste(TRYGDEAVTALE_GB, 123))
            .extracting(
                Mottakerliste::getHovedMottaker,
                Mottakerliste::getKopiMottakere,
                Mottakerliste::getFasteMottakere
            )
            .containsExactly(
                BRUKER,
                List.of(ARBEIDSGIVER, UTENLANDSK_TRYGDEMYNDIGHET),
                List.of(SKATTEETATEN)
            );
    }

    @Test
    void gittInnvilgelsesbrevUKOgArt82_skalIkkeMyndighetFåKopi() {
        var lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setBestemmelse(Lovvalgsbestemmelser_trygdeavtale_gb.UK_ART8_2);
        when(behandlingService.hentBehandlingMedSaksopplysninger(123L)).thenReturn(behandling);
        when(behandling.getFagsak()).thenReturn(lagFagsakMedFullmektigOrg(null));
        when(lovvalgsperiodeService.hentLovvalgsperiode(anyLong())).thenReturn(lovvalgsperiode);

        assertThat(brevmottakerService.hentMottakerliste(TRYGDEAVTALE_GB, 123))
            .extracting(
                Mottakerliste::getHovedMottaker,
                Mottakerliste::getKopiMottakere,
                Mottakerliste::getFasteMottakere
            )
            .containsExactly(
                BRUKER,
                List.of(ARBEIDSGIVER),
                List.of(SKATTEETATEN)
            );
    }


    @Test
    void avklarMottakerRolleFraDokument_tilBruker_girRolleBruker() {
        Mottakerroller mottakerRolle = brevmottakerService.avklarMottakerRolleFraDokument(MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD);

        assertThat(mottakerRolle).isEqualTo(BRUKER);
    }

    @Test
    void avklarMottakerRolleFraDokument_tilArbeidsgiver_girRolleArbeidsgiver() {
        Mottakerroller mottakerRolle = brevmottakerService.avklarMottakerRolleFraDokument(INNVILGELSE_ARBEIDSGIVER);

        assertThat(mottakerRolle).isEqualTo(ARBEIDSGIVER);
    }

    @Test
    void avklarMottakerRolleFraDokument_tilMyndighet_girRolleMyndighet() {
        Mottakerroller mottakerRolle = brevmottakerService.avklarMottakerRolleFraDokument(ATTEST_A1);

        assertThat(mottakerRolle).isEqualTo(UTENLANDSK_TRYGDEMYNDIGHET);
    }

    @Test
    void avklarMottakerRolleFraDokument_UtenMapping_feiler() {
        assertThatThrownBy(() -> brevmottakerService.avklarMottakerRolleFraDokument(ORIENTERING_UTPEKING_UTLAND))
            .isInstanceOf(TekniskException.class)
            .hasMessage("Valg av mottakerRolle støttes ikke for ORIENTERING_UTPEKING_UTLAND");
    }

    private void initMocksForFtrlVedtaksbrev(Representerer representerer, long norskinntekt, boolean selvbetalende) {
        Optional<Trygdeavgiftsberegningsresultat> trygdeavgiftsberegningsresultat = Optional.of(
            new Trygdeavgiftsberegningsresultat(norskinntekt, null, lagAktoer(selvbetalende ? Aktoersroller.BRUKER : Aktoersroller.REPRESENTANT_TRYGDEAVGIFT), emptyList())
        );

        when(trygdeavgiftsberegningServiceDeprecated.finnBeregningsresultat(anyLong())).thenReturn(trygdeavgiftsberegningsresultat);

        when(behandlingService.hentBehandlingMedSaksopplysninger(123L)).thenReturn(behandling);
        Fagsak fagsak = lagFagsakMedFullmektigOrg(representerer);
        fagsak.setType(Sakstyper.FTRL);
        when(behandling.getFagsak()).thenReturn(fagsak);
    }

    private Aktoer lagAktoer(Aktoersroller rolle) {
        var aktoer = new Aktoer();
        aktoer.setRolle(rolle);
        return aktoer;
    }

    private Fagsak lagFagsakMedFullmektigOrg(Representerer representerer) {
        Fagsak fagsak = new Fagsak();
        fagsak.getAktører().add(lagAktoer(Aktoersroller.BRUKER));

        if (representerer != null) {
            Aktoer aktoer = lagAktoer(Aktoersroller.REPRESENTANT);
            aktoer.setRepresenterer(representerer);
            aktoer.setOrgnr("REP-ORGNR");
            fagsak.getAktører().add(aktoer);
        }
        return fagsak;
    }

    private Fagsak lagFagsakMedFullmektigPerson(Representerer representerer) {
        Fagsak fagsak = new Fagsak();
        fagsak.getAktører().add(lagAktoer(Aktoersroller.BRUKER));

        if (representerer != null) {
            Aktoer aktoer = lagAktoer(Aktoersroller.REPRESENTANT);
            aktoer.setRepresenterer(representerer);
            aktoer.setPersonIdent("REP-FNR");
            fagsak.getAktører().add(aktoer);
        }
        return fagsak;
    }

    private MottatteOpplysninger lagMottatteOpplysninger(String ekstraArbeidsgivereOrgnr, String foretakUtlandUuid) {
        var mottatteOpplysningerData = new MottatteOpplysningerData();
        mottatteOpplysningerData.juridiskArbeidsgiverNorge.ekstraArbeidsgivere.add(ekstraArbeidsgivereOrgnr);
        var foretakUtland = new ForetakUtland();
        foretakUtland.uuid = foretakUtlandUuid;
        mottatteOpplysningerData.foretakUtland.add(foretakUtland);
        var mottatteOpplysninger = new MottatteOpplysninger();
        mottatteOpplysninger.setMottatteOpplysningerdata(mottatteOpplysningerData);
        return mottatteOpplysninger;
    }

    private ArbeidsforholdDokument lagArbeidsforholdDokument(String arbeidsgiverIDOrgNr) {
        var arbeidsforhold = new Arbeidsforhold();
        arbeidsforhold.arbeidsgiverID = arbeidsgiverIDOrgNr;
        var arbeidsforholdDokument = new ArbeidsforholdDokument();
        arbeidsforholdDokument.arbeidsforhold.add(arbeidsforhold);
        return arbeidsforholdDokument;
    }

    private UtenlandskMyndighet lagUtenlandskMyndighet() {
        UtenlandskMyndighet utenlandskMyndighet = new UtenlandskMyndighet();
        utenlandskMyndighet.landkode = Land_iso2.CZ;
        utenlandskMyndighet.institusjonskode = "SZUC10416";
        utenlandskMyndighet.preferanser = Collections.singleton(new Preferanse(1L, Preferanse.PreferanseEnum.RESERVERT_FRA_A1));

        return utenlandskMyndighet;
    }

    private Mottaker lagMottakerUtenlandskMyndighet() {
        Mottaker mottaker = Mottaker.medRolle(UTENLANDSK_TRYGDEMYNDIGHET);
        mottaker.setInstitusjonID("CZ:SZUC10416");
        return mottaker;
    }
}
