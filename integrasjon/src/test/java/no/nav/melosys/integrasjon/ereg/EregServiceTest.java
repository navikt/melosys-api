package no.nav.melosys.integrasjon.ereg;

import java.util.Optional;

import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.dokument.DokumentFactory;
import no.nav.melosys.domain.dokument.XsltTemplatesFactory;
import no.nav.melosys.domain.dokument.jaxb.JaxbConfig;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.integrasjon.ereg.organisasjon.OrganisasjonConsumer;
import no.nav.melosys.integrasjon.ereg.organisasjon.OrganisasjonMock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class EregServiceTest {
    private EregService eregService;

    @BeforeEach
    void setUp() {
        OrganisasjonConsumer organisasjonMock = new OrganisasjonMock();
        DokumentFactory dokumentFactory = new DokumentFactory(JaxbConfig.getJaxb2Marshaller(), new XsltTemplatesFactory());
        eregService = new EregService(organisasjonMock, dokumentFactory);
    }

    @Test
    void getOrganisasjon() {
        Saksopplysning saksopplysning = eregService.hentOrganisasjon("873102322");
        OrganisasjonDokument organisasjonDokument = (OrganisasjonDokument) saksopplysning.getDokument();
        assertThat(organisasjonDokument.organisasjonDetaljer.navn.get(0).redigertNavn).isEqualTo("MULTICONSULT ASA");
    }

    @Test
    void finnOrganisasjon_finnerOrganisasjon_returnererMedVerdi() {
        Optional<Saksopplysning> saksopplysning = eregService.finnOrganisasjon("873102322");
        assertThat(saksopplysning).isPresent();
    }

    @Test
    void finnOrganisasjon_finnerIkkeOrganisasjon_returnererTomVerdi() {
        Optional<Saksopplysning> saksopplysning = eregService.finnOrganisasjon("111111111");
        assertThat(saksopplysning).isNotPresent();
    }
}
