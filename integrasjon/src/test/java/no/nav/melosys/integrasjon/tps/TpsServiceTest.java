package no.nav.melosys.integrasjon.tps;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.dokument.DokumentFactory;
import no.nav.melosys.domain.dokument.XsltTemplatesFactory;
import no.nav.melosys.domain.dokument.jaxb.JaxbConfig;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.dokument.person.PersonhistorikkDokument;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.integrasjon.test.TpsTestData;
import no.nav.melosys.integrasjon.tps.aktoer.AktoerIdCache;
import no.nav.melosys.integrasjon.tps.aktoer.AktorConsumer;
import no.nav.melosys.integrasjon.tps.person.PersonConsumer;
import no.nav.tjeneste.virksomhet.aktoer.v2.binding.HentAktoerIdForIdentPersonIkkeFunnet;
import no.nav.tjeneste.virksomhet.aktoer.v2.binding.HentIdentForAktoerIdPersonIkkeFunnet;
import no.nav.tjeneste.virksomhet.aktoer.v2.feil.PersonIkkeFunnet;
import no.nav.tjeneste.virksomhet.aktoer.v2.meldinger.HentAktoerIdForIdentResponse;
import no.nav.tjeneste.virksomhet.aktoer.v2.meldinger.HentIdentForAktoerIdResponse;
import no.nav.tjeneste.virksomhet.aktoer.v2.meldinger.IdentDetaljer;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.*;
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentPersonhistorikkResponse;
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentPersonResponse;
import org.junit.Before;
import org.junit.Test;

