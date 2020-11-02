package no.nav.melosys.integrasjon.ereg;

import java.io.StringWriter;
import javax.xml.bind.JAXBException;

import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.SaksopplysningKildesystem;
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.domain.dokument.DokumentFactory;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.integrasjon.ereg.organisasjon.OrganisasjonConsumer;
import no.nav.tjeneste.virksomhet.organisasjon.v4.binding.HentOrganisasjonOrganisasjonIkkeFunnet;
import no.nav.tjeneste.virksomhet.organisasjon.v4.binding.HentOrganisasjonUgyldigInput;
import no.nav.tjeneste.virksomhet.organisasjon.v4.meldinger.HentOrganisasjonRequest;
import no.nav.tjeneste.virksomhet.organisasjon.v4.meldinger.HentOrganisasjonResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Service
@Primary
public class EregService implements EregFasade {
    private static final String ORGANISASJON_VERSJON = "4.0";

    private final OrganisasjonConsumer organisasjonConsumer;
    private final DokumentFactory dokumentFactory;

    @Autowired
    public EregService(OrganisasjonConsumer organisasjonConsumer, DokumentFactory dokumentFactory) {
        this.organisasjonConsumer = organisasjonConsumer;
        this.dokumentFactory = dokumentFactory;
    }

    @Override
    public Saksopplysning hentOrganisasjon(String orgnummer) throws IkkeFunnetException, IntegrasjonException {
        HentOrganisasjonResponse response = hentOrganisasjonResponse(orgnummer);

        // Response -> xml
        StringWriter xmlWriter = new StringWriter();
        try {
            no.nav.tjeneste.virksomhet.organisasjon.v4.HentOrganisasjonResponse xmlRoot = new no.nav.tjeneste.virksomhet.organisasjon.v4.HentOrganisasjonResponse();
            xmlRoot.setResponse(response);
            dokumentFactory.createMarshaller().marshal(xmlRoot, xmlWriter);
        } catch (JAXBException e) {
            throw new IntegrasjonException(e);
        }

        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.leggTilKildesystemOgMottattDokument(
            SaksopplysningKildesystem.EREG, xmlWriter.toString());
        saksopplysning.setType(SaksopplysningType.ORG);
        saksopplysning.setVersjon(ORGANISASJON_VERSJON);

        // xml -> java objekter
        dokumentFactory.lagDokument(saksopplysning);

        return saksopplysning;
    }

    @Override
    public String hentOrganisasjonNavn(String orgnummer) throws IkkeFunnetException, IntegrasjonException {
        OrganisasjonDokument organisasjonDokument = (OrganisasjonDokument) hentOrganisasjon(orgnummer).getDokument();
        return organisasjonDokument.getNavn();
    }

    @Override
    public boolean organisasjonFinnes(String orgnummer) {
        try {
            return hentOrganisasjonResponse(orgnummer) != null;
        } catch (IkkeFunnetException|IntegrasjonException e) {
            return false;
        }
    }

    private HentOrganisasjonResponse hentOrganisasjonResponse(String orgnummer) throws IkkeFunnetException, IntegrasjonException {
        HentOrganisasjonRequest request = new HentOrganisasjonRequest();
        request.setOrgnummer(orgnummer);

        HentOrganisasjonResponse response;
        try {
            response = organisasjonConsumer.hentOrganisasjon(request);
        } catch (HentOrganisasjonOrganisasjonIkkeFunnet hentOrganisasjonOrganisasjonIkkeFunnet) {
            throw new IkkeFunnetException(hentOrganisasjonOrganisasjonIkkeFunnet);
        } catch (HentOrganisasjonUgyldigInput hentOrganisasjonUgyldigInput) {
            throw new IntegrasjonException(hentOrganisasjonUgyldigInput);
        }
        return response;
    }
}
