package no.nav.melosys.service.brev;

import java.util.stream.Stream;

import no.finn.unleash.FakeUnleash;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.brev.Mottaker;
import no.nav.melosys.domain.brev.NorskMyndighet;
import no.nav.melosys.domain.kodeverk.LovvalgBestemmelse;
import no.nav.melosys.domain.kodeverk.Mottakerroller;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.dokument.BrevmottakerService;
import no.nav.melosys.service.dokument.DokgenService;
import no.nav.melosys.service.dokument.brev.mapper.DokumentproduksjonsInfoMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static no.nav.melosys.domain.kodeverk.Mottakerroller.*;
import static no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper.FØRSTEGANG;
import static no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper.NY_VURDERING;
import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.INNVILGELSE_YRKESAKTIV;
import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.TRYGDEAVTALE_GB;
import static no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_trygdeavtale_uk.UK_ART6_1;
import static no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_trygdeavtale_uk.UK_ART8_2;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DokumentNavnServiceTest {

    @Mock
    private BrevmottakerService brevmottakerService;
    @Mock
    private DokgenService dokgenService;
    @Mock
    private LovvalgsperiodeService lovvalgsperiodeService;
    private DokumentNavnService dokumentNavnService;

    @BeforeEach
    void setUp() {
        dokumentNavnService = new DokumentNavnService(brevmottakerService, dokgenService, lovvalgsperiodeService);
    }

    @Test
    void utledDokumentNavnForProduserbaredokumenterOgAktoerRolle_ikkeStorbritannia_forventetTittel() {
        Behandling behandling = new Behandling();
        behandling.setId(1L);


        String dokumentNavn = dokumentNavnService.utledDokumentNavnForProduserbaredokumenterOgMottakerrolle(behandling, INNVILGELSE_YRKESAKTIV, BRUKER);


        assertThat(dokumentNavn).isEqualTo("Innvilgelse yrkesaktiv");
    }

    @Test
    void utledDokumentNavnForProduserbaredokumenterOgAktoerRolle_ikkeStorbritannia_journalføringsTittel() {
        Behandling behandling = new Behandling();
        behandling.setId(1L);


        String dokumentNavn = dokumentNavnService.utledDokumentNavnForProduserbaredokumenterOgMottakerrolle(behandling, INNVILGELSE_YRKESAKTIV, BRUKER);


        assertThat(dokumentNavn).isEqualTo(INNVILGELSE_YRKESAKTIV.getBeskrivelse());
    }

    @ParameterizedTest
    @MethodSource("testparametre")
    void utledDokumentNavnForProduserbaredokumenterOgAktoerRolle_StorbritanniaInnvilgelseOgAttestMedUlikeParametre_korrektTittel(boolean skalHaAttest, boolean erNyVurdering, Mottaker mottaker, String forventetTittel) {
        Behandling behandling = new Behandling();
        behandling.setId(1L);
        behandling.setType(erNyVurdering ? NY_VURDERING : FØRSTEGANG);

        if (!mottaker.erUtenlandskMyndighet() && !NorskMyndighet.SKATTEETATEN.getOrgnr().equals(mottaker.getOrgnr())) {
            when(lovvalgsperiodeService.hentLovvalgsperiode(anyLong())).thenReturn(lagLovvalsperiode(skalHaAttest ? UK_ART6_1 : UK_ART8_2));
        }

        when(brevmottakerService.avklarMottaker(TRYGDEAVTALE_GB, Mottaker.av(mottaker.getRolle()), behandling)).thenReturn(mottaker);

        DokumentproduksjonsInfoMapper dokumentproduksjonsInfoMapper = new DokumentproduksjonsInfoMapper(new FakeUnleash());
        when(dokgenService.hentDokumentInfo(TRYGDEAVTALE_GB)).thenReturn(dokumentproduksjonsInfoMapper.hentDokumentproduksjonsInfo(TRYGDEAVTALE_GB));


        String dokumentNavn = dokumentNavnService.utledDokumentNavnForProduserbaredokumenterOgMottakerrolle(behandling, TRYGDEAVTALE_GB, mottaker.getRolle());


        assertThat(dokumentNavn).isEqualTo(forventetTittel);
    }

    @ParameterizedTest
    @MethodSource("testparametre")
    void utledDokumentNavnForProduserbaredokumenterOgAktoer_StorbritanniaInnvilgelseOgAttestMedUlikeParametre_korrektTittel(boolean skalHaAttest, boolean erNyVurdering, Mottaker mottaker, String forventetTittel) {
        Behandling behandling = new Behandling();
        behandling.setId(1L);
        behandling.setType(erNyVurdering ? NY_VURDERING : FØRSTEGANG);

        if (!mottaker.erUtenlandskMyndighet() && !NorskMyndighet.SKATTEETATEN.getOrgnr().equals(mottaker.getOrgnr())) {
            when(lovvalgsperiodeService.hentLovvalgsperiode(anyLong())).thenReturn(lagLovvalsperiode(skalHaAttest ? UK_ART6_1 : UK_ART8_2));
        }

        DokumentproduksjonsInfoMapper dokumentproduksjonsInfoMapper = new DokumentproduksjonsInfoMapper(new FakeUnleash());
        when(dokgenService.hentDokumentInfo(TRYGDEAVTALE_GB)).thenReturn(dokumentproduksjonsInfoMapper.hentDokumentproduksjonsInfo(TRYGDEAVTALE_GB));


        String dokumentNavn = dokumentNavnService.utledDokumentNavnForProduserbaredokumenterOgMottaker(behandling, TRYGDEAVTALE_GB, mottaker, null);


        assertThat(dokumentNavn).isEqualTo(forventetTittel);
    }

    @ParameterizedTest
    @MethodSource("testparametre")
    void utledDokumentNavn_StorbritanniaInnvilgelseOgAttestMedUlikeParametre_korrektTittel(boolean skalHaAttest, boolean erNyVurdering, Mottaker mottaker, String forventetTittel) {
        Behandling behandling = new Behandling();
        behandling.setId(123L);
        behandling.setType(erNyVurdering ? NY_VURDERING : FØRSTEGANG);

        if (!mottaker.erUtenlandskMyndighet() && !NorskMyndighet.SKATTEETATEN.getOrgnr().equals(mottaker.getOrgnr())) {
            when(lovvalgsperiodeService.hentLovvalgsperiode(anyLong())).thenReturn(lagLovvalsperiode(skalHaAttest ? UK_ART6_1 : UK_ART8_2));
        }

        DokumentproduksjonsInfoMapper dokumentproduksjonsInfoMapper = new DokumentproduksjonsInfoMapper(new FakeUnleash());


        String dokumentNavn = dokumentNavnService.utledDokumentNavn(behandling, dokumentproduksjonsInfoMapper.hentDokumentproduksjonsInfo(TRYGDEAVTALE_GB), mottaker);


        assertThat(dokumentNavn).isEqualTo(forventetTittel);
    }

    private static Stream<Arguments> testparametre() {
        return Stream.of(
            Arguments.of(true, false, lagMottaker(BRUKER, "1234", null, null), "Vedtak om medlemskap, Attest for utsendt arbeidstaker"),
            Arguments.of(false, false, lagMottaker(BRUKER, "1234", null, null), "Vedtak om medlemskap"),
            Arguments.of(true, false, lagMottaker(ARBEIDSGIVER, null, "1234", null), "Kopi av vedtak om medlemskap, Attest for utsendt arbeidstaker"),
            Arguments.of(false, false, lagMottaker(ARBEIDSGIVER, null, "1234", null), "Kopi av vedtak om medlemskap"),
            Arguments.of(false, false, lagMottaker(UTENLANDSK_TRYGDEMYNDIGHET, null, NorskMyndighet.SKATTEETATEN.getOrgnr(), null), "Kopi av vedtak om medlemskap"),
            Arguments.of(true, false, lagMottaker(UTENLANDSK_TRYGDEMYNDIGHET, null, null, "1234"), "Attest for utsendt arbeidstaker"),

            Arguments.of(true, true, lagMottaker(BRUKER, "1234", null, null), "Vedtak om medlemskap, Attest for utsendt arbeidstaker - endring"),
            Arguments.of(false, true, lagMottaker(BRUKER, "1234", null, null), "Vedtak om medlemskap - endring"),
            Arguments.of(true, true, lagMottaker(ARBEIDSGIVER, null, "1234", null), "Kopi av vedtak om medlemskap, Attest for utsendt arbeidstaker - endring"),
            Arguments.of(false, true, lagMottaker(ARBEIDSGIVER, null, "1234", null), "Kopi av vedtak om medlemskap - endring"),
            Arguments.of(false, true, lagMottaker(UTENLANDSK_TRYGDEMYNDIGHET, null, NorskMyndighet.SKATTEETATEN.getOrgnr(), null), "Kopi av vedtak om medlemskap - endring"),
            Arguments.of(true, true, lagMottaker(UTENLANDSK_TRYGDEMYNDIGHET, null, null, "1234"), "Attest for utsendt arbeidstaker - endring")
        );
    }

    private Lovvalgsperiode lagLovvalsperiode(LovvalgBestemmelse bestemmelse) {
        var lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setBestemmelse(bestemmelse);
        return lovvalgsperiode;
    }

    private static Mottaker lagMottaker(Mottakerroller rolle, String aktørID, String orgnr, String institusjonsID) {
        Mottaker mottaker = new Mottaker();
        mottaker.setRolle(rolle);
        mottaker.setAktørId(aktørID);
        mottaker.setOrgnr(orgnr);
        mottaker.setInstitusjonID(institusjonsID);
        return mottaker;
    }
}
