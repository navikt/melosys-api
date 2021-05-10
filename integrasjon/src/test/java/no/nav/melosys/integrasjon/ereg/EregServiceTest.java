package no.nav.melosys.integrasjon.ereg;

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

public class EregServiceTest {
    private EregService eregService;

    @BeforeEach
    public void setUp() {
        OrganisasjonConsumer organisasjonMock = new OrganisasjonMock();
        DokumentFactory dokumentFactory = new DokumentFactory(JaxbConfig.jaxb2Marshaller(), new XsltTemplatesFactory());
        eregService = new EregService(organisasjonMock, dokumentFactory);
    }

    @Test
    public void getOrganisasjon() throws Exception {
        Saksopplysning saksopplysning = eregService.hentOrganisasjon("873102322");
        OrganisasjonDokument organisasjonDokument = (OrganisasjonDokument) saksopplysning.getDokument();
        assertThat(organisasjonDokument.getOrganisasjonDetaljer().getNavn().get(0).getRedigertNavn()).isEqualTo("MULTICONSULT ASA");
    }

}
