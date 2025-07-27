package no.nav.melosys.service.brev;

import java.util.stream.Stream;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.BehandlingTestBuilder;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.brev.Mottaker;
import no.nav.melosys.domain.kodeverk.LovvalgBestemmelse;
import no.nav.melosys.domain.kodeverk.Mottakerroller;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.dokument.BrevmottakerService;
import no.nav.melosys.service.dokument.DokgenService;
import no.nav.melosys.service.dokument.brev.mapper.DokumentproduksjonsInfoMapper;
import no.nav.melosys.service.ftrl.medlemskapsperiode.MedlemskapsperiodeService;
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
import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.INNVILGELSE_FOLKETRYGDLOVEN;
import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.TRYGDEAVTALE_GB;
import static no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.trygdeavtale.Lovvalgsbestemmelser_trygdeavtale_gb.UK_ART6_1;
import static no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.trygdeavtale.Lovvalgsbestemmelser_trygdeavtale_gb.UK_ART8_2;
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
    @Mock
    private MedlemskapsperiodeService medlemskapsperiodeService;

    private final DokumentproduksjonsInfoMapper dokumentproduksjonsInfoMapper = new DokumentproduksjonsInfoMapper();

    private DokumentNavnService dokumentNavnService;

    @BeforeEach
    void setUp() {
        dokumentNavnService = new DokumentNavnService(brevmottakerService, dokgenService, lovvalgsperiodeService, medlemskapsperiodeService);

    }

    @Test
    void utledDokumentNavnForProduserbartdokumentOgMottakerrolle_ikkeStorbritannia_forventetTittel() {
        var behandling = lagBehandling();
        when(dokgenService.hentDokumentInfo(INNVILGELSE_FOLKETRYGDLOVEN)).thenReturn(dokumentproduksjonsInfoMapper.hentDokumentproduksjonsInfo(INNVILGELSE_FOLKETRYGDLOVEN));


        String dokumentNavn = dokumentNavnService.utledDokumentNavnForProduserbartdokumentOgMottakerrolle(behandling, INNVILGELSE_FOLKETRYGDLOVEN, BRUKER);


        assertThat(dokumentNavn).isEqualTo(INNVILGELSE_FOLKETRYGDLOVEN.getBeskrivelse());
    }

    @ParameterizedTest
    @MethodSource("testparametre")
    void utledDokumentNavnForProduserbartdokumentOgMottakerrolle_StorbritanniaInnvilgelseOgAttestMedUlikeParametre_korrektTittel(boolean skalHaAttest, boolean erNyVurdering, Mottaker mottaker, String forventetTittel) {
        var behandling = lagBehandling();
        behandling.setType(erNyVurdering ? NY_VURDERING : FØRSTEGANG);

        if (!mottaker.erUtenlandskMyndighet()) {
            when(lovvalgsperiodeService.hentLovvalgsperiode(anyLong())).thenReturn(lagLovvalsperiode(skalHaAttest ? UK_ART6_1 : UK_ART8_2));
        }
        when(brevmottakerService.avklarMottaker(TRYGDEAVTALE_GB, Mottaker.medRolle(mottaker.getRolle()), behandling)).thenReturn(mottaker);
        when(dokgenService.hentDokumentInfo(TRYGDEAVTALE_GB)).thenReturn(dokumentproduksjonsInfoMapper.hentDokumentproduksjonsInfo(TRYGDEAVTALE_GB));


        String dokumentNavn = dokumentNavnService.utledDokumentNavnForProduserbartdokumentOgMottakerrolle(behandling, TRYGDEAVTALE_GB, mottaker.getRolle());


        assertThat(dokumentNavn).isEqualTo(forventetTittel);
    }

    @ParameterizedTest
    @MethodSource("testparametre")
    void utledDokumentNavnForProduserbartdokumentOgMottaker_StorbritanniaInnvilgelseOgAttestMedUlikeParametre_korrektTittel(boolean skalHaAttest, boolean erNyVurdering, Mottaker mottaker, String forventetTittel) {
        var behandling = lagBehandling();
        behandling.setType(erNyVurdering ? NY_VURDERING : FØRSTEGANG);

        if (!mottaker.erUtenlandskMyndighet()) {
            when(lovvalgsperiodeService.hentLovvalgsperiode(anyLong())).thenReturn(lagLovvalsperiode(skalHaAttest ? UK_ART6_1 : UK_ART8_2));
        }
        when(dokgenService.hentDokumentInfo(TRYGDEAVTALE_GB)).thenReturn(dokumentproduksjonsInfoMapper.hentDokumentproduksjonsInfo(TRYGDEAVTALE_GB));


        String dokumentNavn = dokumentNavnService.utledDokumentNavnForProduserbartdokumentOgMottaker(behandling, TRYGDEAVTALE_GB, mottaker, "");


        assertThat(dokumentNavn).isEqualTo(forventetTittel);
    }

    @ParameterizedTest
    @MethodSource("testparametre")
    void utledDokumentNavn_StorbritanniaInnvilgelseOgAttestMedUlikeParametre_korrektTittel(boolean skalHaAttest, boolean erNyVurdering, Mottaker mottaker, String forventetTittel) {
        var behandling = lagBehandling();
        behandling.setType(erNyVurdering ? NY_VURDERING : FØRSTEGANG);

        if (!mottaker.erUtenlandskMyndighet()) {
            when(lovvalgsperiodeService.hentLovvalgsperiode(anyLong())).thenReturn(lagLovvalsperiode(skalHaAttest ? UK_ART6_1 : UK_ART8_2));
        }

        String dokumentNavn = dokumentNavnService.utledTittel(behandling, TRYGDEAVTALE_GB, dokumentproduksjonsInfoMapper.hentDokumentproduksjonsInfo(TRYGDEAVTALE_GB), mottaker, null, null, null);


        assertThat(dokumentNavn).isEqualTo(forventetTittel);
    }

    private static Stream<Arguments> testparametre() {
        return Stream.of(
            Arguments.of(true, false, lagMottaker(BRUKER, "1234", null, null), "Vedtak om medlemskap, Attest for medlemskap i folketrygden"),
            Arguments.of(false, false, lagMottaker(BRUKER, "1234", null, null), "Vedtak om medlemskap"),
            Arguments.of(true, false, lagMottaker(ARBEIDSGIVER, null, "1234", null), "Kopi av vedtak om medlemskap, Attest for medlemskap i folketrygden"),
            Arguments.of(false, false, lagMottaker(ARBEIDSGIVER, null, "1234", null), "Kopi av vedtak om medlemskap"),
            Arguments.of(true, false, lagMottaker(UTENLANDSK_TRYGDEMYNDIGHET, null, null, "1234"), "Attest for medlemskap i folketrygden"),

            Arguments.of(true, true, lagMottaker(BRUKER, "1234", null, null), "Vedtak om medlemskap, Attest for medlemskap i folketrygden - endring"),
            Arguments.of(false, true, lagMottaker(BRUKER, "1234", null, null), "Vedtak om medlemskap - endring"),
            Arguments.of(true, true, lagMottaker(ARBEIDSGIVER, null, "1234", null), "Kopi av vedtak om medlemskap, Attest for medlemskap i folketrygden - endring"),
            Arguments.of(false, true, lagMottaker(ARBEIDSGIVER, null, "1234", null), "Kopi av vedtak om medlemskap - endring"),
            Arguments.of(true, true, lagMottaker(UTENLANDSK_TRYGDEMYNDIGHET, null, null, "1234"), "Attest for medlemskap i folketrygden - endring")
        );
    }

    private Behandling lagBehandling() {
        var behandling = BehandlingTestBuilder.builderWithDefaults().build();
        behandling.setId(1L);
        return behandling;
    }

    private Lovvalgsperiode lagLovvalsperiode(LovvalgBestemmelse bestemmelse) {
        var lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setBestemmelse(bestemmelse);
        return lovvalgsperiode;
    }

    private static Mottaker lagMottaker(Mottakerroller rolle, String aktørID, String orgnr, String institusjonsID) {
        Mottaker mottaker = Mottaker.medRolle(rolle);
        mottaker.setAktørId(aktørID);
        mottaker.setOrgnr(orgnr);
        mottaker.setInstitusjonID(institusjonsID);
        return mottaker;
    }
}
