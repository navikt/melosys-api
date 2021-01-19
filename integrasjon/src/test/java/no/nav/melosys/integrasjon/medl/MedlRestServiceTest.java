package no.nav.melosys.integrasjon.medl;

import java.time.LocalDate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.dokument.medlemskap.MedlemskapDokument;
import no.nav.melosys.domain.dokument.medlemskap.Medlemsperiode;
import no.nav.melosys.domain.kodeverk.Landkoder;
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
import static org.junit.jupiter.api.Assertions.*;
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
        assertNotNull(saksopplysning);
        assertNotNull(saksopplysning.getKilder());
        assertFalse(saksopplysning.getKilder().isEmpty());
        assertNotNull(saksopplysning.getKilder().iterator().next().getMottattDokument());

        MedlemskapDokument medlemskapDokument = (MedlemskapDokument) saksopplysning.getDokument();

        assertNotNull(medlemskapDokument);
        assertNotNull(medlemskapDokument.getMedlemsperiode());
        assertFalse(medlemskapDokument.getMedlemsperiode().isEmpty());

        for (Medlemsperiode medlemsperiode : medlemskapDokument.getMedlemsperiode()) {
            assertNotNull(medlemsperiode.getType());
            assertNotNull(medlemsperiode.getStatus());
            assertNotNull(medlemsperiode.getLovvalg());
            assertNotNull(medlemsperiode.getKilde());
            assertNotNull(medlemsperiode.getGrunnlagstype());
        }
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

        assertEquals(FNR, request.getIdent());
        assertEquals(lovvalgsperiode.getFom(), request.getFraOgMed());
        assertEquals(lovvalgsperiode.getTom(), request.getTilOgMed());
        assertEquals(GYLD.getKode(), request.getStatus());
        assertEquals(DekningMedl.FULL.getKode(), request.getDekning());
        assertEquals("BEL", request.getLovvalgsland());
        assertEquals(ENDL.getKode(), request.getLovvalg());
        assertEquals("FO_11_4_1", request.getGrunnlag());
        assertEquals(KildedokumenttypeMedl.HENV_SOKNAD.getKode(), request.getSporingsinformasjon().getKildedokument());
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

        assertEquals(FNR, request.getIdent());
        assertEquals(lovvalgsperiode.getFom(), request.getFraOgMed());
        assertEquals(lovvalgsperiode.getTom(), request.getTilOgMed());
        assertEquals(UAVK.getKode(), request.getStatus());
        assertEquals(DekningMedl.FULL.getKode(), request.getDekning());
        assertEquals("BEL", request.getLovvalgsland());
        assertEquals(UAVK.getKode(), request.getLovvalg());
        assertEquals("FO_11_4_1", request.getGrunnlag());
        assertEquals(KildedokumenttypeMedl.HENV_SOKNAD.getKode(), request.getSporingsinformasjon().getKildedokument());
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

        assertEquals(FNR, request.getIdent());
        assertEquals(lovvalgsperiode.getFom(), request.getFraOgMed());
        assertEquals(lovvalgsperiode.getTom(), request.getTilOgMed());
        assertEquals(UAVK.getKode(), request.getStatus());
        assertEquals(DekningMedl.FULL.getKode(), request.getDekning());
        assertEquals("BEL", request.getLovvalgsland());
        assertEquals(FORL.getKode(), request.getLovvalg());
        assertEquals("FO_11_4_1", request.getGrunnlag());
        assertEquals(KildedokumenttypeMedl.HENV_SOKNAD.getKode(), request.getSporingsinformasjon().getKildedokument());
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

        assertEquals(123456L, request.getUnntakId());
        assertEquals(lovvalgsperiode.getFom(), request.getFraOgMed());
        assertEquals(lovvalgsperiode.getTom(), request.getTilOgMed());
        assertEquals(GYLD.getKode(), request.getStatus());
        assertEquals(DekningMedl.FULL.getKode(), request.getDekning());
        assertEquals("BEL", request.getLovvalgsland());
        assertEquals(ENDL.getKode(), request.getLovvalg());
        assertEquals("FO_11_4_1", request.getGrunnlag());
        assertEquals(KildedokumenttypeMedl.HENV_SOKNAD.getKode(), request.getSporingsinformasjon().getKildedokument());
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

        assertEquals(123456L, request.getUnntakId());
        assertEquals(lovvalgsperiode.getFom(), request.getFraOgMed());
        assertEquals(lovvalgsperiode.getTom(), request.getTilOgMed());
        assertEquals(UAVK.getKode(), request.getStatus());
        assertEquals(DekningMedl.FULL.getKode(), request.getDekning());
        assertEquals("BEL", request.getLovvalgsland());
        assertEquals(FORL.getKode(), request.getLovvalg());
        assertEquals("FO_11_4_1", request.getGrunnlag());
        assertEquals(KildedokumenttypeMedl.HENV_SOKNAD.getKode(), request.getSporingsinformasjon().getKildedokument());
    }

    @Test
    void oppdaterePeriodeFeilerMedManglendePeriodeId() {
        TekniskException exception = assertThrows(TekniskException.class, () ->
            medlRestService.oppdaterPeriodeForeløpig(lagLovvalgsPeriode(), KildedokumenttypeMedl.HENV_SOKNAD)
        );

        assertEquals("Det er ikke lagret noen medlPeriodeID på lovvalgsperiode som skal oppdateres i MEDL", exception.getMessage());
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

        assertEquals(123456L, request.getUnntakId());
        assertEquals(AVST.getKode(), request.getStatus());
        assertEquals(AVVIST.getKode(), request.getStatusaarsak());
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

    private <T> T hentJsonResponse(String filnavn, Class<T> clazz) throws java.io.IOException {
        return objectMapper.readValue(
            getClass().getClassLoader().getResource("mock/medlemskap/" + filnavn + ".json"),
            clazz);
    }
}
