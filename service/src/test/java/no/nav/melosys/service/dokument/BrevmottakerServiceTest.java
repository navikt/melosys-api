package no.nav.melosys.service.dokument;

import java.util.*;

import com.google.common.collect.Sets;
import no.nav.melosys.domain.*;
import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet;
import no.nav.melosys.domain.brev.Mottaker;
import no.nav.melosys.domain.brev.Mottakerliste;
import no.nav.melosys.domain.dokument.arbeidsforhold.Arbeidsforhold;
import no.nav.melosys.domain.dokument.arbeidsforhold.ArbeidsforholdDokument;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Fullmaktstype;
import no.nav.melosys.domain.kodeverk.Land_iso2;
import no.nav.melosys.domain.kodeverk.Mottakerroller;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.trygdeavtale.Lovvalgsbestemmelser_trygdeavtale_ca;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static java.util.Collections.emptyList;
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

    @BeforeEach
    void setup() {
        brevmottakerService = new BrevmottakerService(avklarteVirksomheterService, utenlandskMyndighetService, behandlingsresultatService, lovvalgsperiodeService);

        behandlingsresultat = new Behandlingsresultat();
        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1);
        behandlingsresultat.getLovvalgsperioder().add(lovvalgsperiode);
    }

    @Test
    void avklarMottakere_medFullmektigForArbeidsgiver_feiler() {
        when(behandling.getFagsak()).thenReturn(lagFagsakMedFullmektigPerson(Fullmaktstype.FULLMEKTIG_ARBEIDSGIVER));

        assertThatThrownBy(() -> brevmottakerService.avklarMottakere(null, Mottaker.medRolle(FULLMEKTIG), behandling))
            .isInstanceOf(FunksjonellException.class)
            .hasMessage("Finner ikke fullmektig for bruker");
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
        when(behandling.getFagsak()).thenReturn(FagsakTestFactory.lagFagsak());

        assertThatThrownBy(() -> brevmottakerService.avklarMottakere(null, Mottaker.medRolle(BRUKER), behandling))
            .isInstanceOf(FunksjonellException.class)
            .hasMessage("Bruker er ikke registrert.");
    }

    @Test
    void avklarMottakere_medBrukerRolleUtenFullmektig_girBrukerMottaker() {
        when(behandling.getFagsak()).thenReturn(lagFagsakMedBruker());

        List<Mottaker> mottakere = brevmottakerService.avklarMottakere(null, Mottaker.medRolle(BRUKER), behandling);

        assertThat(mottakere).hasSize(1);
        assertThat(mottakere.get(0).getRolle()).isEqualTo(BRUKER);
    }

    @Test
    void avklarMottakere_medBrukerRolleMedFullmektigOrg_girFullmektigMottaker() {
        when(behandling.getFagsak()).thenReturn(lagFagsakMedFullmektigOrg(Fullmaktstype.FULLMEKTIG_SØKNAD));

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
        when(behandling.getFagsak()).thenReturn(FagsakTestFactory.lagFagsak());

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> brevmottakerService.avklarMottakere(null, mottaker, behandling))
            .withMessageContaining("Virksomhet er ikke registrert.");
    }

    @Test
    void avklarMottakere_medVirksomhetRolleOgVirksomhet_girVirksomhetMottaker() {
        Aktoer virksomhet = new Aktoer();
        virksomhet.setRolle(Aktoersroller.VIRKSOMHET);
        virksomhet.setOrgnr("orgnr");
        var fagsak = FagsakTestFactory.builder().aktører(virksomhet).build();
        when(behandling.getFagsak()).thenReturn(fagsak);

        List<Mottaker> mottakere = brevmottakerService.avklarMottakere(null, Mottaker.medRolle(VIRKSOMHET), behandling);

        assertThat(mottakere)
            .hasSize(1)
            .contains(Mottaker.av(virksomhet));
    }

    @Test
    void avklarMottakere_medArbeidsgiverRolleOgIngenArbeidsgivere_feiler() {
        when(behandling.getFagsak()).thenReturn(lagFagsakMedBruker());
        when(avklarteVirksomheterService.hentNorskeArbeidsgivendeOrgnumre(behandling)).thenReturn(Collections.emptySet());
        when(avklarteVirksomheterService.hentUtenlandskeVirksomheter(behandling)).thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> brevmottakerService.avklarMottakere(null, Mottaker.medRolle(ARBEIDSGIVER), behandling))
            .isInstanceOf(FunksjonellException.class)
            .hasMessage("Arbeidsgiver er ikke registrert.");
    }

    @Test
    void avklarMottakere_medArbeidsgiverRolle_girArbeidsgiverMottakere() {
        when(avklarteVirksomheterService.hentNorskeArbeidsgivendeOrgnumre(behandling)).thenReturn(Sets.newHashSet("123456789", "987654321"));
        when(behandling.getFagsak()).thenReturn(lagFagsakMedBruker());

        List<Mottaker> arbeidsgivere = brevmottakerService.avklarMottakere(null, Mottaker.medRolle(ARBEIDSGIVER), behandling);

        assertThat(arbeidsgivere)
            .flatExtracting(Mottaker::getOrgnr)
            .containsExactlyInAnyOrder("123456789", "987654321");
    }

    @Test
    void avklarMottakere_medBareUtenlandskeArbeidsgivere_girIngenMottakere() {
        when(behandling.getFagsak()).thenReturn(lagFagsakMedBruker());
        when(avklarteVirksomheterService.hentNorskeArbeidsgivendeOrgnumre(behandling)).thenReturn(Collections.emptySet());
        when(avklarteVirksomheterService.hentUtenlandskeVirksomheter(behandling))
            .thenReturn(Collections.singletonList(new AvklartVirksomhet(new ForetakUtland())));

        List<Mottaker> arbeidsgivere = brevmottakerService.avklarMottakere(null, Mottaker.medRolle(ARBEIDSGIVER), behandling);
        assertThat(arbeidsgivere).isEmpty();
    }

    @Test
    void avklarMottakere_medArbeidsgiverRolleIkkeKunAvklarteVirksomheterOgIngenArbeidsgivere_girTomListe() {
        when(behandling.getFagsak()).thenReturn(lagFagsakMedBruker());
        when(behandling.getMottatteOpplysninger()).thenReturn(lagMottatteOpplysninger(null, null));
        when(behandling.finnArbeidsforholdDokument()).thenReturn(Optional.of(lagArbeidsforholdDokument(null)));

        List<Mottaker> arbeidsgivere = brevmottakerService.avklarMottakere(null, Mottaker.medRolle(ARBEIDSGIVER), behandling, false, false);

        assertThat(arbeidsgivere).isEmpty();
    }

    @Test
    void avklarMottakere_medArbeidsgiverRolleIkkeKunAvklarteVirksomheter_girArbeidsgiverMottakere() {
        when(behandling.getFagsak()).thenReturn(lagFagsakMedBruker());
        when(behandling.getMottatteOpplysninger()).thenReturn(lagMottatteOpplysninger("987654321", null));
        when(behandling.finnArbeidsforholdDokument()).thenReturn(Optional.of(lagArbeidsforholdDokument("123456789")));

        List<Mottaker> arbeidsgivere = brevmottakerService.avklarMottakere(null, Mottaker.medRolle(ARBEIDSGIVER), behandling, false, false);

        assertThat(arbeidsgivere)
            .flatExtracting(Mottaker::getOrgnr)
            .containsExactlyInAnyOrder("123456789", "987654321");
    }

    @Test
    void avklarMottakere_medBareUtenlandskeArbeidsgivereIkkeKunAvklarteVirksomheter_girIngenMottakere() {
        when(behandling.getFagsak()).thenReturn(lagFagsakMedBruker());
        when(behandling.getMottatteOpplysninger()).thenReturn(lagMottatteOpplysninger(null, "uuid"));
        when(behandling.finnArbeidsforholdDokument()).thenReturn(Optional.of(lagArbeidsforholdDokument(null)));

        List<Mottaker> arbeidsgivere = brevmottakerService.avklarMottakere(null, Mottaker.medRolle(ARBEIDSGIVER), behandling, false, false);
        assertThat(arbeidsgivere).isEmpty();
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

    @Test
    void avklarMottakere_medArbeidsgiverRolleOgFullmektigForArbeidsgiver_girFullmektigMottakere() {
        when(behandling.getFagsak()).thenReturn(lagFagsakMedFullmektigOrg(Fullmaktstype.FULLMEKTIG_ARBEIDSGIVER));

        List<Mottaker> arbeidsgivere = brevmottakerService.avklarMottakere(null, Mottaker.medRolle(ARBEIDSGIVER), behandling);

        assertThat(arbeidsgivere)
            .flatExtracting(Mottaker::getAktørId, Mottaker::getPersonIdent, Mottaker::getOrgnr)
            .containsExactly(null, null, "REP-ORGNR");
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
                Mottakerliste::getKopiMottakere,
                Mottakerliste::getFasteMottakere
            )
            .containsExactly(
                BRUKER,
                List.of(ARBEIDSGIVER, UTENLANDSK_TRYGDEMYNDIGHET),
                emptyList()
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
                Mottakerliste::getKopiMottakere,
                Mottakerliste::getFasteMottakere
            )
            .containsExactly(
                BRUKER,
                List.of(ARBEIDSGIVER),
                emptyList()
            );
    }

    @Test
    void gittInnvilgelsesbrevCANogArt6_2_skalIkkeArbeidsgiverFåKopi() {
        var lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setBestemmelse(Lovvalgsbestemmelser_trygdeavtale_ca.CAN_ART6_2);
        when(lovvalgsperiodeService.hentLovvalgsperiode(anyLong())).thenReturn(lovvalgsperiode);
        when(lovvalgsperiodeService.harSelvstendigNæringsdrivendeLovvalgsbestemmelse(anyLong())).thenReturn(true);

        assertThat(brevmottakerService.hentMottakerliste(TRYGDEAVTALE_GB, 123))
            .extracting(
                Mottakerliste::getHovedMottaker,
                Mottakerliste::getKopiMottakere,
                Mottakerliste::getFasteMottakere
            )
            .containsExactly(
                BRUKER,
                List.of(UTENLANDSK_TRYGDEMYNDIGHET),
                emptyList()
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

    private Fagsak lagFagsakMedBruker() {
        return FagsakTestFactory.builder().medBruker().build();
    }

    private Fagsak lagFagsakMedFullmektigOrg(Fullmaktstype fullmaktstype) {
        Fagsak fagsak = lagFagsakMedBruker();
        Aktoer aktoer = new Aktoer();
        aktoer.setRolle(Aktoersroller.FULLMEKTIG);
        aktoer.setFullmaktstype(fullmaktstype);
        aktoer.setOrgnr("REP-ORGNR");
        fagsak.leggTilAktør(aktoer);
        return fagsak;
    }

    private Fagsak lagFagsakMedFullmektigPerson(Fullmaktstype fullmaktstype) {
        Fagsak fagsak = lagFagsakMedBruker();
        Aktoer aktoer = new Aktoer();
        aktoer.setRolle(Aktoersroller.FULLMEKTIG);
        aktoer.setFullmaktstype(fullmaktstype);
        aktoer.setPersonIdent("REP-FNR");
        fagsak.leggTilAktør(aktoer);
        return fagsak;
    }

    private MottatteOpplysninger lagMottatteOpplysninger(String ekstraArbeidsgivereOrgnr, String foretakUtlandUuid) {
        var mottatteOpplysningerData = new MottatteOpplysningerData();
        mottatteOpplysningerData.juridiskArbeidsgiverNorge.getEkstraArbeidsgivere().add(ekstraArbeidsgivereOrgnr);
        var foretakUtland = new ForetakUtland();
        foretakUtland.setUuid(foretakUtlandUuid);
        mottatteOpplysningerData.foretakUtland.add(foretakUtland);
        var mottatteOpplysninger = new MottatteOpplysninger();
        mottatteOpplysninger.setMottatteOpplysningerData(mottatteOpplysningerData);
        return mottatteOpplysninger;
    }

    private ArbeidsforholdDokument lagArbeidsforholdDokument(String arbeidsgiverIDOrgNr) {
        var arbeidsforhold = new Arbeidsforhold();
        arbeidsforhold.setArbeidsgiverID(arbeidsgiverIDOrgNr);
        var arbeidsforholdDokument = new ArbeidsforholdDokument();
        arbeidsforholdDokument.arbeidsforhold.add(arbeidsforhold);
        return arbeidsforholdDokument;
    }

    private UtenlandskMyndighet lagUtenlandskMyndighet() {
        UtenlandskMyndighet utenlandskMyndighet = new UtenlandskMyndighet();
        utenlandskMyndighet.setLandkode(Land_iso2.CZ);
        utenlandskMyndighet.setInstitusjonskode("SZUC10416");
        utenlandskMyndighet.setPostnummer("123");
        var preferanser = new HashSet<Preferanse>();
        preferanser.add(new Preferanse(1L, Preferanse.PreferanseEnum.RESERVERT_FRA_A1));
        utenlandskMyndighet.setPreferanser(preferanser);

        return utenlandskMyndighet;
    }

    private Mottaker lagMottakerUtenlandskMyndighet() {
        Mottaker mottaker = Mottaker.medRolle(UTENLANDSK_TRYGDEMYNDIGHET);
        mottaker.setInstitusjonID("CZ:SZUC10416");
        return mottaker;
    }
}
