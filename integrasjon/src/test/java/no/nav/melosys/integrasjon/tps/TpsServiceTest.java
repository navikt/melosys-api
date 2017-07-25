package no.nav.melosys.integrasjon.tps;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.Before;
import org.junit.Test;

import no.nav.melosys.domain.Bruker;
import no.nav.melosys.integrasjon.test.TpsTestData;
import no.nav.melosys.integrasjon.tps.aktoer.AktorConsumer;
import no.nav.melosys.integrasjon.tps.person.PersonConsumer;
import no.nav.tjeneste.virksomhet.aktoer.v2.binding.HentAktoerIdForIdentPersonIkkeFunnet;
import no.nav.tjeneste.virksomhet.aktoer.v2.feil.PersonIkkeFunnet;
import no.nav.tjeneste.virksomhet.aktoer.v2.meldinger.HentAktoerIdForIdentResponse;
import no.nav.tjeneste.virksomhet.person.v2.informasjon.Diskresjonskoder;
import no.nav.tjeneste.virksomhet.person.v2.informasjon.Kjoenn;
import no.nav.tjeneste.virksomhet.person.v2.informasjon.Kjoennstyper;
import no.nav.tjeneste.virksomhet.person.v2.informasjon.Person;
import no.nav.tjeneste.virksomhet.person.v2.informasjon.Personnavn;
import no.nav.tjeneste.virksomhet.person.v2.meldinger.HentKjerneinformasjonResponse;

public class TpsServiceTest {

    private static final String FNR_1 = TpsTestData.STD_KVINNE_FNR;
    private static final String FNR_UKJENT = "88888888889";
    private static final Long AKTØRID_1 = TpsTestData.STD_KVINNE_AKTØR_ID;

    private AktorConsumer aktorConsumer;

    private PersonConsumer personConsumer;

    private TpsService service;

    @Before
    public void setUp() {
        aktorConsumer = mock(AktorConsumer.class);
        personConsumer = mock(PersonConsumer.class);
        service = new TpsService(aktorConsumer, personConsumer);
    }

    @Test
    public void test_hentAktørIdForIdent_normal() {
        HentAktoerIdForIdentResponse r1 = new no.nav.tjeneste.virksomhet.aktoer.v2.meldinger.HentAktoerIdForIdentResponse();
        r1.setAktoerId(AKTØRID_1.toString());

        try {
            when(aktorConsumer.hentAktørIdForIdent(any())).thenReturn(r1);
        } catch (HentAktoerIdForIdentPersonIkkeFunnet hentAktoerIdForIdentPersonIkkeFunnet) {
            hentAktoerIdForIdentPersonIkkeFunnet.printStackTrace();
        }

        Optional<String> optAktørId = service.hentAktørIdForIdent(FNR_1);
        assertNotNull(optAktørId);
        assertTrue(optAktørId.isPresent());
        assertEquals(AKTØRID_1.toString(), optAktørId.get());
    }

    @Test
    public void test_hentAktørIdForIdent_ikkeFunnet() throws HentAktoerIdForIdentPersonIkkeFunnet {
        when(aktorConsumer.hentAktørIdForIdent(any())).thenThrow(new HentAktoerIdForIdentPersonIkkeFunnet("test", new PersonIkkeFunnet()));

        Optional<String> optAktørId = service.hentAktørIdForIdent(FNR_UKJENT);
        assertNotNull(optAktørId);
        assertFalse(optAktørId.isPresent());
    }

    @Test
    public void hentKjerneinformasjon() throws Exception {

        Person p = new Person();
        p.setPersonnavn(new Personnavn());
        String navn = "Ola Normann";
        p.getPersonnavn().setSammensattNavn(navn);

        Kjoennstyper kjoennType = new Kjoennstyper();
        kjoennType.setValue("M");
        Kjoenn kjoenn = new Kjoenn();
        kjoenn.setKjoenn(kjoennType);
        p.setKjoenn(kjoenn);

        Diskresjonskoder kode = new Diskresjonskoder();
        kode.setValue("6");
        p.setDiskresjonskode(kode);

        HentKjerneinformasjonResponse response = new HentKjerneinformasjonResponse();
        response.setPerson(p);
        when(personConsumer.hentKjerneinformasjon(any())).thenReturn(response);

        Bruker bruker = new Bruker();
        bruker.setFnr("1234");
        bruker = service.hentKjerneinformasjon(bruker);
        assertThat(bruker.getNavn()).isEqualTo(navn);
        assertThat(bruker.getKjønn().getKode()).isEqualTo(kjoennType.getValue());
        assertThat(bruker.getDiskresjonskode()).isEqualTo(kode.getValue());
    }
}