import static java.lang.Thread.sleep;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class TpsServiceTest {

    private static final String FNR_1 = TpsTestData.STD_KVINNE_FNR;
    private static final String FNR_UKJENT = "88888888889";
    private static final Long AKTØRID_1 = TpsTestData.STD_KVINNE_AKTØR_ID;

    private AktorConsumer aktorConsumer;

    private PersonConsumer personConsumer;

    private AktoerIdCache aktørIdCache;

    private TpsService service;

    @Before
    public void setUp() throws HentAktoerIdForIdentPersonIkkeFunnet, HentIdentForAktoerIdPersonIkkeFunnet {
        aktorConsumer = mock(AktorConsumer.class);
        personConsumer = mock(PersonConsumer.class);

        when(aktorConsumer.hentAktørIdForIdent(any())).thenReturn(hentAktørIdResponse());

        HentIdentForAktoerIdResponse identResponse = new HentIdentForAktoerIdResponse();
        identResponse.setIdent(FNR_1);
        when(aktorConsumer.hentIdentForAktoerId(any())).thenReturn(identResponse);

        DokumentFactory dokumentFactory = new DokumentFactory(new JaxbConfig().jaxb2Marshaller(), new XsltTemplatesFactory());

        aktørIdCache = new AktoerIdCache(1000, 1000, 16);
        service = new TpsService(aktorConsumer, personConsumer, dokumentFactory, aktørIdCache);
    }

    @Test
    public void test_hentAktørIdForIdent_normal() throws HentAktoerIdForIdentPersonIkkeFunnet, IkkeFunnetException {
        HentAktoerIdForIdentResponse r1 = new no.nav.tjeneste.virksomhet.aktoer.v2.meldinger.HentAktoerIdForIdentResponse();
        r1.setAktoerId(AKTØRID_1.toString());

        when(aktorConsumer.hentAktørIdForIdent(any())).thenReturn(r1);

        String aktørIdForIdent = service.hentAktørIdForIdent(FNR_1);
        assertNotNull(aktørIdForIdent);
        assertEquals(AKTØRID_1.toString(), aktørIdForIdent);
    }

    @Test(expected = IkkeFunnetException.class)
    public void test_hentAktørIdForIdent_ikkeFunnet() throws HentAktoerIdForIdentPersonIkkeFunnet, IkkeFunnetException {
        when(aktorConsumer.hentAktørIdForIdent(any())).thenThrow(new HentAktoerIdForIdentPersonIkkeFunnet("test", new PersonIkkeFunnet()));

        service.hentAktørIdForIdent(FNR_UKJENT);
    }

    @Test
    public void hentPerson() throws Exception {
        HentPersonResponse r1 = new HentPersonResponse();
        Person person = new Person().withAktoer(new PersonIdent().withIdent(new NorskIdent().withIdent(AKTØRID_1.toString())));
        r1.setPerson(person);

        when(personConsumer.hentPerson(any())).thenReturn(r1);

        Saksopplysning saksopplysning = service.hentPerson(AKTØRID_1.toString());

        PersonDokument dokument = (PersonDokument) saksopplysning.getDokument();
        assertThat(dokument.fnr).isEqualTo(AKTØRID_1.toString());
    }

    @Test
    public void hentPersonhistorikk() throws Exception {
        HentPersonhistorikkResponse r1 = new HentPersonhistorikkResponse();
        StatsborgerskapPeriode sp = new StatsborgerskapPeriode();
        sp.setStatsborgerskap(new Statsborgerskap());
        r1.getStatsborgerskapListe().add(sp);

        when(personConsumer.hentPersonhistorikk(any())).thenReturn(r1);

        Saksopplysning saksopplysning = service.hentPersonhistorikk(AKTØRID_1.toString(), LocalDate.now());

        PersonhistorikkDokument dokument = (PersonhistorikkDokument) saksopplysning.getDokument();

        assertThat(dokument).isNotNull();
        assertThat(dokument.statsborgerskapListe).isNotNull();
    }

    @Test
    public void aktørIdFinnesICache() throws IkkeFunnetException, HentAktoerIdForIdentPersonIkkeFunnet, HentIdentForAktoerIdPersonIkkeFunnet {
        aktørIdCache.onApplicationEvent(null);

        String aktørId = service.hentAktørIdForIdent(FNR_1);
        assertEquals(Long.toString(AKTØRID_1), aktørId);

        String fnr = service.hentIdentForAktørId(aktørId);
        assertEquals(fnr, FNR_1);

        verify(aktorConsumer, times(1)).hentAktørIdForIdent(any());
        verify(aktorConsumer, times(0)).hentIdentForAktoerId(any());
    }

    @Test
    public void aktørIdTømtFraCache() throws IkkeFunnetException, InterruptedException, HentAktoerIdForIdentPersonIkkeFunnet, HentIdentForAktoerIdPersonIkkeFunnet {
        aktørIdCache.onApplicationEvent(null);

        String aktørId = service.hentAktørIdForIdent(FNR_1);
        assertEquals(Long.toString(AKTØRID_1), aktørId);

        sleep(1500);

        String fnr = service.hentIdentForAktørId(aktørId);
        assertEquals(fnr, FNR_1);

        verify(aktorConsumer, times(1)).hentAktørIdForIdent(any());
        verify(aktorConsumer, times(1)).hentIdentForAktoerId(any());
    }

    @Test
    public void hentSammensattNavn() throws Exception {
        service = spy(service);
        PersonDokument personDokument = new PersonDokument();
        String sammensattNavn = "sammensattNavn";
        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setDokument(personDokument);
        personDokument.sammensattNavn = sammensattNavn;

        String fnr = "fnr";
        doReturn(saksopplysning).when(service).hentPerson(fnr);

        assertThat(service.hentSammensattNavn(fnr)).isEqualTo(sammensattNavn);
    }

    private HentAktoerIdForIdentResponse hentAktørIdResponse() {

        // Feltet identHistorikk er protected, så vi utvider klassen med en setter for mocking
        class AktoerIdResponse extends HentAktoerIdForIdentResponse {
            private void setIdentHistorikk(List<IdentDetaljer> identHistorikk) {
                super.identHistorikk = identHistorikk;
            }
        }

        AktoerIdResponse aktørIdResponse = new AktoerIdResponse();
        aktørIdResponse.setAktoerId(Long.toString(AKTØRID_1));

        IdentDetaljer identDetaljer = new IdentDetaljer();
        identDetaljer.setTpsId(FNR_1);
        aktørIdResponse.setIdentHistorikk(new ArrayList<>());
        aktørIdResponse.getIdentHistorikk().add(identDetaljer);

        return aktørIdResponse;
    }
}
