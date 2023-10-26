package no.nav.melosys.service.dokument;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.Sets;
import io.getunleash.FakeUnleash;
import no.nav.melosys.domain.*;
import no.nav.melosys.domain.avgift.Inntektsperiode;
import no.nav.melosys.domain.avgift.Trygdeavgiftsgrunnlag;
import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode;
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
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BrevmottakerServiceTest {
    @Mock
    private AvklarteVirksomheterService avklarteVirksomheterService;
    @Mock
    private UtenlandskMyndighetService utenlandskMyndighetService;
    @Mock
    private BehandlingsresultatService behandlingsresultatService;
    @Mock
    private LovvalgsperiodeService lovvalgsperiodeService;
    @Mock
    private Behandling behandling;

    private Behandlingsresultat behandlingsresultat;
    private BrevmottakerService brevmottakerService;
    private FakeUnleash fakeUnleash = new FakeUnleash();

    @BeforeEach
    void setup() {
        fakeUnleash.enableAll();
        brevmottakerService = new BrevmottakerService(
            avklarteVirksomheterService, utenlandskMyndighetService, behandlingsresultatService, lovvalgsperiodeService, fakeUnleash);

        behandlingsresultat = new Behandlingsresultat();
        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1);
        behandlingsresultat.getLovvalgsperioder().add(lovvalgsperiode);
    }

    @Disabled("Etter fjerning av toggle melosys.fullmakt.trygdeavgift")
    @Test
    void avklarMottakere_medFullmektigRepresentererOrg_feiler() {
        when(behandling.getFagsak()).thenReturn(lagFagsakMedFullmektigPerson(Representerer.ARBEIDSGIVER));

        assertThatThrownBy(() -> brevmottakerService.avklarMottakere(null, Mottaker.medRolle(FULLMEKTIG), behandling))
            .isInstanceOf(FunksjonellException.class)
            .hasMessage("Finner ikke fullmektig for bruker");
    }

    @Test
    void avklarMottakere_medFullmektigForArbeidsgiver_feiler() {
        when(behandling.getFagsak()).thenReturn(lagFagsakMedFullmektigPerson(Fullmaktstype.FULLMEKTIG_ARBEIDSGIVER));

        assertThatThrownBy(() -> brevmottakerService.avklarMottakere(null, Mottaker.medRolle(FULLMEKTIG), behandling))
            .isInstanceOf(FunksjonellException.class)
            .hasMessage("Finner ikke fullmektig for bruker");
    }

    @Disabled("Etter fjerning av toggle melosys.fullmakt.trygdeavgift")
    @Test
    void avklarMottakere_medFullmektigRepresentererBruker_girFullmektigMottaker() {
        when(behandling.getFagsak()).thenReturn(lagFagsakMedFullmektigPerson(Representerer.BRUKER));

        List<Mottaker> mottakere = brevmottakerService.avklarMottakere(null, Mottaker.medRolle(BRUKER), behandling);

        assertThat(mottakere).hasSize(1);
        assertThat(mottakere.get(0).getRolle()).isEqualTo(FULLMEKTIG);
    }

    @Test
    void avklarMottakere_medFullmektigForBruker_girFullmektigMottaker() {
        when(behandling.getFagsak()).thenReturn(lagFagsakMedFullmektigPerson(Fullmaktstype.FULLMEKTIG_SØKNAD));

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
        when(behandling.getFagsak()).thenReturn(lagFagsak());

        List<Mottaker> mottakere = brevmottakerService.avklarMottakere(null, Mottaker.medRolle(BRUKER), behandling);

        assertThat(mottakere).hasSize(1);
        assertThat(mottakere.get(0).getRolle()).isEqualTo(BRUKER);
    }

    @Disabled("Etter fjerning av toggle melosys.fullmakt.trygdeavgift")
    @Test
    void avklarMottakere_medBrukerRolleMedRepresentantOrg_girFullmektigMottaker() {
        when(behandling.getFagsak()).thenReturn(lagFagsakMedFullmektigOrg(Representerer.BRUKER));

        List<Mottaker> mottakere = brevmottakerService.avklarMottakere(null, Mottaker.medRolle(BRUKER), behandling);

        assertThat(mottakere).hasSize(1);
        assertThat(mottakere.get(0).getRolle()).isEqualTo(FULLMEKTIG);
    }

    @Test
    void avklarMottakere_medBrukerRolleMedFullmektigOrg_girFullmektigMottaker() {
        when(behandling.getFagsak()).thenReturn(lagFagsakMedFullmektigOrg(Fullmaktstype.FULLMEKTIG_SØKNAD));

        List<Mottaker> mottakere = brevmottakerService.avklarMottakere(null, Mottaker.medRolle(BRUKER), behandling);

        assertThat(mottakere).hasSize(1);
        assertThat(mottakere.get(0).getRolle()).isEqualTo(FULLMEKTIG);
    }

    @Disabled("Etter fjerning av toggle melosys.fullmakt.trygdeavgift")
    @Test
    void avklarMottakere_medBrukerRolleMedPersonRepresentant_girFullmektigMottaker() {
        when(behandling.getFagsak()).thenReturn(lagFagsakMedFullmektigPerson(Representerer.BRUKER));

        List<Mottaker> mottakere = brevmottakerService.avklarMottakere(null, Mottaker.medRolle(BRUKER), behandling);

        assertThat(mottakere).hasSize(1);
        assertThat(mottakere.get(0).getRolle()).isEqualTo(FULLMEKTIG);
    }

    @Test
    void avklarMottakere_medBrukerRolleMedFullmektigPerson_girFullmektigMottaker() {
        when(behandling.getFagsak()).thenReturn(lagFagsakMedFullmektigPerson(Fullmaktstype.FULLMEKTIG_SØKNAD));

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
        when(behandling.getFagsak()).thenReturn(lagFagsak());
        when(avklarteVirksomheterService.hentNorskeArbeidsgivendeOrgnumre(behandling)).thenReturn(Collections.emptySet());
        when(avklarteVirksomheterService.hentUtenlandskeVirksomheter(behandling)).thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> brevmottakerService.avklarMottakere(null, Mottaker.medRolle(ARBEIDSGIVER), behandling))
            .isInstanceOf(FunksjonellException.class)
            .hasMessage("Arbeidsgiver er ikke registrert.");
    }

    @Test
    void avklarMottakere_medArbeidsgiverRolle_girArbeidsgiverMottakere() {
        when(avklarteVirksomheterService.hentNorskeArbeidsgivendeOrgnumre(behandling)).thenReturn(Sets.newHashSet("123456789", "987654321"));
        when(behandling.getFagsak()).thenReturn(lagFagsak());

        List<Mottaker> arbeidsgivere = brevmottakerService.avklarMottakere(null, Mottaker.medRolle(ARBEIDSGIVER), behandling);

        assertThat(arbeidsgivere)
            .flatExtracting(Mottaker::getOrgnr)
            .containsExactlyInAnyOrder("123456789", "987654321");
    }

    @Test
    void avklarMottakere_medBareUtenlandskeArbeidsgivere_girIngenMottakere() {
        when(behandling.getFagsak()).thenReturn(lagFagsak());
        when(avklarteVirksomheterService.hentNorskeArbeidsgivendeOrgnumre(behandling)).thenReturn(Collections.emptySet());
        when(avklarteVirksomheterService.hentUtenlandskeVirksomheter(behandling))
            .thenReturn(Collections.singletonList(new AvklartVirksomhet(new ForetakUtland())));

        List<Mottaker> arbeidsgivere = brevmottakerService.avklarMottakere(null, Mottaker.medRolle(ARBEIDSGIVER), behandling);
        assertThat(arbeidsgivere).isEmpty();
    }

    @Test
    void avklarMottakere_medArbeidsgiverRolleIkkeKunAvklarteVirksomheterOgIngenArbeidsgivere_girTomListe() {
        when(behandling.getFagsak()).thenReturn(lagFagsak());
        when(behandling.getMottatteOpplysninger()).thenReturn(lagMottatteOpplysninger(null, null));
        when(behandling.finnArbeidsforholdDokument()).thenReturn(Optional.of(lagArbeidsforholdDokument(null)));

        List<Mottaker> arbeidsgivere = brevmottakerService.avklarMottakere(null, Mottaker.medRolle(ARBEIDSGIVER), behandling, false, false);

        assertThat(arbeidsgivere).isEmpty();
    }

    @Test
    void avklarMottakere_medArbeidsgiverRolleIkkeKunAvklarteVirksomheter_girArbeidsgiverMottakere() {
        when(behandling.getFagsak()).thenReturn(lagFagsak());
        when(behandling.getMottatteOpplysninger()).thenReturn(lagMottatteOpplysninger("987654321", null));
        when(behandling.finnArbeidsforholdDokument()).thenReturn(Optional.of(lagArbeidsforholdDokument("123456789")));

        List<Mottaker> arbeidsgivere = brevmottakerService.avklarMottakere(null, Mottaker.medRolle(ARBEIDSGIVER), behandling, false, false);

        assertThat(arbeidsgivere)
            .flatExtracting(Mottaker::getOrgnr)
            .containsExactlyInAnyOrder("123456789", "987654321");
    }

    @Test
    void avklarMottakere_medBareUtenlandskeArbeidsgivereIkkeKunAvklarteVirksomheter_girIngenMottakere() {
        when(behandling.getFagsak()).thenReturn(lagFagsak());
        when(behandling.getMottatteOpplysninger()).thenReturn(lagMottatteOpplysninger(null, "uuid"));
        when(behandling.finnArbeidsforholdDokument()).thenReturn(Optional.of(lagArbeidsforholdDokument(null)));

        List<Mottaker> arbeidsgivere = brevmottakerService.avklarMottakere(null, Mottaker.medRolle(ARBEIDSGIVER), behandling, false, false);
        assertThat(arbeidsgivere).isEmpty();
    }

    @Disabled("Etter fjerning av toggle melosys.fullmakt.trygdeavgift")
    @Test
    void avklarMottakere_medArbeidsgiverRolleOgRepresentantForBruker_girArbeidsgiverMottakere() {
        when(avklarteVirksomheterService.hentNorskeArbeidsgivendeOrgnumre(behandling)).thenReturn(Sets.newHashSet("123456789", "987654321"));
        when(behandling.getFagsak()).thenReturn(lagFagsakMedFullmektigOrg(Representerer.BRUKER));

        List<Mottaker> arbeidsgivere = brevmottakerService.avklarMottakere(null, Mottaker.medRolle(ARBEIDSGIVER), behandling);

        assertThat(arbeidsgivere)
            .flatExtracting(Mottaker::getOrgnr)
            .containsExactlyInAnyOrder("123456789", "987654321");
    }

    @Test
    void avklarMottakere_medArbeidsgiverRolleOgFullmektigForBruker_girArbeidsgiverMottakere() {
        when(avklarteVirksomheterService.hentNorskeArbeidsgivendeOrgnumre(behandling)).thenReturn(Sets.newHashSet("123456789", "987654321"));
        when(behandling.getFagsak()).thenReturn(lagFagsakMedFullmektigOrg(Fullmaktstype.FULLMEKTIG_SØKNAD));

        List<Mottaker> arbeidsgivere = brevmottakerService.avklarMottakere(null, Mottaker.medRolle(ARBEIDSGIVER), behandling);

        assertThat(arbeidsgivere)
            .flatExtracting(Mottaker::getOrgnr)
            .containsExactlyInAnyOrder("123456789", "987654321");
    }

    @Disabled("Etter fjerning av toggle melosys.fullmakt.trygdeavgift")
    @Test
    void avklarMottakere_medArbeidsgiverRolleOgRepresentantForArbeidsgiver_girFullmektigMottakere() {
        when(behandling.getFagsak()).thenReturn(lagFagsakMedFullmektigOrg(Representerer.ARBEIDSGIVER));

        List<Mottaker> arbeidsgivere = brevmottakerService.avklarMottakere(null, Mottaker.medRolle(ARBEIDSGIVER), behandling);

        assertThat(arbeidsgivere)
            .flatExtracting(Mottaker::getAktørId, Mottaker::getPersonIdent, Mottaker::getOrgnr)
            .containsExactly(null, null, "REP-ORGNR");
    }

    @Test
    void avklarMottakere_medArbeidsgiverRolleOgFullmektigForArbeidsgiver_girFullmektigMottakere() {
        when(behandling.getFagsak()).thenReturn(lagFagsakMedFullmektigOrg(Fullmaktstype.FULLMEKTIG_ARBEIDSGIVER));

        List<Mottaker> arbeidsgivere = brevmottakerService.avklarMottakere(null, Mottaker.medRolle(ARBEIDSGIVER), behandling);

        assertThat(arbeidsgivere)
            .flatExtracting(Mottaker::getAktørId, Mottaker::getPersonIdent, Mottaker::getOrgnr)
            .containsExactly(null, null, "REP-ORGNR");
    }

    @Disabled("Etter fjerning av toggle melosys.fullmakt.trygdeavgift")
    @Test
    void avklarMottakere_medArbeidsgiverRolleOgRepresentantForBegge_girFullmektigMottakere() {
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
        assertThat(brevmottakerService.hentMottakerliste(MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD, 123))
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

        verifyNoInteractions(behandlingsresultatService);
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

        verifyNoInteractions(behandlingsresultatService);
    }

    @Test
    void gittMangelbrevArbeidsgiver_skalHovedmottakerVæreArbeidsgiverMedKopi() {
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
    }

    @Test
    void gittVedtakFtrl2_8_skalHovedmottakerVæreBrukerMedKopier() {
        assertThat(brevmottakerService.hentMottakerliste(INNVILGELSE_FOLKETRYGDLOVEN, 123L))
            .isNotNull()
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
    }

    @Test
    void gittInnvilgelsesbrevUK_skalHovedmottakerVæreBrukerMedKopier() {
        var lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setBestemmelse(Lovvalgsbestemmelser_trygdeavtale_gb.UK_ART6_1);
        when(lovvalgsperiodeService.hentLovvalgsperiode(anyLong())).thenReturn(lovvalgsperiode);

        assertThat(brevmottakerService.hentMottakerliste(TRYGDEAVTALE_GB, 123))
            .extracting(
                Mottakerliste::getHovedMottaker,
                Mottakerliste::getKopiMottakere
            )
            .containsExactly(
                BRUKER,
                List.of(ARBEIDSGIVER, UTENLANDSK_TRYGDEMYNDIGHET)
            );
    }

    @Test
    void gittInnvilgelsesbrevUKOgArt82_skalIkkeMyndighetFåKopi() {
        var lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setBestemmelse(Lovvalgsbestemmelser_trygdeavtale_gb.UK_ART8_2);
        when(lovvalgsperiodeService.hentLovvalgsperiode(anyLong())).thenReturn(lovvalgsperiode);

        assertThat(brevmottakerService.hentMottakerliste(TRYGDEAVTALE_GB, 123))
            .extracting(
                Mottakerliste::getHovedMottaker,
                Mottakerliste::getKopiMottakere
            )
            .containsExactly(
                BRUKER,
                List.of(ARBEIDSGIVER)
            );
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

    private Aktoer lagAktoer(Aktoersroller rolle) {
        var aktoer = new Aktoer();
        aktoer.setRolle(rolle);
        return aktoer;
    }

    private Fagsak lagFagsak() {
        Fagsak fagsak = new Fagsak();
        fagsak.getAktører().add(lagAktoer(Aktoersroller.BRUKER));
        return fagsak;
    }

    private Fagsak lagFagsakMedFullmektigOrg(Representerer representerer) {
        Fagsak fagsak = lagFagsak();
        Aktoer aktoer = lagAktoer(Aktoersroller.REPRESENTANT);
        aktoer.setRepresenterer(representerer);
        aktoer.setOrgnr("REP-ORGNR");
        fagsak.getAktører().add(aktoer);
        return fagsak;
    }

    private Fagsak lagFagsakMedFullmektigPerson(Representerer representerer) {
        var fagsak = lagFagsak();
        Aktoer aktoer = lagAktoer(Aktoersroller.REPRESENTANT);
        aktoer.setRepresenterer(representerer);
        aktoer.setPersonIdent("REP-FNR");
        fagsak.getAktører().add(aktoer);
        return fagsak;
    }

    private Fagsak lagFagsakMedFullmektigOrg(Fullmaktstype fullmaktstype) {
        Fagsak fagsak = lagFagsak();
        Aktoer aktoer = lagAktoer(Aktoersroller.FULLMEKTIG);
        aktoer.setFullmaktstype(fullmaktstype);
        aktoer.setOrgnr("REP-ORGNR");
        fagsak.getAktører().add(aktoer);
        return fagsak;
    }

    private Fagsak lagFagsakMedFullmektigPerson(Fullmaktstype fullmaktstype) {
        var fagsak = lagFagsak();
        Aktoer aktoer = lagAktoer(Aktoersroller.FULLMEKTIG);
        aktoer.setFullmaktstype(fullmaktstype);
        aktoer.setPersonIdent("REP-FNR");
        fagsak.getAktører().add(aktoer);
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
