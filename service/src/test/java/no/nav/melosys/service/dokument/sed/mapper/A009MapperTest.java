package no.nav.melosys.service.dokument.sed.mapper;

import java.io.IOException;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.util.Collections;

import no.nav.melosys.domain.Landkoder;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.bestemmelse.LovvalgBestemmelse_883_2004;
import no.nav.melosys.domain.bestemmelse.LovvalgBestemmelse_987_2009;
import no.nav.melosys.eux.model.medlemskap.impl.MedlemskapA009;
import no.nav.melosys.eux.model.nav.SED;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.dokument.sed.A009Data;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
public class A009MapperTest {

    private A009Mapper a009Mapper = new A009Mapper();

    private A009Data a009Data;

    @Before
    public void setup() throws IOException, URISyntaxException {
        a009Data = SedDataStub.hent(new A009Data());

        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setBestemmelse(LovvalgBestemmelse_883_2004.FO_883_2004_ART12_1);
        lovvalgsperiode.setFom(LocalDate.now());
        lovvalgsperiode.setTom(LocalDate.now().plusYears(1L));
        lovvalgsperiode.setLovvalgsland(Landkoder.NO);
        a009Data.setLovvalgsperioder(Collections.singletonList(lovvalgsperiode));

        a009Data.getPersonDokument().erEgenAnsatt = false;
    }

    @Test
    public void testHentMedlemskapIkkeSelvstendigOg12_1() throws FunksjonellException, TekniskException {
        a009Data.getPersonDokument().erEgenAnsatt = false;
        SED sed = a009Mapper.mapTilSed(a009Data);

        assertEquals(MedlemskapA009.class, sed.getMedlemskap().getClass());

        MedlemskapA009 medlemskapA009 = (MedlemskapA009) sed.getMedlemskap();

        assertNotNull(medlemskapA009);
        assertNotNull(medlemskapA009.getUtsendingsland());
        assertEquals(sed.getNav().getArbeidsgiver().get(0).getNavn(), medlemskapA009.getUtsendingsland().getArbeidsgiver().get(0).getNavn());

        assertNotNull(medlemskapA009.getVedtak());
        assertEquals("12_1", medlemskapA009.getVedtak().getArtikkelforordning());
        assertNotNull(medlemskapA009.getVedtak().getGjelderperiode().getFastperiode());
        assertNull(medlemskapA009.getVedtak().getGjelderperiode().getAapenperiode());
    }

    @Test
    public void testHentMedlemskapErSelvstendigOg12_2() throws FunksjonellException, TekniskException {
        a009Data.getLovvalgsperioder().iterator().next().setBestemmelse(LovvalgBestemmelse_883_2004.FO_883_2004_ART12_2);
        a009Data.getPersonDokument().erEgenAnsatt = true;
        SED sed = a009Mapper.mapTilSed(a009Data);

        assertEquals(MedlemskapA009.class, sed.getMedlemskap().getClass());

        MedlemskapA009 medlemskapA009 = (MedlemskapA009) sed.getMedlemskap();

        assertNotNull(medlemskapA009);
        assertNull(medlemskapA009.getUtsendingsland());

        assertNotNull(medlemskapA009.getVedtak());
        assertEquals("12_2", medlemskapA009.getVedtak().getArtikkelforordning());
        assertNotNull(medlemskapA009.getVedtak().getGjelderperiode().getFastperiode());
        assertNull(medlemskapA009.getVedtak().getGjelderperiode().getAapenperiode());
    }

    @Test(expected = FunksjonellException.class)
    public void testHentMedlemkapFeilLovvalgsBestemmelse() throws FunksjonellException, TekniskException {
        a009Data.getLovvalgsperioder().iterator().next().setBestemmelse(LovvalgBestemmelse_987_2009.FO_987_2009_ART14_11);
        a009Mapper.mapTilSed(a009Data);
    }

    @Test(expected = TekniskException.class)
    @SuppressWarnings("unchecked")
    public void testIngenLovvalgsperioder() throws FunksjonellException, TekniskException {
        a009Data.setLovvalgsperioder(Collections.EMPTY_LIST);
        a009Mapper.mapTilSed(a009Data);
    }
}
