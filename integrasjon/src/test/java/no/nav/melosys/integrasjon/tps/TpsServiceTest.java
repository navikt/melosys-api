package no.nav.melosys.integrasjon.tps;

import java.time.LocalDate;

import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.dokument.DokumentFactory;
import no.nav.melosys.domain.dokument.XsltTemplatesFactory;
import no.nav.melosys.domain.dokument.jaxb.JaxbConfig;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.dokument.person.PersonhistorikkDokument;
import no.nav.melosys.domain.person.Informasjonsbehov;
import no.nav.melosys.integrasjon.tps.person.PersonConsumer;
import no.nav.tjeneste.virksomhet.aktoer.v2.binding.HentAktoerIdForIdentPersonIkkeFunnet;
import no.nav.tjeneste.virksomhet.aktoer.v2.binding.HentIdentForAktoerIdPersonIkkeFunnet;
import no.nav.tjeneste.virksomhet.aktoer.v2.meldinger.HentIdentForAktoerIdResponse;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.*;
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentPersonResponse;
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentPersonhistorikkResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class TpsServiceTest {
    private static final String FNR = "88888888888";
    private static final Long AKTØRID_1 = 1000021552067L;

    private PersonConsumer personConsumer;
    private TpsService service;

    @BeforeEach
    public void setUp() throws HentAktoerIdForIdentPersonIkkeFunnet, HentIdentForAktoerIdPersonIkkeFunnet {
        personConsumer = mock(PersonConsumer.class);

        HentIdentForAktoerIdResponse identResponse = new HentIdentForAktoerIdResponse();
        identResponse.setIdent(FNR);

        DokumentFactory dokumentFactory = new DokumentFactory(JaxbConfig.jaxb2Marshaller(), new XsltTemplatesFactory());

        service = new TpsService(personConsumer, dokumentFactory);
    }

    @Test
    public void hentPerson() throws Exception {
        HentPersonResponse r1 = new HentPersonResponse();
        Person person = new Person().withAktoer(new PersonIdent().withIdent(new NorskIdent().withIdent(AKTØRID_1.toString())));
        r1.setPerson(person);

        when(personConsumer.hentPerson(any())).thenReturn(r1);

        Saksopplysning saksopplysning = service.hentPerson(AKTØRID_1.toString(), Informasjonsbehov.INGEN);

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
    public void hentSammensattNavn() throws Exception {
        service = spy(service);
        PersonDokument personDokument = new PersonDokument();
        String sammensattNavn = "sammensattNavn";
        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setDokument(personDokument);
        personDokument.sammensattNavn = sammensattNavn;

        String fnr = "fnr";
        doReturn(saksopplysning).when(service).hentPerson(fnr, Informasjonsbehov.INGEN);

        assertThat(service.hentSammensattNavn(fnr)).isEqualTo(sammensattNavn);
    }
}
