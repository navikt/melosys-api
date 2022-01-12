package no.nav.melosys.service.dokument;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.google.common.collect.Sets;
import no.nav.melosys.domain.*;
import no.nav.melosys.domain.avgift.Trygdeavgiftsberegningsresultat;
import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet;
import no.nav.melosys.domain.behandlingsgrunnlag.Behandlingsgrunnlag;
import no.nav.melosys.domain.behandlingsgrunnlag.BehandlingsgrunnlagData;
import no.nav.melosys.domain.behandlingsgrunnlag.data.ForetakUtland;
import no.nav.melosys.domain.brev.Mottaker;
import no.nav.melosys.domain.brev.Mottakerliste;
import no.nav.melosys.domain.dokument.arbeidsforhold.Arbeidsforhold;
import no.nav.melosys.domain.dokument.arbeidsforhold.ArbeidsforholdDokument;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Representerer;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.aktoer.KontaktopplysningService;
import no.nav.melosys.service.aktoer.UtenlandskMyndighetService;
import no.nav.melosys.service.avgift.TrygdeavgiftsberegningService;
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static java.util.Collections.emptyList;
import static no.nav.melosys.domain.brev.FastMottaker.SKATT;
import static no.nav.melosys.domain.kodeverk.Aktoersroller.*;
import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BrevmottakerServiceTest {
    @Mock
    private KontaktopplysningService kontaktopplysningService;
    @Mock
    private AvklarteVirksomheterService avklarteVirksomheterService;
    @Mock
    private UtenlandskMyndighetService utenlandskMyndighetService;
    @Mock
    private BehandlingsresultatService behandlingsresultatService;
    @Mock
    private TrygdeavgiftsberegningService trygdeavgiftsberegningService;
    @Mock
    private Behandling behandling;

    private Behandlingsresultat behandlingsresultat;
    private BrevmottakerService brevmottakerService;

    @BeforeEach
    void setup() {
        brevmottakerService = new BrevmottakerService(kontaktopplysningService, avklarteVirksomheterService,
            utenlandskMyndighetService, behandlingsresultatService, trygdeavgiftsberegningService);

        behandlingsresultat = new Behandlingsresultat();
        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1);
        behandlingsresultat.getLovvalgsperioder().add(lovvalgsperiode);
    }

    @Test
    void avklarMottakere_medRolleSomIkkeErStøttet_feiler() {
        assertThatThrownBy(() -> brevmottakerService.avklarMottakere(null, Mottaker.av(REPRESENTANT), behandling))
            .isInstanceOf(FunksjonellException.class)
            .hasMessage("REPRESENTANT støttes ikke.");
    }

    @Test
    void avklarMottakere_medBrukerRolleOgIkkeRegistretBruker_feiler() {
        when(behandling.getFagsak()).thenReturn(new Fagsak());

        assertThatThrownBy(() -> brevmottakerService.avklarMottakere(null, Mottaker.av(BRUKER), behandling))
            .isInstanceOf(FunksjonellException.class)
            .hasMessage("Bruker er ikke registrert.");
    }

    @Test
    void avklarMottakere_medBrukerRolleUtenRepresentant_girBrukerAktør() {
        when(behandling.getFagsak()).thenReturn(lagFagsakMedRepresentant(null));

        List<Aktoer> aktoers = brevmottakerService.avklarMottakere(null, Mottaker.av(BRUKER), behandling);

        assertThat(aktoers).hasSize(1);
        assertThat(aktoers.get(0).getRolle()).isEqualTo(BRUKER);
    }

    @Test
    void avklarMottakere_medBrukerRolleMedRepresentant_girRepresentantAktør() {
        when(behandling.getFagsak()).thenReturn(lagFagsakMedRepresentant(Representerer.BRUKER));

        List<Aktoer> aktoers = brevmottakerService.avklarMottakere(null, Mottaker.av(BRUKER), behandling);

        assertThat(aktoers).hasSize(1);
        assertThat(aktoers.get(0).getRolle()).isEqualTo(REPRESENTANT);
    }

    @Test
    void avklarMottakere_medArbeidsgiverRolleOgIngenArbeidsgivere_feiler() {
        when(behandling.getFagsak()).thenReturn(lagFagsakMedRepresentant(null));
        when(avklarteVirksomheterService.hentNorskeArbeidsgivendeOrgnumre(eq(behandling))).thenReturn(Collections.emptySet());
        when(avklarteVirksomheterService.hentUtenlandskeVirksomheter(eq(behandling))).thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> brevmottakerService.avklarMottakere(null, Mottaker.av(ARBEIDSGIVER), behandling))
            .isInstanceOf(FunksjonellException.class)
            .hasMessage("Arbeidsgiver er ikke registrert.");
    }

    @Test
    void avklarMottakere_medArbeidsgiverRolle_girArbeidsgiverAktører() {
        when(avklarteVirksomheterService.hentNorskeArbeidsgivendeOrgnumre(eq(behandling))).thenReturn(Sets.newHashSet("123456789", "987654321"));
        when(behandling.getFagsak()).thenReturn(lagFagsakMedRepresentant(null));

        List<Aktoer> arbeidsgivere = brevmottakerService.avklarMottakere(null, Mottaker.av(ARBEIDSGIVER), behandling);

        assertThat(arbeidsgivere)
            .flatExtracting(Aktoer::getOrgnr)
            .containsExactlyInAnyOrder("123456789", "987654321");
    }

    @Test
    void avklarMottakere_medBareUtenlandskeArbeidsgivere_girIngenMottakere() {
        when(behandling.getFagsak()).thenReturn(lagFagsakMedRepresentant(null));
        when(avklarteVirksomheterService.hentNorskeArbeidsgivendeOrgnumre(eq(behandling))).thenReturn(Collections.emptySet());
        when(avklarteVirksomheterService.hentUtenlandskeVirksomheter(eq(behandling)))
            .thenReturn(Collections.singletonList(new AvklartVirksomhet(new ForetakUtland())));

        List<Aktoer> arbeidsgivere = brevmottakerService.avklarMottakere(null, Mottaker.av(ARBEIDSGIVER), behandling);
        assertThat(arbeidsgivere).isEmpty();
    }

    @Test
    void avklarMottakere_medArbeidsgiverRolleIkkeKunAvklarteVirksomheterOgIngenArbeidsgivere_girTomListe() {
        when(behandling.getFagsak()).thenReturn(lagFagsakMedRepresentant(null));
        when(behandling.getBehandlingsgrunnlag()).thenReturn(lagBehandlingsgrunnlag(null, null));
        when(behandling.hentArbeidsforholdDokument()).thenReturn(lagArbeidsforholdDokument(null));

        List<Aktoer> arbeidsgivere = brevmottakerService.avklarMottakere(null, Mottaker.av(ARBEIDSGIVER), behandling, false, false);

        assertThat(arbeidsgivere).isEmpty();
    }

    @Test
    void avklarMottakere_medArbeidsgiverRolleIkkeKunAvklarteVirksomheter_girArbeidsgiverAktører() {
        when(behandling.getFagsak()).thenReturn(lagFagsakMedRepresentant(null));
        when(behandling.getBehandlingsgrunnlag()).thenReturn(lagBehandlingsgrunnlag("987654321", null));
        when(behandling.hentArbeidsforholdDokument()).thenReturn(lagArbeidsforholdDokument("123456789"));

        List<Aktoer> arbeidsgivere = brevmottakerService.avklarMottakere(null, Mottaker.av(ARBEIDSGIVER), behandling, false, false);

        assertThat(arbeidsgivere)
            .flatExtracting(Aktoer::getOrgnr)
            .containsExactlyInAnyOrder("123456789", "987654321");
    }

    @Test
    void avklarMottakere_medBareUtenlandskeArbeidsgivereIkkeKunAvklarteVirksomheter_girIngenMottakere() {
        when(behandling.getFagsak()).thenReturn(lagFagsakMedRepresentant(null));
        when(behandling.getBehandlingsgrunnlag()).thenReturn(lagBehandlingsgrunnlag(null, "uuid"));
        when(behandling.hentArbeidsforholdDokument()).thenReturn(lagArbeidsforholdDokument(null));

        List<Aktoer> arbeidsgivere = brevmottakerService.avklarMottakere(null, Mottaker.av(ARBEIDSGIVER), behandling, false, false);
        assertThat(arbeidsgivere).isEmpty();
    }

    @Test
    void avklarMottakere_medArbeidsgiverRolleOgRepresentantForBruker_girArbeidsgiverAktører() {
        when(avklarteVirksomheterService.hentNorskeArbeidsgivendeOrgnumre(eq(behandling))).thenReturn(Sets.newHashSet("123456789", "987654321"));
        when(behandling.getFagsak()).thenReturn(lagFagsakMedRepresentant(Representerer.BRUKER));

        List<Aktoer> arbeidsgivere = brevmottakerService.avklarMottakere(null, Mottaker.av(ARBEIDSGIVER), behandling);

        assertThat(arbeidsgivere)
            .flatExtracting(Aktoer::getOrgnr)
            .containsExactlyInAnyOrder("123456789", "987654321");
    }

    @Test
    void avklarMottakere_medArbeidsgiverRolleOgRepresentantForArbeidsgiver_girRepresentantAktør() {
        when(behandling.getFagsak()).thenReturn(lagFagsakMedRepresentant(Representerer.ARBEIDSGIVER));

        List<Aktoer> arbeidsgivere = brevmottakerService.avklarMottakere(null, Mottaker.av(ARBEIDSGIVER), behandling);

        assertThat(arbeidsgivere)
            .flatExtracting(Aktoer::getOrgnr)
            .containsExactly("REP-ORGNR");
    }

    @Test
    void avklarMottakere_medArbeidsgiverRolleOgRepresentantForBegge_girRepresentantAktør() {
        when(behandling.getFagsak()).thenReturn(lagFagsakMedRepresentant(Representerer.BEGGE));

        List<Aktoer> arbeidsgivere = brevmottakerService.avklarMottakere(null, Mottaker.av(ARBEIDSGIVER), behandling);

        assertThat(arbeidsgivere)
            .flatExtracting(Aktoer::getOrgnr)
            .containsExactly("REP-ORGNR");
    }

    @Test
    void avklarMottakere_art12_1_CZerReservertFraA1_forventerIngenAktør() {
        when(utenlandskMyndighetService.lagUtenlandskeMyndigheterFraBehandling(eq(behandling))).thenReturn(Collections.singletonMap(lagUtenlandskMyndighet(), lagAktoerUtenlandskMyndighet()));
        when(behandlingsresultatService.hentBehandlingsresultat(anyLong())).thenReturn(behandlingsresultat);

        List<Aktoer> myndigheter = brevmottakerService.avklarMottakere(Produserbaredokumenter.ATTEST_A1, Mottaker.av(MYNDIGHET), behandling);
        assertThat(myndigheter).isEmpty();
    }

    @Test
    void avklarMottakere_art_11_4_2_CZerReservertFraA1_forventerIngenAktør() {
        when(utenlandskMyndighetService.lagUtenlandskeMyndigheterFraBehandling(eq(behandling))).thenReturn(Collections.singletonMap(lagUtenlandskMyndighet(), lagAktoerUtenlandskMyndighet()));
        when(behandlingsresultatService.hentBehandlingsresultat(anyLong())).thenReturn(behandlingsresultat);

        behandlingsresultat.hentValidertLovvalgsperiode().setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_4_2);
        List<Aktoer> myndigheter = brevmottakerService.avklarMottakere(Produserbaredokumenter.ATTEST_A1, Mottaker.av(MYNDIGHET), behandling);
        assertThat(myndigheter).isEmpty();
    }

    @Test
    void avklarMottakere_A001_CZerReservertFraA1_forventerMyndighetAktør() {
        when(utenlandskMyndighetService.lagUtenlandskeMyndigheterFraBehandling(eq(behandling))).thenReturn(Collections.singletonMap(lagUtenlandskMyndighet(), lagAktoerUtenlandskMyndighet()));

        List<Aktoer> myndigheter = brevmottakerService.avklarMottakere(Produserbaredokumenter.ANMODNING_UNNTAK, Mottaker.av(MYNDIGHET), behandling);
        assertThat(myndigheter)
            .flatExtracting(Aktoer::getInstitusjonId)
            .containsExactly("CZ:SZUC10416");
    }

    @Test
    void gittMalIkkeRegistret_skalKasteFeil() {
        assertThatExceptionOfType(IkkeFunnetException.class)
            .isThrownBy(() -> brevmottakerService.hentMottakerliste(ATTEST_A1, behandling))
            .withMessage("Mangler mapping av mottakere for ATTEST_A1");
    }

    @Test
    void gittForvaltningsmelding_skalHovedmottakerVæreBruker() {
        assertThat(brevmottakerService.hentMottakerliste(MELDING_FORVENTET_SAKSBEHANDLINGSTID, behandling))
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

        verifyNoInteractions(trygdeavgiftsberegningService);
    }

    @Test
    void gittMangelbrevBruker_skalHovedmottakerVæreBruker() {
        assertThat(brevmottakerService.hentMottakerliste(MANGELBREV_BRUKER, behandling))
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

        verifyNoInteractions(trygdeavgiftsberegningService);
    }

    @Test
    void gittMangelbrevArbeidsgiver_skalHovedmottakerVæreArbeidsgiverMedKopi() {
        when(behandling.getFagsak()).thenReturn(lagFagsakMedRepresentant(null));

        assertThat(brevmottakerService.hentMottakerliste(MANGELBREV_ARBEIDSGIVER, behandling))
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

        verify(trygdeavgiftsberegningService).finnBeregningsresultat(anyLong());
    }

    @Test
    void gittVedtakFtrl2_8UtenFullmektigIkkeSelvbetalende_skalHovedmottakerVæreBrukerMedKopier() {
        initMocksForFtrlVedtaksbrev(null, 10000, false);

        assertThat(brevmottakerService.hentMottakerliste(INNVILGELSE_FOLKETRYGDLOVEN_2_8, behandling))
            .extracting(
                Mottakerliste::getHovedMottaker,
                Mottakerliste::getKopiMottakere,
                Mottakerliste::getFasteMottakere
            )
            .containsExactly(
                BRUKER,
                List.of(ARBEIDSGIVER),
                List.of(SKATT)
            );

        verify(trygdeavgiftsberegningService).finnBeregningsresultat(anyLong());
    }

    @Test
    void gittVedtakFtrl2_8UtenFullmektigSelvbetalende_skalHovedmottakerVæreBrukerMedKopier() {
        initMocksForFtrlVedtaksbrev(null, 10000, true);

        assertThat(brevmottakerService.hentMottakerliste(INNVILGELSE_FOLKETRYGDLOVEN_2_8, behandling))
            .isNotNull()
            .extracting(
                Mottakerliste::getHovedMottaker,
                Mottakerliste::getKopiMottakere,
                Mottakerliste::getFasteMottakere
            )
            .containsExactly(
                BRUKER,
                emptyList(),
                List.of(SKATT)
            );

        verify(trygdeavgiftsberegningService).finnBeregningsresultat(anyLong());
    }

    @Test
    void gittVedtakFtrl2_8FullmektigIkkeSelvbetalende_skalHovedmottakerVæreBrukerMedKopier() {
        initMocksForFtrlVedtaksbrev(Representerer.BRUKER, 10000, false);

        assertThat(brevmottakerService.hentMottakerliste(INNVILGELSE_FOLKETRYGDLOVEN_2_8, behandling))
            .isNotNull()
            .extracting(
                Mottakerliste::getHovedMottaker,
                Mottakerliste::getKopiMottakere,
                Mottakerliste::getFasteMottakere
            )
            .containsExactly(
                BRUKER,
                List.of(BRUKER, ARBEIDSGIVER),
                List.of(SKATT)
            );

        verify(trygdeavgiftsberegningService).finnBeregningsresultat(anyLong());
    }

    @Test
    void gittVedtakFtrl2_8FullmektigSelvbetalende_skalHovedmottakerVæreBrukerMedKopier() {
        initMocksForFtrlVedtaksbrev(Representerer.BRUKER, 10000, true);

        Mottakerliste actual = brevmottakerService.hentMottakerliste(INNVILGELSE_FOLKETRYGDLOVEN_2_8, behandling);
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
                List.of(SKATT)
            );

        verify(trygdeavgiftsberegningService).finnBeregningsresultat(anyLong());
    }

    @Test
    void gittVedtakFtrl2_8FullmektigIkkeSelvbetalendeIkkeInntekt_skalHovedmottakerVæreBrukerMedKopier() {
        initMocksForFtrlVedtaksbrev(Representerer.BRUKER, 0, false);

        assertThat(brevmottakerService.hentMottakerliste(INNVILGELSE_FOLKETRYGDLOVEN_2_8, behandling))
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

        verify(trygdeavgiftsberegningService).finnBeregningsresultat(anyLong());
    }

    @Test
    void gittInnvilgelsesbrevUK_skalHovedmottakerVæreBrukerMedKopier() {
        when(behandling.getFagsak()).thenReturn(lagFagsakMedRepresentant(null));

        assertThat(brevmottakerService.hentMottakerliste(STORBRITANNIA, behandling))
            .extracting(
                Mottakerliste::getHovedMottaker,
                Mottakerliste::getKopiMottakere,
                Mottakerliste::getFasteMottakere
            )
            .containsExactly(
                BRUKER,
                List.of(ARBEIDSGIVER),
                List.of(SKATT)
            );
    }

    @Test
    void hentKontaktopplysning_utenMottaker_girTomtResultat() {
        Kontaktopplysning kontaktopplysning = brevmottakerService.hentKontaktopplysning("MEL-123", null);
        assertThat(kontaktopplysning).isNull();

        verifyNoInteractions(kontaktopplysningService);
    }

    @Test
    void hentKontaktopplysning_mottakerBruker_girTomtResultat() {
        Aktoer aktoer = new Aktoer();
        aktoer.setRolle(BRUKER);

        Kontaktopplysning kontaktopplysning = brevmottakerService.hentKontaktopplysning("MEL-123", aktoer);
        assertThat(kontaktopplysning).isNull();

        verifyNoInteractions(kontaktopplysningService);
    }

    @Test
    void hentKontaktopplysning_mottakerArbeidsgiver_returnererKontaktopplysning() {
        String orgnr = "987654321";
        when(kontaktopplysningService.hentKontaktopplysning(any(), eq(orgnr))).thenReturn(lagKontaktOpplysning(false));
        Aktoer aktoer = new Aktoer();
        aktoer.setRolle(ARBEIDSGIVER);
        aktoer.setOrgnr(orgnr);

        Kontaktopplysning kontaktopplysning = brevmottakerService.hentKontaktopplysning("MEL-123", aktoer);
        assertThat(kontaktopplysning).isNotNull();
        assertThat(kontaktopplysning.getKontaktNavn()).isEqualTo("Kari Arbeidsgiver");

        verify(kontaktopplysningService).hentKontaktopplysning(eq("MEL-123"), eq(orgnr));
    }

    @Test
    void hentKontaktopplysning_mottakerRepresentant_returnererKontaktopplysning() {
        String orgnr = "123456789";
        when(kontaktopplysningService.hentKontaktopplysning(any(), eq(orgnr))).thenReturn(lagKontaktOpplysning(true));
        Aktoer aktoer = new Aktoer();
        aktoer.setRolle(REPRESENTANT);
        aktoer.setOrgnr(orgnr);

        Kontaktopplysning kontaktopplysning = brevmottakerService.hentKontaktopplysning("MEL-123", aktoer);
        assertThat(kontaktopplysning).isNotNull();
        assertThat(kontaktopplysning.getKontaktNavn()).isEqualTo("Ole Fullmektig");

        verify(kontaktopplysningService).hentKontaktopplysning(eq("MEL-123"), eq(orgnr));
    }

    @Test
    void avklarMottakerRolleFraDokument_tilBruker_girRolleBruker() {
        Aktoersroller mottakerRolle = brevmottakerService.avklarMottakerRolleFraDokument(MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD);

        assertThat(mottakerRolle).isEqualTo(BRUKER);
    }

    @Test
    void avklarMottakerRolleFraDokument_tilArbeidsgiver_girRolleArbeidsgiver() {
        Aktoersroller mottakerRolle = brevmottakerService.avklarMottakerRolleFraDokument(INNVILGELSE_ARBEIDSGIVER);

        assertThat(mottakerRolle).isEqualTo(ARBEIDSGIVER);
    }

    @Test
    void avklarMottakerRolleFraDokument_tilMyndighet_girRolleMyndighet() {
        Aktoersroller mottakerRolle = brevmottakerService.avklarMottakerRolleFraDokument(ATTEST_A1);

        assertThat(mottakerRolle).isEqualTo(MYNDIGHET);
    }

    @Test
    void avklarMottakerRolleFraDokument_UtenMapping_feiler() {
        assertThatThrownBy(() -> brevmottakerService.avklarMottakerRolleFraDokument(ORIENTERING_UTPEKING_UTLAND))
            .isInstanceOf(TekniskException.class)
            .hasMessage("Valg av mottakerRolle støttes ikke for ORIENTERING_UTPEKING_UTLAND");
    }

    private void initMocksForFtrlVedtaksbrev(Representerer representerer, long norskinntekt, boolean selvbetalende) {
        Aktoer aktoer = new Aktoer();
        aktoer.setRolle(selvbetalende ? BRUKER : REPRESENTANT_TRYGDEAVGIFT);
        Optional<Trygdeavgiftsberegningsresultat> trygdeavgiftsberegningsresultat =
            Optional.of(new Trygdeavgiftsberegningsresultat(norskinntekt, null, aktoer, emptyList()));

        when(trygdeavgiftsberegningService.finnBeregningsresultat(anyLong())).thenReturn(trygdeavgiftsberegningsresultat);

        Fagsak fagsak = lagFagsakMedRepresentant(representerer);
        fagsak.setType(Sakstyper.FTRL);
        when(behandling.getFagsak()).thenReturn(fagsak);
    }

    private Fagsak lagFagsakMedRepresentant(Representerer representerer) {
        Fagsak fagsak = new Fagsak();
        Aktoer bruker = new Aktoer();
        bruker.setRolle(BRUKER);
        fagsak.getAktører().add(bruker);

        if (representerer != null) {
            Aktoer representant = new Aktoer();
            representant.setRepresenterer(representerer);
            representant.setRolle(REPRESENTANT);
            representant.setOrgnr("REP-ORGNR");
            fagsak.getAktører().add(representant);
        }
        return fagsak;
    }

    private Behandlingsgrunnlag lagBehandlingsgrunnlag(String ekstraArbeidsgivereOrgnr, String foretakUtlandUuid) {
        var behandlingsgrunnlagData = new BehandlingsgrunnlagData();
        behandlingsgrunnlagData.juridiskArbeidsgiverNorge.ekstraArbeidsgivere.add(ekstraArbeidsgivereOrgnr);
        var foretakUtland = new ForetakUtland();
        foretakUtland.uuid = foretakUtlandUuid;
        behandlingsgrunnlagData.foretakUtland.add(foretakUtland);
        var behandlingsgrunnlag = new Behandlingsgrunnlag();
        behandlingsgrunnlag.setBehandlingsgrunnlagdata(behandlingsgrunnlagData);
        return behandlingsgrunnlag;
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
        utenlandskMyndighet.landkode = Landkoder.CZ;
        utenlandskMyndighet.institusjonskode = "SZUC10416";
        utenlandskMyndighet.preferanser = Collections.singleton(new Preferanse(1L, Preferanse.PreferanseEnum.RESERVERT_FRA_A1));

        return utenlandskMyndighet;
    }

    private Aktoer lagAktoerUtenlandskMyndighet() {
        Aktoer aktoer = new Aktoer();
        aktoer.setRolle(MYNDIGHET);
        aktoer.setInstitusjonId("CZ:SZUC10416");
        return aktoer;
    }


    private Optional<Kontaktopplysning> lagKontaktOpplysning(boolean representant) {
        Kontaktopplysning kontaktopplysning = new Kontaktopplysning();
        kontaktopplysning.setKontaktNavn(representant ? "Ole Fullmektig" : "Kari Arbeidsgiver");
        return Optional.of(kontaktopplysning);
    }
}
