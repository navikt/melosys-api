package no.nav.melosys.integrasjon.tps;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.SaksopplysningKilde;
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.domain.dokument.DokumentFactory;
import no.nav.melosys.integrasjon.tps.aktoer.AktorConsumer;
import no.nav.melosys.integrasjon.tps.person.PersonConsumer;
import no.nav.tjeneste.virksomhet.aktoer.v2.binding.HentAktoerIdForIdentPersonIkkeFunnet;
import no.nav.tjeneste.virksomhet.aktoer.v2.binding.HentIdentForAktoerIdPersonIkkeFunnet;
import no.nav.tjeneste.virksomhet.aktoer.v2.meldinger.HentAktoerIdForIdentRequest;
import no.nav.tjeneste.virksomhet.aktoer.v2.meldinger.HentAktoerIdForIdentResponse;
import no.nav.tjeneste.virksomhet.aktoer.v2.meldinger.HentIdentForAktoerIdRequest;
import no.nav.tjeneste.virksomhet.aktoer.v2.meldinger.HentIdentForAktoerIdResponse;
import no.nav.tjeneste.virksomhet.person.v3.binding.HentPersonPersonIkkeFunnet;
import no.nav.tjeneste.virksomhet.person.v3.binding.HentPersonSikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Informasjonsbehov;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.NorskIdent;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.PersonIdent;
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentPersonRequest;
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentPersonResponse;

@Service
public class TpsService implements TpsFasade {

    private static final Logger log = LoggerFactory.getLogger(TpsService.class);

    private static final String PERSON_VERSJON = "3.0";

    private AktorConsumer aktorConsumer;

    private PersonConsumer personConsumer;

    private DokumentFactory dokumentFactory;

    private final Marshaller marshaller;

    @Autowired
    public TpsService(AktorConsumer aktorConsumer, PersonConsumer personConsumer, DokumentFactory dokumentFactory) {
        this.aktorConsumer = aktorConsumer;
        this.personConsumer = personConsumer;
        this.dokumentFactory = dokumentFactory;

        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(no.nav.tjeneste.virksomhet.person.v3.HentPersonResponse.class);
            marshaller = jaxbContext.createMarshaller();
        } catch (JAXBException e) {
            log.error("", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public Optional<String> hentAktørIdForIdent(String fnr) {
        HentAktoerIdForIdentRequest request = new HentAktoerIdForIdentRequest();
        request.setIdent(fnr);

        Optional<String> optResult = null;
        try {
            HentAktoerIdForIdentResponse response = aktorConsumer.hentAktørIdForIdent(request);
            String aktørId = response.getAktoerId();
            optResult = Optional.of(aktørId);
        } catch (HentAktoerIdForIdentPersonIkkeFunnet e) { // NOSONAR
            optResult = Optional.empty();
        }
        return optResult;
    }

    @Override
    public Optional<String> hentIdentForAktørId(String aktørID) {
        HentIdentForAktoerIdRequest request = new HentIdentForAktoerIdRequest();
        request.setAktoerId(aktørID);
        
        Optional<String> optResult = null;
        try {
            HentIdentForAktoerIdResponse response = aktorConsumer.hentIdentForAktoerId(request);
            optResult = Optional.of(response.getIdent());
        } catch (HentIdentForAktoerIdPersonIkkeFunnet hentIdentForAktoerIdPersonIkkeFunnet) { // NOSONAR
            optResult = Optional.empty();
        }

        return optResult;
    }

    @Override
    public Saksopplysning hentPerson(String ident, Collection<Informasjonsbehov> behov) throws HentPersonPersonIkkeFunnet, HentPersonSikkerhetsbegrensning {
        HentPersonRequest request = new HentPersonRequest();
        NorskIdent norskIdent = new NorskIdent();
        norskIdent.setIdent(ident);

        PersonIdent personIdent = new PersonIdent();
        personIdent.setIdent(norskIdent);

        request.setAktoer(personIdent);
        if (behov != null) {
            request.getInformasjonsbehov().addAll(behov);
        }

        // Kall til TPS
        HentPersonResponse response = personConsumer.hentPerson(request);

        // Response -> xml
        StringWriter xmlWriter = new StringWriter();
        try {
            no.nav.tjeneste.virksomhet.person.v3.HentPersonResponse xmlRoot = new no.nav.tjeneste.virksomhet.person.v3.HentPersonResponse();
            xmlRoot.setResponse(response);
            marshaller.marshal(xmlRoot, xmlWriter);
        } catch (JAXBException e) {
            log.error("", e);
            throw new RuntimeException(e);
        }

        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setDokumentXml(xmlWriter.toString());
        saksopplysning.setKilde(SaksopplysningKilde.TPS);
        saksopplysning.setType(SaksopplysningType.PERSONOPPLYSNING);
        saksopplysning.setVersjon(PERSON_VERSJON);

        // xml -> java objekter
        dokumentFactory.lagDokument(saksopplysning);

        return saksopplysning;
    }

    @Override
    @Deprecated
    public HentPersonResponse hentPersonMedAdresse(String ident) throws HentPersonPersonIkkeFunnet, HentPersonSikkerhetsbegrensning {
        Collection<Informasjonsbehov> behov = new ArrayList<>();
        behov.add(Informasjonsbehov.ADRESSE);

        HentPersonRequest request = new HentPersonRequest();
        NorskIdent norskIdent = new NorskIdent();
        norskIdent.setIdent(ident);

        PersonIdent personIdent = new PersonIdent();
        personIdent.setIdent(norskIdent);

        request.setAktoer(personIdent);
        if (behov != null) {
            request.getInformasjonsbehov().addAll(behov);
        }

        return personConsumer.hentPerson(request);
    }

}
