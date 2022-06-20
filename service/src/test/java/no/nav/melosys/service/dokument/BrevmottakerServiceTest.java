package no.nav.melosys.service.dokument;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_trygdeavtale_uk;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.aktoer.KontaktopplysningService;
import no.nav.melosys.service.aktoer.UtenlandskMyndighetService;
import no.nav.melosys.service.avgift.TrygdeavgiftsberegningService;
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static java.util.Collections.emptyList;
import static no.nav.melosys.domain.brev.FastMottakerMedOrgnr.SKATT;
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
    private LovvalgsperiodeService lovvalgsperiodeService;
    @Mock
    private BehandlingService behandlingService;
    @Mock
    private Behandling behandling;

    private Behandlingsresultat behandlingsresultat;
    private BrevmottakerService brevmottakerService;

    @BeforeEach
    void setup() {
        brevmottakerService = new BrevmottakerService(kontaktopplysningService, avklarteVirksomheterService,
            utenlandskMyndighetService, behandlingsresultatService, trygdeavgiftsberegningService, lovvalgsperiodeService, behandlingService);

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
        when(behandling.getFagsak()).thenReturn(lagFagsakMedRepresentantOrg(null));

        List<Aktoer> aktoers = brevmottakerService.avklarMottakere(null, Mottaker.av(BRUKER), behandling);

        assertThat(aktoers).hasSize(1);
        assertThat(aktoers.get(0).getRolle()).isEqualTo(BRUKER);
    }

    @Test
    void avklarMottakere_medBrukerRolleMedRepresentantOrg_girRepresentantAktør() {
        when(behandling.getFagsak()).thenReturn(lagFagsakMedRepresentantOrg(Representerer.BRUKER));

        List<Aktoer> aktoers = brevmottakerService.avklarMottakere(null, Mottaker.av(BRUKER), behandling);

        assertThat(aktoers).hasSize(1);
        assertThat(aktoers.get(0).getRolle()).isEqualTo(REPRESENTANT);
    }

    @Test
    void avklarMottakere_medBrukerRolleMedRepresentantPerson_girRepresentantAktør() {
        when(behandling.getFagsak()).thenReturn(lagFagsakMedRepresentantPerson(Representerer.BRUKER));

        List<Aktoer> aktoers = brevmottakerService.avklarMottakere(null, Mottaker.av(BRUKER), behandling);

        assertThat(aktoers).hasSize(1);
        assertThat(aktoers.get(0).getRolle()).isEqualTo(REPRESENTANT);
    }

    @Test
    void avklarMottakere_medVirksomhetRolleOgIngenVirksomhet_feiler() {
        var mottaker = Mottaker.av(VIRKSOMHET);
        when(behandling.getFagsak()).thenReturn(new Fagsak());

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> brevmottakerService.avklarMottakere(null, mottaker, behandling))
            .withMessageContaining("Virksomhet er ikke registrert.");
    }

    @Test
    void avklarMottakere_medVirksomhetRolleOgVirksomhet_girVirksomhetAktør() {
        Aktoer virksomhet = new Aktoer();
        virksomhet.setRolle(VIRKSOMHET);
        virksomhet.setOrgnr("orgnr");
        var fagsak = new Fagsak();
        fagsak.setAktører(Set.of(virksomhet));
        when(behandling.getFagsak()).thenReturn(fagsak);

        List<Aktoer> aktoers = brevmottakerService.avklarMottakere(null, Mottaker.av(VIRKSOMHET), behandling);

        assertThat(aktoers)
            .hasSize(1)
            .contains(virksomhet);
    }

    @Test
    void avklarMottakere_medArbeidsgiverRolleOgIngenArbeidsgivere_feiler() {
        when(behandling.getFagsak()).thenReturn(lagFagsakMedRepresentantOrg(null));
        when(avklarteVirksomheterService.hentNorskeArbeidsgivendeOrgnumre(behandling)).thenReturn(Collections.emptySet());
        when(avklarteVirksomheterService.hentUtenlandskeVirksomheter(behandling)).thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> brevmottakerService.avklarMottakere(null, Mottaker.av(ARBEIDSGIVER), behandling))
            .isInstanceOf(FunksjonellException.class)
            .hasMessage("Arbeidsgiver er ikke registrert.");
    }

    @Test
    void avklarMottakere_medArbeidsgiverRolle_girArbeidsgiverAktører() {
        when(avklarteVirksomheterService.hentNorskeArbeidsgivendeOrgnumre(behandling)).thenReturn(Sets.newHashSet("123456789", "987654321"));
        when(behandling.getFagsak()).thenReturn(lagFagsakMedRepresentantOrg(null));

        List<Aktoer> arbeidsgivere = brevmottakerService.avklarMottakere(null, Mottaker.av(ARBEIDSGIVER), behandling);

        assertThat(arbeidsgivere)
            .flatExtracting(Aktoer::getOrgnr)
            .containsExactlyInAnyOrder("123456789", "987654321");
    }

    @Test
    void avklarMottakere_medBareUtenlandskeArbeidsgivere_girIngenMottakere() {
        when(behandling.getFagsak()).thenReturn(lagFagsakMedRepresentantOrg(null));
        when(avklarteVirksomheterService.hentNorskeArbeidsgivendeOrgnumre(behandling)).thenReturn(Collections.emptySet());
        when(avklarteVirksomheterService.hentUtenlandskeVirksomheter(behandling))
            .thenReturn(Collections.singletonList(new AvklartVirksomhet(new ForetakUtland())));

        List<Aktoer> arbeidsgivere = brevmottakerService.avklarMottakere(null, Mottaker.av(ARBEIDSGIVER), behandling);
        assertThat(arbeidsgivere).isEmpty();
    }

    @Test
    void avklarMottakere_medArbeidsgiverRolleIkkeKunAvklarteVirksomheterOgIngenArbeidsgivere_girTomListe() {
        when(behandling.getFagsak()).thenReturn(lagFagsakMedRepresentantOrg(null));
        when(behandling.getBehandlingsgrunnlag()).thenReturn(lagBehandlingsgrunnlag(null, null));
        when(behandling.hentArbeidsforholdDokument()).thenReturn(lagArbeidsforholdDokument(null));

        List<Aktoer> arbeidsgivere = brevmottakerService.avklarMottakere(null, Mottaker.av(ARBEIDSGIVER), behandling, false, false);

        assertThat(arbeidsgivere).isEmpty();
    }

    @Test
    void avklarMottakere_medArbeidsgiverRolleIkkeKunAvklarteVirksomheter_girArbeidsgiverAktører() {
        when(behandling.getFagsak()).thenReturn(lagFagsakMedRepresentantOrg(null));
        when(behandling.getBehandlingsgrunnlag()).thenReturn(lagBehandlingsgrunnlag("987654321", null));
        when(behandling.hentArbeidsforholdDokument()).thenReturn(lagArbeidsforholdDokument("123456789"));

        List<Aktoer> arbeidsgivere = brevmottakerService.avklarMottakere(null, Mottaker.av(ARBEIDSGIVER), behandling, false, false);

        assertThat(arbeidsgivere)
            .flatExtracting(Aktoer::getOrgnr)
            .containsExactlyInAnyOrder("123456789", "987654321");
    }

    @Test
    void avklarMottakere_medBareUtenlandskeArbeidsgivereIkkeKunAvklarteVirksomheter_girIngenMottakere() {
        when(behandling.getFagsak()).thenReturn(lagFagsakMedRepresentantOrg(null));
        when(behandling.getBehandlingsgrunnlag()).thenReturn(lagBehandlingsgrunnlag(null, "uuid"));
        when(behandling.hentArbeidsforholdDokument()).thenReturn(lagArbeidsforholdDokument(null));

        List<Aktoer> arbeidsgivere = brevmottakerService.avklarMottakere(null, Mottaker.av(ARBEIDSGIVER), behandling, false, false);
        assertThat(arbeidsgivere).isEmpty();
    }

    @Test
    void avklarMottakere_medArbeidsgiverRolleOgRepresentantForBruker_girArbeidsgiverAktører() {
        when(avklarteVirksomheterService.hentNorskeArbeidsgivendeOrgnumre(behandling)).thenReturn(Sets.newHashSet("123456789", "987654321"));
        when(behandling.getFagsak()).thenReturn(lagFagsakMedRepresentantOrg(Representerer.BRUKER));

        List<Aktoer> arbeidsgivere = brevmottakerService.avklarMottakere(null, Mottaker.av(ARBEIDSGIVER), behandling);

        assertThat(arbeidsgivere)
            .flatExtracting(Aktoer::getOrgnr)
            .containsExactlyInAnyOrder("123456789", "987654321");
    }

    @Test
    void avklarMottakere_medArbeidsgiverRolleOgRepresentantForArbeidsgiver_girRepresentantAktør() {
        when(behandling.getFagsak()).thenReturn(lagFagsakMedRepresentantOrg(Representerer.ARBEIDSGIVER));

        List<Aktoer> arbeidsgivere = brevmottakerService.avklarMottakere(null, Mottaker.av(ARBEIDSGIVER), behandling);

        assertThat(arbeidsgivere)
            .flatExtracting(Aktoer::getAktørId, Aktoer::getPersonIdent, Aktoer::getOrgnr)
            .containsExactly(null, null, "REP-ORGNR");
    }

    @Test
    void avklarMottakere_medArbeidsgiverRolleOgRepresentantForBegge_girRepresentantAktør() {
        when(behandling.getFagsak()).thenReturn(lagFagsakMedRepresentantPerson(Representerer.BEGGE));

        List<Aktoer> arbeidsgivere = brevmottakerService.avklarMottakere(null, Mottaker.av(ARBEIDSGIVER), behandling);

        assertThat(arbeidsgivere)
            .flatExtracting(Aktoer::getAktørId, Aktoer::getPersonIdent, Aktoer::getOrgnr)
            .containsExactly(null, "REP-FNR", null);
    }

    @Test
    void avklarMottakere_art12_1_CZerReservertFraA1_forventerIngenAktør() {
        when(utenlandskMyndighetService.lagUtenlandskeMyndigheterFraBehandling(behandling)).thenReturn(Collections.singletonMap(lagUtenlandskMyndighet(), lagAktoerUtenlandskMyndighet()));
        when(behandlingsresultatService.hentBehandlingsresultat(anyLong())).thenReturn(behandlingsresultat);

        List<Aktoer> myndigheter = brevmottakerService.avklarMottakere(Produserbaredokumenter.ATTEST_A1, Mottaker.av(TRYGDEMYNDIGHET), behandling);
        assertThat(myndigheter).isEmpty();
    }

    @Test
    void avklarMottakere_art_11_4_2_CZerReservertFraA1_forventerIngenAktør() {
        when(utenlandskMyndighetService.lagUtenlandskeMyndigheterFraBehandling(behandling)).thenReturn(Collections.singletonMap(lagUtenlandskMyndighet(), lagAktoerUtenlandskMyndighet()));
        when(behandlingsresultatService.hentBehandlingsresultat(anyLong())).thenReturn(behandlingsresultat);

        behandlingsresultat.hentValidertLovvalgsperiode().setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_4_2);
        List<Aktoer> myndigheter = brevmottakerService.avklarMottakere(Produserbaredokumenter.ATTEST_A1, Mottaker.av(TRYGDEMYNDIGHET), behandling);
        assertThat(myndigheter).isEmpty();
    }

    @Test
    void avklarMottakere_A001_CZerReservertFraA1_forventerMyndighetAktør() {
        when(utenlandskMyndighetService.lagUtenlandskeMyndigheterFraBehandling(behandling)).thenReturn(Collections.singletonMap(lagUtenlandskMyndighet(), lagAktoerUtenlandskMyndighet()));

        List<Aktoer> myndigheter = brevmottakerService.avklarMottakere(Produserbaredokumenter.ANMODNING_UNNTAK, Mottaker.av(TRYGDEMYNDIGHET), behandling);
        assertThat(myndigheter)
            .flatExtracting(Aktoer::getInstitusjonId)
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

        verifyNoInteractions(trygdeavgiftsberegningService);
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

        verifyNoInteractions(trygdeavgiftsberegningService);
    }

    @Test
    void gittMangelbrevArbeidsgiver_skalHovedmottakerVæreArbeidsgiverMedKopi() {
        when(behandlingService.hentBehandlingMedSaksopplysninger(123L)).thenReturn(behandling);
        when(behandling.getFagsak()).thenReturn(lagFagsakMedRepresentantOrg(null));

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

        verify(trygdeavgiftsberegningService).finnBeregningsresultat(anyLong());
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
                List.of(SKATT)
            );

        verify(trygdeavgiftsberegningService).finnBeregningsresultat(anyLong());
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
                List.of(SKATT)
            );

        verify(trygdeavgiftsberegningService).finnBeregningsresultat(anyLong());
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
                List.of(SKATT)
            );

        verify(trygdeavgiftsberegningService).finnBeregningsresultat(anyLong());
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
                List.of(SKATT)
            );

        verify(trygdeavgiftsberegningService).finnBeregningsresultat(anyLong());
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

        verify(trygdeavgiftsberegningService).finnBeregningsresultat(anyLong());
    }

    @Test
    void gittInnvilgelsesbrevUK_skalHovedmottakerVæreBrukerMedKopier() {
        var lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_trygdeavtale_uk.UK_ART6_1);
        when(behandlingService.hentBehandlingMedSaksopplysninger(123L)).thenReturn(behandling);
        when(behandling.getFagsak()).thenReturn(lagFagsakMedRepresentantOrg(null));
        when(lovvalgsperiodeService.hentValidertLovvalgsperiode(anyLong())).thenReturn(lovvalgsperiode);

        assertThat(brevmottakerService.hentMottakerliste(STORBRITANNIA, 123))
            .extracting(
                Mottakerliste::getHovedMottaker,
                Mottakerliste::getKopiMottakere,
                Mottakerliste::getFasteMottakere
            )
            .containsExactly(
                BRUKER,
                List.of(ARBEIDSGIVER, TRYGDEMYNDIGHET),
                List.of(SKATT)
            );
    }

    @Test
    void gittInnvilgelsesbrevUKOgArt82_skalIkkeMyndighetFåKopi() {
        var lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_trygdeavtale_uk.UK_ART8_2);
        when(behandlingService.hentBehandlingMedSaksopplysninger(123L)).thenReturn(behandling);
        when(behandling.getFagsak()).thenReturn(lagFagsakMedRepresentantOrg(null));
        when(lovvalgsperiodeService.hentValidertLovvalgsperiode(anyLong())).thenReturn(lovvalgsperiode);

        assertThat(brevmottakerService.hentMottakerliste(STORBRITANNIA, 123))
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
        Kontaktopplysning kontaktopplysning = brevmottakerService.hentKontaktopplysning("MEL-123", lagAktoer(BRUKER));
        assertThat(kontaktopplysning).isNull();

        verifyNoInteractions(kontaktopplysningService);
    }

    @Test
    void hentKontaktopplysning_mottakerArbeidsgiver_returnererKontaktopplysning() {
        String orgnr = "987654321";
        when(kontaktopplysningService.hentKontaktopplysning(any(), eq(orgnr))).thenReturn(lagKontaktOpplysning(false));
        Aktoer aktoer = lagAktoer(ARBEIDSGIVER);
        aktoer.setOrgnr(orgnr);

        Kontaktopplysning kontaktopplysning = brevmottakerService.hentKontaktopplysning("MEL-123", aktoer);
        assertThat(kontaktopplysning).isNotNull();
        assertThat(kontaktopplysning.getKontaktNavn()).isEqualTo("Kari Arbeidsgiver");

        verify(kontaktopplysningService).hentKontaktopplysning("MEL-123", orgnr);
    }

    @Test
    void hentKontaktopplysning_mottakerRepresentant_returnererKontaktopplysning() {
        String orgnr = "123456789";
        when(kontaktopplysningService.hentKontaktopplysning(any(), eq(orgnr))).thenReturn(lagKontaktOpplysning(true));
        Aktoer aktoer = lagAktoer(REPRESENTANT);
        aktoer.setOrgnr(orgnr);

        Kontaktopplysning kontaktopplysning = brevmottakerService.hentKontaktopplysning("MEL-123", aktoer);
        assertThat(kontaktopplysning).isNotNull();
        assertThat(kontaktopplysning.getKontaktNavn()).isEqualTo("Ole Fullmektig");

        verify(kontaktopplysningService).hentKontaktopplysning("MEL-123", orgnr);
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

        assertThat(mottakerRolle).isEqualTo(TRYGDEMYNDIGHET);
    }

    @Test
    void avklarMottakerRolleFraDokument_UtenMapping_feiler() {
        assertThatThrownBy(() -> brevmottakerService.avklarMottakerRolleFraDokument(ORIENTERING_UTPEKING_UTLAND))
            .isInstanceOf(TekniskException.class)
            .hasMessage("Valg av mottakerRolle støttes ikke for ORIENTERING_UTPEKING_UTLAND");
    }

    private void initMocksForFtrlVedtaksbrev(Representerer representerer, long norskinntekt, boolean selvbetalende) {
        Optional<Trygdeavgiftsberegningsresultat> trygdeavgiftsberegningsresultat = Optional.of(
            new Trygdeavgiftsberegningsresultat(norskinntekt, null, lagAktoer(selvbetalende ? BRUKER : REPRESENTANT_TRYGDEAVGIFT), emptyList())
        );

        when(trygdeavgiftsberegningService.finnBeregningsresultat(anyLong())).thenReturn(trygdeavgiftsberegningsresultat);

        when(behandlingService.hentBehandlingMedSaksopplysninger(123L)).thenReturn(behandling);
        Fagsak fagsak = lagFagsakMedRepresentantOrg(representerer);
        fagsak.setType(Sakstyper.FTRL);
        when(behandling.getFagsak()).thenReturn(fagsak);
    }

    private Aktoer lagAktoer(Aktoersroller rolle) {
        var aktoer = new Aktoer();
        aktoer.setRolle(rolle);
        return aktoer;
    }

    private Fagsak lagFagsakMedRepresentantOrg(Representerer representerer) {
        Fagsak fagsak = new Fagsak();
        fagsak.getAktører().add(lagAktoer(BRUKER));

        if (representerer != null) {
            Aktoer representant = lagAktoer(REPRESENTANT);
            representant.setRepresenterer(representerer);
            representant.setOrgnr("REP-ORGNR");
            fagsak.getAktører().add(representant);
        }
        return fagsak;
    }

    private Fagsak lagFagsakMedRepresentantPerson(Representerer representerer) {
        Fagsak fagsak = new Fagsak();
        fagsak.getAktører().add(lagAktoer(BRUKER));

        if (representerer != null) {
            Aktoer representant = lagAktoer(REPRESENTANT);
            representant.setRepresenterer(representerer);
            representant.setPersonIdent("REP-FNR");
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
        Aktoer aktoer = lagAktoer(TRYGDEMYNDIGHET);
        aktoer.setInstitusjonId("CZ:SZUC10416");
        return aktoer;
    }


    private Optional<Kontaktopplysning> lagKontaktOpplysning(boolean representant) {
        Kontaktopplysning kontaktopplysning = new Kontaktopplysning();
        kontaktopplysning.setKontaktNavn(representant ? "Ole Fullmektig" : "Kari Arbeidsgiver");
        return Optional.of(kontaktopplysning);
    }
}
