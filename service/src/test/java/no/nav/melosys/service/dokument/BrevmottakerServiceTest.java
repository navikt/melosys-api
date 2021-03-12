package no.nav.melosys.service.dokument;

import java.util.*;

import com.google.common.collect.Sets;
import no.nav.melosys.domain.*;
import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet;
import no.nav.melosys.domain.brev.Mottaker;
import no.nav.melosys.domain.behandlingsgrunnlag.data.ForetakUtland;
import no.nav.melosys.domain.brev.Mottakerliste;
import no.nav.melosys.domain.folketrygden.FastsattTrygdeavgift;
import no.nav.melosys.domain.folketrygden.MedlemAvFolketrygden;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Representerer;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.repository.MedlemAvFolketrygdenRepository;
import no.nav.melosys.service.aktoer.KontaktopplysningService;
import no.nav.melosys.service.aktoer.UtenlandskMyndighetService;
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static java.util.Collections.emptyList;
import static no.nav.melosys.domain.brev.FastMottaker.SKATT;
import static no.nav.melosys.domain.kodeverk.Aktoersroller.*;
import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class BrevmottakerServiceTest {
    @Mock
    private KontaktopplysningService kontaktopplysningService;
    @Mock
    private AvklarteVirksomheterService avklarteVirksomheterService;
    @Mock
    private UtenlandskMyndighetService utenlandskMyndighetService;
    @Mock
    private BehandlingsresultatService behandlingsresultatService;
    @Mock
    private Behandling behandling;
    @Mock
    private MedlemAvFolketrygdenRepository medlemAvFolketrygdenRepository;

    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    private Behandlingsresultat behandlingsresultat;
    private BrevmottakerService brevmottakerService;

    @Before
    public void setup() throws TekniskException, IkkeFunnetException {
        brevmottakerService = new BrevmottakerService(kontaktopplysningService, avklarteVirksomheterService,
            utenlandskMyndighetService, behandlingsresultatService, medlemAvFolketrygdenRepository);
        when(avklarteVirksomheterService.hentNorskeArbeidsgivendeOrgnumre(eq(behandling))).thenReturn(Sets.newHashSet("123456789", "987654321"));
        when(utenlandskMyndighetService.lagUtenlandskeMyndigheterFraBehandling(eq(behandling))).thenReturn(Collections.singletonMap(lagUtenlandskMyndighet(), lagAktoerUtenlandskMyndighet()));

        behandlingsresultat = new Behandlingsresultat();
        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1);
        behandlingsresultat.getLovvalgsperioder().add(lovvalgsperiode);
        when(behandlingsresultatService.hentBehandlingsresultat(anyLong())).thenReturn(behandlingsresultat);
    }

    @Test
    public void avklarMottakere_medArbeidsgiverRolleOgIngenArbeidsgivere_feiler() throws FunksjonellException, TekniskException {
        when(behandling.getFagsak()).thenReturn(lagFagsakMedRepresentant(null));
        when(avklarteVirksomheterService.hentNorskeArbeidsgivendeOrgnumre(eq(behandling))).thenReturn(Collections.emptySet());
        when(avklarteVirksomheterService.hentUtenlandskeVirksomheter(eq(behandling))).thenReturn(Collections.emptyList());
        expectedException.expect(FunksjonellException.class);

        brevmottakerService.avklarMottakere(null, Mottaker.av(ARBEIDSGIVER), behandling);
        expectedException.expectMessage("Arbeidsgiver er ikke registrert");
    }

    @Test
    public void avklarMottakere_medArbeidsgiverRolle_girArbeidsgiverAktører() throws FunksjonellException, TekniskException {
        when(behandling.getFagsak()).thenReturn(lagFagsakMedRepresentant(null));

        List<Aktoer> arbeidsgivere = brevmottakerService.avklarMottakere(null, Mottaker.av(ARBEIDSGIVER), behandling);

        assertThat(arbeidsgivere.stream()
            .map(Aktoer::getOrgnr))
            .containsExactlyInAnyOrder("123456789", "987654321");
    }

    @Test
    public void avklarMottakere_medBareUtenlandskeArbeidsgivere_girIngenMottakere() throws FunksjonellException, TekniskException {
        when(behandling.getFagsak()).thenReturn(lagFagsakMedRepresentant(null));
        when(avklarteVirksomheterService.hentNorskeArbeidsgivendeOrgnumre(eq(behandling))).thenReturn(Collections.emptySet());
        when(avklarteVirksomheterService.hentUtenlandskeVirksomheter(eq(behandling)))
            .thenReturn(Collections.singletonList(new AvklartVirksomhet(new ForetakUtland())));

        List<Aktoer> arbeidsgivere = brevmottakerService.avklarMottakere(null, Mottaker.av(ARBEIDSGIVER), behandling);
        assertThat(arbeidsgivere).isEmpty();
    }

    @Test
    public void avklarMottakere_medArbeidsgiverRolleOgRepresentantForBruker_girArbeidsgiverAktører() throws FunksjonellException, TekniskException {
        when(behandling.getFagsak()).thenReturn(lagFagsakMedRepresentant(Representerer.BRUKER));

        List<Aktoer> arbeidsgivere = brevmottakerService.avklarMottakere(null, Mottaker.av(ARBEIDSGIVER), behandling);

        assertThat(arbeidsgivere.stream()
            .map(Aktoer::getOrgnr))
            .containsExactlyInAnyOrder("123456789", "987654321");
    }

    @Test
    public void avklarMottakere_medArbeidsgiverRolleOgRepresentantForArbeidsgiver_girRepresentantAktør() throws FunksjonellException, TekniskException {
        when(behandling.getFagsak()).thenReturn(lagFagsakMedRepresentant(Representerer.ARBEIDSGIVER));

        List<Aktoer> arbeidsgivere = brevmottakerService.avklarMottakere(null, Mottaker.av(ARBEIDSGIVER), behandling);

        assertThat(arbeidsgivere.stream()
            .map(Aktoer::getOrgnr))
            .containsExactly("REP-ORGNR");
    }

    @Test
    public void avklarMottakere_medArbeidsgiverRolleOgRepresentantForBegge_girRepresentantAktør() throws FunksjonellException, TekniskException {
        when(behandling.getFagsak()).thenReturn(lagFagsakMedRepresentant(Representerer.BEGGE));

        List<Aktoer> arbeidsgivere = brevmottakerService.avklarMottakere(null, Mottaker.av(ARBEIDSGIVER), behandling);

        assertThat(arbeidsgivere.stream()
            .map(Aktoer::getOrgnr))
            .containsExactly("REP-ORGNR");
    }

    @Test
    public void avklarMottakere_art12_1_CZerReservertFraA1_forventerIngenAktør() throws FunksjonellException, TekniskException {
        List<Aktoer> myndigheter = brevmottakerService.avklarMottakere(Produserbaredokumenter.ATTEST_A1, Mottaker.av(MYNDIGHET), behandling);
        assertThat(myndigheter).isEmpty();
    }

    @Test
    public void avklarMottakere_art_11_4_2_CZerReservertFraA1_forventerIngenAktør() throws FunksjonellException, TekniskException {
        behandlingsresultat.hentValidertLovvalgsperiode().setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_4_2);
        List<Aktoer> myndigheter = brevmottakerService.avklarMottakere(Produserbaredokumenter.ATTEST_A1, Mottaker.av(MYNDIGHET), behandling);
        assertThat(myndigheter).isEmpty();
    }

    @Test
    public void avklarMottakere_A001_CZerReservertFraA1_forventerMyndighetAktør() throws FunksjonellException, TekniskException {
        List<Aktoer> myndigheter = brevmottakerService.avklarMottakere(Produserbaredokumenter.ANMODNING_UNNTAK, Mottaker.av(MYNDIGHET), behandling);
        assertThat(myndigheter.stream()
                .map(Aktoer::getInstitusjonId))
                .containsExactly("CZ:SZUC10416");
    }

    @Test
    public void gittMalIkkeRegistret_skalKasteFeil() {
        assertThatExceptionOfType(IkkeFunnetException.class)
            .isThrownBy(() -> brevmottakerService.hentMottakerliste(ATTEST_A1, behandling))
            .withMessage("Mangler mapping av mottakere for ATTEST_A1");
    }

    @Test
    public void gittForvaltningsmelding_skalHovedmottakerVæreBruker() throws Exception {
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

        verifyNoInteractions(medlemAvFolketrygdenRepository);
    }

    @Test
    public void gittMangelbrevBruker_skalHovedmottakerVæreBruker() throws Exception {
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

        verifyNoInteractions(medlemAvFolketrygdenRepository);
    }

    @Test
    public void gittMangelbrevArbeidsgiver_skalHovedmottakerVæreArbeidsgiverMedKopi() throws Exception {
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

        verifyNoInteractions(medlemAvFolketrygdenRepository);
    }

    @Test
    public void gittVedtakFtrl2_8UtenFullmektigIkkeSelvbetalende_skalHovedmottakerVæreBrukerMedKopier() throws Exception {
        initMocksForFtrlVedtaksbrev(null, ARBEIDSGIVER, 10000);

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

        verify(medlemAvFolketrygdenRepository).findByBehandlingsresultatId(anyLong());
    }

    @Test
    public void gittVedtakFtrl2_8UtenFullmektigSelvbetalende_skalHovedmottakerVæreBrukerMedKopier() throws Exception {
        initMocksForFtrlVedtaksbrev(null, BRUKER, 10000);

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

        verify(medlemAvFolketrygdenRepository).findByBehandlingsresultatId(anyLong());
    }

    @Test
    public void gittVedtakFtrl2_8FullmektigIkkeSelvbetalende_skalHovedmottakerVæreBrukerMedKopier() throws Exception {
        initMocksForFtrlVedtaksbrev(Representerer.BRUKER, ARBEIDSGIVER, 10000);

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

        verify(medlemAvFolketrygdenRepository).findByBehandlingsresultatId(anyLong());
    }

    @Test
    public void gittVedtakFtrl2_8FullmektigSelvbetalende_skalHovedmottakerVæreBrukerMedKopier() throws Exception {
        initMocksForFtrlVedtaksbrev(Representerer.BRUKER, BRUKER, 10000);

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

        verify(medlemAvFolketrygdenRepository).findByBehandlingsresultatId(anyLong());
    }

    @Test
    public void gittVedtakFtrl2_8FullmektigIkkeSelvbetalendeIkkeInntekt_skalHovedmottakerVæreBrukerMedKopier() throws Exception {
        initMocksForFtrlVedtaksbrev(Representerer.BRUKER, ARBEIDSGIVER, 0);

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

        verify(medlemAvFolketrygdenRepository).findByBehandlingsresultatId(anyLong());
    }

    private void initMocksForFtrlVedtaksbrev(Representerer representerer, Aktoersroller betaler, long norskinntekt) {
        when(medlemAvFolketrygdenRepository.findByBehandlingsresultatId(anyLong())).thenReturn(lagMedlemAvFolketrygden(betaler, norskinntekt));
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

    private Behandling lagBehandling(Sakstyper sakstype, Fagsak fagsak) {
        Behandling behandling = new Behandling();
        fagsak.setType(sakstype);
        behandling.setFagsak(fagsak);

        return behandling;
    }

    private Optional<MedlemAvFolketrygden> lagMedlemAvFolketrygden(Aktoersroller betaler, long norskinntekt) {
        MedlemAvFolketrygden medlemAvFolketrygden = new MedlemAvFolketrygden();
        FastsattTrygdeavgift fastsattTrygdeavgift = new FastsattTrygdeavgift();
        Aktoer betalesAv = new Aktoer();
        betalesAv.setRolle(betaler);

        fastsattTrygdeavgift.setBetalesAv(betalesAv);
        fastsattTrygdeavgift.setAvgiftspliktigNorskInntektMnd(norskinntekt);

        medlemAvFolketrygden.setFastsattTrygdeavgift(fastsattTrygdeavgift);
        return Optional.of(medlemAvFolketrygden);
    }
}
