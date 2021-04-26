package no.nav.melosys.integrasjon.medl;

import java.time.LocalDate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.Medlemskapsperiode;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.dokument.medlemskap.MedlemskapDokument;
import no.nav.melosys.domain.dokument.medlemskap.Medlemsperiode;
import no.nav.melosys.domain.kodeverk.Folketrygdloven_kap2_bestemmelser;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Medlemskapstyper;
import no.nav.melosys.domain.kodeverk.Trygdedekninger;
import no.nav.melosys.exception.TekniskException;
import no.nav.tjenester.medlemskapsunntak.api.v1.MedlemskapsunntakForGet;
import no.nav.tjenester.medlemskapsunntak.api.v1.MedlemskapsunntakForPost;
import no.nav.tjenester.medlemskapsunntak.api.v1.MedlemskapsunntakForPut;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.ArgumentCaptor;

import static java.util.Arrays.asList;
import static no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Tilleggsbestemmelser_883_2004.FO_883_2004_ART11_4_1;
import static no.nav.melosys.integrasjon.medl.LovvalgMedl.*;
import static no.nav.melosys.integrasjon.medl.PeriodestatusMedl.AVST;
import static no.nav.melosys.integrasjon.medl.PeriodestatusMedl.GYLD;
import static no.nav.melosys.integrasjon.medl.StatusaarsakMedl.AVVIST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MedlRestServiceTest {

    private MedlRestService medlRestService;

    private final String FNR = "77777777773";

    private MedlemskapRestConsumer mockRestConsumer;
    private ObjectMapper objectMapper;

    @BeforeAll
    void init() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        mockRestConsumer = mock(MedlemskapRestConsumer.class);
        medlRestService = new MedlRestService(mockRestConsumer, objectMapper);
    }

    @BeforeEach
    void setUp() {
        reset(mockRestConsumer);
    }

    @Test
    void skalHentPeriodeliste() throws Exception {
        when(mockRestConsumer.hentPeriodeListe(eq(FNR), any(), any())).thenReturn(
            asList(hentJsonResponse("gyldigPeriodelisteResponse", MedlemskapsunntakForGet[].class))
        );

        Saksopplysning saksopplysning = medlRestService.hentPeriodeListe(FNR, null, null);
        assertThat(saksopplysning).isNotNull();
        assertThat(saksopplysning.getKilder()).isNotEmpty();
        assertThat(saksopplysning.getKilder().iterator().next().getMottattDokument()).isNotNull();

        MedlemskapDokument medlemskapDokument = (MedlemskapDokument) saksopplysning.getDokument();

        assertThat(medlemskapDokument).isNotNull();
        assertThat(medlemskapDokument.getMedlemsperiode()).hasSize(1);

        Medlemsperiode medlemsperiode = medlemskapDokument.getMedlemsperiode().get(0);
        assertThat(medlemsperiode.getType()).isEqualTo("PUMEDSKP");
        assertThat(medlemsperiode.getStatus()).isEqualTo(GYLD.getKode());
        assertThat(medlemsperiode.getLovvalg()).isEqualTo(ENDL.getKode());
        assertThat(medlemsperiode.getKilde()).isEqualTo("INFOTR");
        assertThat(medlemsperiode.getGrunnlagstype()).isEqualTo("IMEDEOS");
    }

    @Test
    void skalOpprettPeriodeEndelig() throws Exception {
        when(mockRestConsumer.opprettPeriode(any())).thenReturn(
            hentJsonResponse("gyldigPeriodeResponse", MedlemskapsunntakForGet.class)
        );

        Lovvalgsperiode lovvalgsperiode = lagLovvalgsPeriode();
        medlRestService.opprettPeriodeEndelig(FNR, lovvalgsperiode, KildedokumenttypeMedl.HENV_SOKNAD);

        ArgumentCaptor<MedlemskapsunntakForPost> captor = ArgumentCaptor.forClass(MedlemskapsunntakForPost.class);

        verify(mockRestConsumer).opprettPeriode(captor.capture());

        MedlemskapsunntakForPost request = captor.getValue();

        assertThat(request.getIdent()).isEqualTo(FNR);
        assertThat(request.getFraOgMed()).isEqualTo(lovvalgsperiode.getFom());
        assertThat(request.getTilOgMed()).isEqualTo(lovvalgsperiode.getTom());
        assertThat(request.getStatus()).isEqualTo(GYLD.getKode());
        assertThat(request.getDekning()).isEqualTo(DekningMedl.FULL.getKode());
        assertThat(request.getLovvalgsland()).isEqualTo("BEL");
        assertThat(request.getLovvalg()).isEqualTo(ENDL.getKode());
        assertThat(request.getGrunnlag()).isEqualTo("FO_11_4_1");
        assertThat(request.getSporingsinformasjon().getKildedokument()).isEqualTo(KildedokumenttypeMedl.HENV_SOKNAD.getKode());
    }

    @Test
    void skalOpprettPeriodeUnderAvklaring() throws Exception {
        when(mockRestConsumer.opprettPeriode(any())).thenReturn(
            hentJsonResponse("gyldigPeriodeResponse", MedlemskapsunntakForGet.class)
        );

        Lovvalgsperiode lovvalgsperiode = lagLovvalgsPeriode();
        medlRestService.opprettPeriodeUnderAvklaring(FNR, lovvalgsperiode, KildedokumenttypeMedl.HENV_SOKNAD);

        ArgumentCaptor<MedlemskapsunntakForPost> captor = ArgumentCaptor.forClass(MedlemskapsunntakForPost.class);

        verify(mockRestConsumer).opprettPeriode(captor.capture());

        MedlemskapsunntakForPost request = captor.getValue();

        assertThat(request.getIdent()).isEqualTo(FNR);
        assertThat(request.getFraOgMed()).isEqualTo(lovvalgsperiode.getFom());
        assertThat(request.getTilOgMed()).isEqualTo(lovvalgsperiode.getTom());
        assertThat(request.getStatus()).isEqualTo(UAVK.getKode());
        assertThat(request.getDekning()).isEqualTo(DekningMedl.FULL.getKode());
        assertThat(request.getLovvalgsland()).isEqualTo("BEL");
        assertThat(request.getLovvalg()).isEqualTo(UAVK.getKode());
        assertThat(request.getGrunnlag()).isEqualTo("FO_11_4_1");
        assertThat(request.getSporingsinformasjon().getKildedokument()).isEqualTo(KildedokumenttypeMedl.HENV_SOKNAD.getKode());
    }

    @Test
    void skalOpprettPeriodeForeløpig() throws Exception {
        when(mockRestConsumer.opprettPeriode(any())).thenReturn(
            hentJsonResponse("gyldigPeriodeResponse", MedlemskapsunntakForGet.class)
        );

        Lovvalgsperiode lovvalgsperiode = lagLovvalgsPeriode();
        medlRestService.opprettPeriodeForeløpig(FNR, lovvalgsperiode, KildedokumenttypeMedl.HENV_SOKNAD);

        ArgumentCaptor<MedlemskapsunntakForPost> captor = ArgumentCaptor.forClass(MedlemskapsunntakForPost.class);

        verify(mockRestConsumer).opprettPeriode(captor.capture());

        MedlemskapsunntakForPost request = captor.getValue();

        assertThat(request.getIdent()).isEqualTo(FNR);
        assertThat(request.getFraOgMed()).isEqualTo(lovvalgsperiode.getFom());
        assertThat(request.getTilOgMed()).isEqualTo(lovvalgsperiode.getTom());
        assertThat(request.getStatus()).isEqualTo(UAVK.getKode());
        assertThat(request.getDekning()).isEqualTo(DekningMedl.FULL.getKode());
        assertThat(request.getLovvalgsland()).isEqualTo("BEL");
        assertThat(request.getLovvalg()).isEqualTo(FORL.getKode());
        assertThat(request.getGrunnlag()).isEqualTo("FO_11_4_1");
        assertThat(request.getSporingsinformasjon().getKildedokument()).isEqualTo(KildedokumenttypeMedl.HENV_SOKNAD.getKode());
    }

    @Test
    void skalOppdaterePeriodeEndelig() throws Exception {
        when(mockRestConsumer.hentPeriode(any())).thenReturn(
            hentJsonResponse("gyldigPeriodeResponse", MedlemskapsunntakForGet.class)
        );
        when(mockRestConsumer.oppdaterPeriode(any())).thenReturn(
            hentJsonResponse("gyldigPeriodeResponse", MedlemskapsunntakForGet.class)
        );

        Lovvalgsperiode lovvalgsperiode = lagLovvalgsPeriode();
        lovvalgsperiode.setMedlPeriodeID(123456L);
        medlRestService.oppdaterPeriodeEndelig(lovvalgsperiode, KildedokumenttypeMedl.HENV_SOKNAD);

        ArgumentCaptor<MedlemskapsunntakForPut> captor = ArgumentCaptor.forClass(MedlemskapsunntakForPut.class);

        verify(mockRestConsumer).oppdaterPeriode(captor.capture());

        MedlemskapsunntakForPut request = captor.getValue();

        assertThat(request.getUnntakId()).isEqualTo(123456L);
        assertThat(request.getFraOgMed()).isEqualTo(lovvalgsperiode.getFom());
        assertThat(request.getTilOgMed()).isEqualTo(lovvalgsperiode.getTom());
        assertThat(request.getStatus()).isEqualTo(GYLD.getKode());
        assertThat(request.getDekning()).isEqualTo(DekningMedl.FULL.getKode());
        assertThat(request.getLovvalgsland()).isEqualTo("BEL");
        assertThat(request.getLovvalg()).isEqualTo(ENDL.getKode());
        assertThat(request.getGrunnlag()).isEqualTo("FO_11_4_1");
        assertThat(request.getSporingsinformasjon().getKildedokument()).isEqualTo(KildedokumenttypeMedl.HENV_SOKNAD.getKode());
    }

    @Test
    void skalOpprettePeriodeEndeligFtrl() throws Exception {
        when(mockRestConsumer.opprettPeriode(any())).thenReturn(
            hentJsonResponse("gyldigPeriodeResponse", MedlemskapsunntakForGet.class)
        );

        Medlemskapsperiode medlemskapsperiode = lagMedlemskapsPeriode();

        medlRestService.opprettPeriodeEndelig(FNR, medlemskapsperiode, KildedokumenttypeMedl.HENV_SOKNAD);

        ArgumentCaptor<MedlemskapsunntakForPost> captor = ArgumentCaptor.forClass(MedlemskapsunntakForPost.class);

        verify(mockRestConsumer).opprettPeriode(captor.capture());

        MedlemskapsunntakForPost request = captor.getValue();

        assertThat(request.getFraOgMed()).isEqualTo(medlemskapsperiode.getFom());
        assertThat(request.getTilOgMed()).isEqualTo(medlemskapsperiode.getTom());
        assertThat(request.getStatus()).isEqualTo(GYLD.getKode());
        assertThat(request.getDekning()).isEqualTo(DekningMedl.FTRL_2_9_1_LEDD_A.getKode());
        assertThat(request.getLovvalgsland()).isEqualTo("BEL");
        assertThat(request.getLovvalg()).isEqualTo(ENDL.getKode());
        assertThat(request.getGrunnlag()).isEqualTo(GrunnlagMedl.FTL_2_8.getKode());
        assertThat(request.getSporingsinformasjon().getKildedokument()).isEqualTo(KildedokumenttypeMedl.HENV_SOKNAD.getKode());
    }

    @Test
    void skalOppdaterePeriodeForeløpig() throws Exception {
        when(mockRestConsumer.hentPeriode(any())).thenReturn(
            hentJsonResponse("gyldigPeriodeResponse", MedlemskapsunntakForGet.class)
        );
        when(mockRestConsumer.oppdaterPeriode(any())).thenReturn(
            hentJsonResponse("gyldigPeriodeResponse", MedlemskapsunntakForGet.class)
        );

        Lovvalgsperiode lovvalgsperiode = lagLovvalgsPeriode();
        lovvalgsperiode.setMedlPeriodeID(123456L);
        medlRestService.oppdaterPeriodeForeløpig(lovvalgsperiode, KildedokumenttypeMedl.HENV_SOKNAD);

        ArgumentCaptor<MedlemskapsunntakForPut> captor = ArgumentCaptor.forClass(MedlemskapsunntakForPut.class);

        verify(mockRestConsumer).oppdaterPeriode(captor.capture());

        MedlemskapsunntakForPut request = captor.getValue();

        assertThat(request.getUnntakId()).isEqualTo(123456L);
        assertThat(request.getFraOgMed()).isEqualTo(lovvalgsperiode.getFom());
        assertThat(request.getTilOgMed()).isEqualTo(lovvalgsperiode.getTom());
        assertThat(request.getStatus()).isEqualTo(UAVK.getKode());
        assertThat(request.getDekning()).isEqualTo(DekningMedl.FULL.getKode());
        assertThat(request.getLovvalgsland()).isEqualTo("BEL");
        assertThat(request.getLovvalg()).isEqualTo(FORL.getKode());
        assertThat(request.getGrunnlag()).isEqualTo("FO_11_4_1");
        assertThat(request.getSporingsinformasjon().getKildedokument()).isEqualTo(KildedokumenttypeMedl.HENV_SOKNAD.getKode());
    }

    @Test
    void oppdaterePeriodeFeilerMedManglendePeriodeId() {
        assertThatExceptionOfType(TekniskException.class)
            .isThrownBy(() -> medlRestService.oppdaterPeriodeForeløpig(lagLovvalgsPeriode(), KildedokumenttypeMedl.HENV_SOKNAD))
            .withMessageContaining("Det er ikke lagret noen medlPeriodeID på lovvalgsperiode som skal oppdateres i MEDL");
    }

    @Test
    void skalAvvisePeriode() throws Exception {
        when(mockRestConsumer.hentPeriode(any())).thenReturn(
            hentJsonResponse("gyldigPeriodeResponse", MedlemskapsunntakForGet.class)
        );
        when(mockRestConsumer.oppdaterPeriode(any())).thenReturn(
            hentJsonResponse("gyldigPeriodeResponse", MedlemskapsunntakForGet.class)
        );

        Lovvalgsperiode lovvalgsperiode = lagLovvalgsPeriode();
        lovvalgsperiode.setMedlPeriodeID(123456L);
        medlRestService.avvisPeriode(123456L, AVVIST);

        ArgumentCaptor<MedlemskapsunntakForPut> captor = ArgumentCaptor.forClass(MedlemskapsunntakForPut.class);

        verify(mockRestConsumer).oppdaterPeriode(captor.capture());

        MedlemskapsunntakForPut request = captor.getValue();

        assertThat(request.getUnntakId()).isEqualTo(123456L);
        assertThat(request.getStatus()).isEqualTo(AVST.getKode());
        assertThat(request.getStatusaarsak()).isEqualTo(AVVIST.getKode());
    }

    private Lovvalgsperiode lagLovvalgsPeriode() {
        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setFom(LocalDate.now());
        lovvalgsperiode.setTom(LocalDate.now().plusYears(1));
        lovvalgsperiode.setDekning(Trygdedekninger.FULL_DEKNING_EOSFO);
        lovvalgsperiode.setLovvalgsland(Landkoder.BE);
        lovvalgsperiode.setTilleggsbestemmelse(FO_883_2004_ART11_4_1);
        return lovvalgsperiode;
    }

    private Medlemskapsperiode lagMedlemskapsPeriode() {
        Medlemskapsperiode medlemskapsperiode = new Medlemskapsperiode();
        medlemskapsperiode.setMedlemskapstype(Medlemskapstyper.FRIVILLIG);
        medlemskapsperiode.setArbeidsland(Landkoder.BE.getKode());
        medlemskapsperiode.setBestemmelse(Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8);
        medlemskapsperiode.setTrygdedekning(Trygdedekninger.HELSEDEL);
        medlemskapsperiode.setFom(LocalDate.now());
        medlemskapsperiode.setTom(LocalDate.now().plusYears(1));

        return medlemskapsperiode;
    }

    private <T> T hentJsonResponse(String filnavn, Class<T> clazz) throws java.io.IOException {
        return objectMapper.readValue(
            getClass().getClassLoader().getResource("mock/medlemskap/" + filnavn + ".json"),
            clazz);
    }
}
