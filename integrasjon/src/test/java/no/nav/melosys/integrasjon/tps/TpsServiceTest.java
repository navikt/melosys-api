package no.nav.melosys.integrasjon.tps;

import java.time.LocalDate;

import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.dokument.DokumentFactory;
import no.nav.melosys.domain.dokument.XsltTemplatesFactory;
import no.nav.melosys.domain.dokument.jaxb.JaxbConfig;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.dokument.person.PersonhistorikkDokument;
import no.nav.melosys.domain.person.Informasjonsbehov;
import no.nav.melosys.integrasjon.kodeverk.KodeOppslag;
import no.nav.melosys.integrasjon.tps.person.PersonConsumer;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.*;
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentPersonResponse;
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentPersonhistorikkResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TpsServiceTest {
    private static final Long AKTØRID_1 = 1000021552067L;

    @Mock
    private PersonConsumer personConsumer;
    @Mock
    private KodeOppslag kodeOppslag;

    private TpsService service;

    @BeforeEach
    void setUp() {
        DokumentFactory dokumentFactory = new DokumentFactory(JaxbConfig.jaxb2Marshaller(), new XsltTemplatesFactory());

        service = new TpsService(personConsumer, dokumentFactory, kodeOppslag);
    }

    @Test
    void hentPerson() throws Exception {
        HentPersonResponse r1 = new HentPersonResponse();
        Person person = new Person().withAktoer(new PersonIdent().withIdent(new NorskIdent().withIdent(AKTØRID_1.toString())));
        r1.setPerson(person);

        when(personConsumer.hentPerson(any())).thenReturn(r1);

        Saksopplysning saksopplysning = service.hentPerson(AKTØRID_1.toString(), Informasjonsbehov.INGEN);

        PersonDokument dokument = (PersonDokument) saksopplysning.getDokument();
        assertThat(dokument.hentFolkeregisterident()).isEqualTo(AKTØRID_1.toString());
    }

    @Test
    void hentPersonhistorikk() throws Exception {
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
    void hentSammensattNavn() {
        service = spy(service);
        PersonDokument personDokument = new PersonDokument();
        String sammensattNavn = "sammensattNavn";
        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setDokument(personDokument);
        personDokument.setSammensattNavn(sammensattNavn);

        String fnr = "fnr";
        doReturn(saksopplysning).when(service).hentPerson(fnr, Informasjonsbehov.INGEN);

        assertThat(service.hentSammensattNavn(fnr)).isEqualTo(sammensattNavn);
    }
}
