package no.nav.melosys.regler.lovvalg;

import static no.nav.melosys.regler.lovvalg.LovvalgKontekst.initialiserLokalKontekst;
import static no.nav.melosys.regler.lovvalg.LovvalgKontekst.slettLokalKontekst;
import static no.nav.melosys.regler.lovvalg.LovvalgKontekst.søknad;
import static no.nav.melosys.regler.lovvalg.LovvalgPredikater.antallMånederIPeriodenErMindreEnnEllerLik;
import static no.nav.melosys.regler.service.lovvalg.LovvalgTjenesteImplTest.lagFastsettLovvalgRequest;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.LocalDate;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class LovvalgPredikaterTest {

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        initialiserLokalKontekst(lagFastsettLovvalgRequest());
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        slettLokalKontekst();
    }

    @Test
    public void testAntallMånederIPeriodenErMindreEnnEllerLik() {
        // Standard funksjonalitet
        søknad().periodeFom = LocalDate.of(2017, 1, 1);
        søknad().periodeTom = LocalDate.of(2018, 12, 31);
        assertTrue(antallMånederIPeriodenErMindreEnnEllerLik(24).test());
        søknad().periodeTom = LocalDate.of(2019, 1, 1);
        assertFalse(antallMånederIPeriodenErMindreEnnEllerLik(24).test());
        // Perioder som ikke starter den første i en måned
        søknad().periodeFom = LocalDate.of(2017, 4, 30);
        søknad().periodeTom = LocalDate.of(2017, 5, 30);
        assertFalse(antallMånederIPeriodenErMindreEnnEllerLik(1).test());
        søknad().periodeFom = LocalDate.of(2017, 5, 31);
        søknad().periodeTom = LocalDate.of(2017, 6, 30);
        assertFalse(antallMånederIPeriodenErMindreEnnEllerLik(1).test());
        /* Denne testen er skrdd av. Ikke sikkert dette er klart definert i lovverket.
        // Skuddår
        // Kuriøst nok mener java at det er mer enn 24 måneder mellom en skuddårsdag og 28. februar to år etter...
        // Dette er ikke intuitivt, og kan gi oss problem.
        søknad().periodeFom = LocalDate.of(2016, 2, 29);
        søknad().periodeTom = LocalDate.of(2018, 2, 28);
        assertTrue(antallMånederIPeriodenErMindreEnnEllerLik(24).test());
        //*/
    }
    
}
