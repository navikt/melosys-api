package no.nav.melosys.domain.dokument.sed;

import java.time.Instant;
import java.util.Optional;
import java.util.stream.Collectors;

import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.SaksopplysningKilde;
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.domain.dokument.DokumentFactory;
import no.nav.melosys.domain.dokument.SaksopplysningDokument;
import no.nav.melosys.domain.dokument.XsltTemplatesFactory;
import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.dokument.jaxb.JaxbConfig;
import no.nav.melosys.domain.eessi.melding.Arbeidssted;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.LovvalgBestemmelse;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.jeasy.random.randomizers.misc.EnumRandomizer;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.jeasy.random.FieldPredicates.ofType;

public class SedDokumentTest {
    @Test
    public void testXmlSerialisering() {
        EasyRandomParameters easyRandomParameters = new EasyRandomParameters().collectionSizeRange(1, 2).stringLengthRange(1,4)
            .randomize(ofType(LovvalgBestemmelse.class), () -> new EnumRandomizer<>(Lovvalgbestemmelser_883_2004.class).getRandomValue());
        final EasyRandom easyRandom = new EasyRandom(easyRandomParameters);
        SedDokument sedDokument = easyRandom.nextObject(SedDokument.class);
        sedDokument.setArbeidssteder(easyRandom.objects(Arbeidssted.class, 2).collect(Collectors.toList()));
        sedDokument.setStatsborgerskapKoder(easyRandom.objects(String.class, 2).collect(Collectors.toList()));

        DokumentFactory dokumentFactory = new DokumentFactory(JaxbConfig.jaxb2Marshaller(), new XsltTemplatesFactory());
        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setDokument(sedDokument);
        saksopplysning.setType(SaksopplysningType.SEDOPPL);
        saksopplysning.setKilde(SaksopplysningKilde.EESSI);
        Instant nå = Instant.now();
        saksopplysning.setEndretDato(nå);
        saksopplysning.setRegistrertDato(nå);
        String xml = dokumentFactory.lagInternXml(saksopplysning);
        saksopplysning.setDokumentXml(xml);

        final SaksopplysningDokument saksopplysningDokument = dokumentFactory.lagDokument(saksopplysning);
        assertThat(saksopplysningDokument).isEqualToComparingFieldByField(sedDokument);
    }

    @Test
    public void finnAvsenderLand() {
        SedDokument sedDokument = new SedDokument();
        sedDokument.setAvsenderID("NO" + ":" + "xopjaf");

        Optional<Landkoder> avsenderLand = sedDokument.finnAvsenderLand();
        assertThat(avsenderLand).contains(Landkoder.NO);
    }
}