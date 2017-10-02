package no.nav.melosys.integrasjon.ereg;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import org.junit.Before;
import org.junit.Test;

import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.dokument.DokumentFactory;
import no.nav.melosys.domain.dokument.XsltTemplatesFactory;
import no.nav.melosys.domain.dokument.jaxb.JaxbConfig;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.integrasjon.ereg.organisasjon.OrganisasjonConsumer;
import no.nav.melosys.integrasjon.ereg.organisasjon.OrganisasjonMock;

public class EregServiceTest {

    private EregService eregService;

    @Before
    public void setUp() {
        OrganisasjonConsumer organisasjonMock = new OrganisasjonMock();
        DokumentFactory dokumentFactory = new DokumentFactory(new JaxbConfig().jaxb2Marshaller(), new XsltTemplatesFactory());
        eregService = new EregService(organisasjonMock, dokumentFactory);
    }

    @Test
    public void getOrganisasjon() throws Exception {
        Saksopplysning saksopplysning = eregService.getOrganisasjon("873102322");
        OrganisasjonDokument organisasjonDokument = (OrganisasjonDokument) saksopplysning.getDokument();
        assertThat(organisasjonDokument.getOrganisasjonDetaljer().getNavn().get(0).getRedigertNavn()).isEqualTo("MULTICONSULT ASA");
    }

}