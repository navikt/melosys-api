package no.nav.melosys.integrasjon.ereg;

import java.io.StringWriter;
import java.util.Optional;
import javax.xml.bind.JAXBException;
import javax.xml.ws.soap.SOAPFaultException;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Service
@Primary
public class EregService implements EregFasade {
    private static final Logger log = LoggerFactory.getLogger(EregService.class);
    private static final String ORGANISASJON_VERSJON = "4.0";

    private final OrganisasjonConsumer organisasjonConsumer;
    private final DokumentFactory dokumentFactory;

    public EregService(OrganisasjonConsumer organisasjonConsumer, DokumentFactory dokumentFactory) {
        this.organisasjonConsumer = organisasjonConsumer;
        this.dokumentFactory = dokumentFactory;
    }

    @Override
    public Saksopplysning hentOrganisasjon(String orgnr) {
        StringWriter xmlWriter = new StringWriter();

        try {
            HentOrganisasjonResponse response = hentOrganisasjonResponse(orgnr);
            no.nav.tjeneste.virksomhet.organisasjon.v4.HentOrganisasjonResponse xmlRoot = new no.nav.tjeneste.virksomhet.organisasjon.v4.HentOrganisasjonResponse();
            xmlRoot.setResponse(response);
            dokumentFactory.createMarshaller().marshal(xmlRoot, xmlWriter);
        } catch (SOAPFaultException | JAXBException e) {
            log.warn("Fikk feil etter vi forsøkte å finne dette orgnr fra Ereg: {}", orgnr);
            throw new IntegrasjonException(e);
        }

        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.leggTilKildesystemOgMottattDokument(
            SaksopplysningKildesystem.EREG, xmlWriter.toString());
        saksopplysning.setType(SaksopplysningType.ORG);
        saksopplysning.setVersjon(ORGANISASJON_VERSJON);

        dokumentFactory.lagDokument(saksopplysning);

        return saksopplysning;
    }

    @Override
    public Optional<Saksopplysning> finnOrganisasjon(String orgnr) {
        try {
            return Optional.ofNullable(hentOrganisasjon(orgnr));
        } catch (IkkeFunnetException ex) {
            log.warn("Fant ikke organisasjon med orgnr {}", orgnr);
            return Optional.empty();
        }
    }

    @Override
    public String hentOrganisasjonNavn(String orgnr) {
        OrganisasjonDokument organisasjonDokument = (OrganisasjonDokument) hentOrganisasjon(orgnr).getDokument();
        return organisasjonDokument.getNavn();
    }

    private HentOrganisasjonResponse hentOrganisasjonResponse(String orgnr) {
        HentOrganisasjonRequest request = new HentOrganisasjonRequest();
        request.setOrgnummer(orgnr);

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
