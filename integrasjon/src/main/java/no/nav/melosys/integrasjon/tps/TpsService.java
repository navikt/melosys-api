package no.nav.melosys.integrasjon.tps;

import java.io.StringWriter;
import java.time.LocalDate;
import java.util.*;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.XMLGregorianCalendar;

import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.SaksopplysningKilde;
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.domain.dokument.DokumentFactory;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.integrasjon.KonverteringsUtils;
import no.nav.melosys.integrasjon.tps.aktoer.AktoerIdCache;
import no.nav.melosys.integrasjon.tps.aktoer.AktorConsumer;
import no.nav.melosys.integrasjon.tps.person.PersonConsumer;
import no.nav.tjeneste.virksomhet.aktoer.v2.binding.HentAktoerIdForIdentPersonIkkeFunnet;
import no.nav.tjeneste.virksomhet.aktoer.v2.binding.HentIdentForAktoerIdPersonIkkeFunnet;
import no.nav.tjeneste.virksomhet.aktoer.v2.meldinger.HentAktoerIdForIdentRequest;
import no.nav.tjeneste.virksomhet.aktoer.v2.meldinger.HentAktoerIdForIdentResponse;
import no.nav.tjeneste.virksomhet.aktoer.v2.meldinger.HentIdentForAktoerIdRequest;
import no.nav.tjeneste.virksomhet.aktoer.v2.meldinger.HentIdentForAktoerIdResponse;
import no.nav.tjeneste.virksomhet.person.v3.binding.*;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.*;
import no.nav.tjeneste.virksomhet.person.v3.meldinger.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Service
@Primary
public class TpsService implements TpsFasade {

    private static final Logger log = LoggerFactory.getLogger(TpsService.class);

    private static final String PERSON_VERSJON = "3.0";

    private final AktorConsumer aktorConsumer;

    private final PersonConsumer personConsumer;

    private final DokumentFactory dokumentFactory;

    private final AktoerIdCache aktørIdCache;

    private final JAXBContext jaxbContext;

    // Endringstidspunkt sorteres fra nyest til eldst.
    static final Comparator<StatsborgerskapPeriode> endringstidspunktKomparator =
        (sp1, sp2) -> sp2.getEndringstidspunkt().compare(sp1.getEndringstidspunkt());

    @Autowired
    public TpsService(AktorConsumer aktorConsumer, PersonConsumer personConsumer, DokumentFactory dokumentFactory, AktoerIdCache aktørIdCache) {
        this.aktorConsumer = aktorConsumer;
        this.personConsumer = personConsumer;
        this.dokumentFactory = dokumentFactory;
        this.aktørIdCache = aktørIdCache;

        try {
            jaxbContext = JAXBContext.newInstance(no.nav.tjeneste.virksomhet.person.v3.HentPersonResponse.class);
        } catch (JAXBException e) {
            log.error("", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public String hentAktørIdForIdent(String fnr) throws IkkeFunnetException {
        if (aktørIdCache.hentAktørIdFraCache(fnr) != null) {
            return aktørIdCache.hentAktørIdFraCache(fnr);
        }

        HentAktoerIdForIdentRequest request = new HentAktoerIdForIdentRequest();
        request.setIdent(fnr);

        try {
            HentAktoerIdForIdentResponse response = aktorConsumer.hentAktørIdForIdent(request);

            // I følge kontrakten kan ident-historikken være tom
            if (response.getIdentHistorikk() != null && !response.getIdentHistorikk().isEmpty()) {
                String gjeldendeFnr = response.getIdentHistorikk().get(0).getTpsId();
                aktørIdCache.leggTilCache(gjeldendeFnr, response.getAktoerId());
            }

            return response.getAktoerId();
        } catch (HentAktoerIdForIdentPersonIkkeFunnet e) { // NOSONAR
            throw new IkkeFunnetException(e);
        }
    }

    @Override
    public String hentIdentForAktørId(String aktørID) throws IkkeFunnetException {
        if (aktørIdCache.hentIdentFraCache(aktørID) != null) {
            return aktørIdCache.hentIdentFraCache(aktørID);
        }

        HentIdentForAktoerIdRequest request = new HentIdentForAktoerIdRequest();
        request.setAktoerId(aktørID);

        try {
            HentIdentForAktoerIdResponse response = aktorConsumer.hentIdentForAktoerId(request);
            aktørIdCache.leggTilCache(response.getIdent(), aktørID);
            return response.getIdent();
        } catch (HentIdentForAktoerIdPersonIkkeFunnet e) { // NOSONAR
            throw new IkkeFunnetException(e);
        }
    }

    private Saksopplysning hentPerson(String ident, Collection<Informasjonsbehov> behov) throws IkkeFunnetException, SikkerhetsbegrensningException {
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
        HentPersonResponse response = null;
        try {
            response = personConsumer.hentPerson(request);
        } catch (HentPersonSikkerhetsbegrensning hentPersonSikkerhetsbegrensning) {
            throw new SikkerhetsbegrensningException(hentPersonSikkerhetsbegrensning);
        } catch (HentPersonPersonIkkeFunnet hentPersonPersonIkkeFunnet) {
            throw new IkkeFunnetException(hentPersonPersonIkkeFunnet);
        }

        // Response -> xml
        StringWriter xmlWriter = new StringWriter();
        try {
            no.nav.tjeneste.virksomhet.person.v3.HentPersonResponse xmlRoot = new no.nav.tjeneste.virksomhet.person.v3.HentPersonResponse();
            xmlRoot.setResponse(response);
            jaxbContext.createMarshaller().marshal(xmlRoot, xmlWriter);
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
    public Saksopplysning hentPerson(String ident) throws IkkeFunnetException, SikkerhetsbegrensningException {
        return hentPerson(ident, null);
    }

    @Override
    public Saksopplysning hentPersonMedAdresse(String ident) throws IkkeFunnetException, SikkerhetsbegrensningException {
        Collection<Informasjonsbehov> behov = new ArrayList<>();
        behov.add(Informasjonsbehov.ADRESSE);

        return hentPerson(ident, behov);
    }

    @Override
    public int hentAntallPersonerSomBorPåBostedsadresse(String argAktørId) throws IntegrasjonException {
        //Hvis adressedato ikke gitt som request paramerter da tjensten antar at adresseDato er dagensdato.
        HentPersonerMedSammeAdresseRequest request = new HentPersonerMedSammeAdresseRequest();
        AktoerId aktørId = new AktoerId();
        aktørId.setAktoerId(argAktørId);
        try {
            HentPersonerMedSammeAdresseResponse response = personConsumer.hentPersonerMedSammeAdresse(request);
            if (response != null) {
                return response.getPersonBorHerListe().size();
            } else {
                throw new IntegrasjonException("Feil ved henting av antall personer bor på bostedsadresse");
            }
        } catch (HentPersonerMedSammeAdresseSikkerhetsbegrensning hentPersonerMedSammeAdresseSikkerhetsbegrensning) {
            throw new IntegrasjonException(hentPersonerMedSammeAdresseSikkerhetsbegrensning.getMessage());
        } catch (HentPersonerMedSammeAdresseIkkeFunnet hentPersonerMedSammeAdresseIkkeFunnet) {
            throw new IntegrasjonException(hentPersonerMedSammeAdresseIkkeFunnet.getMessage());
        }
    }

    @Override
    public String hentStatsborgerskapPåGittDato(String ident, LocalDate dato) throws IkkeFunnetException, SikkerhetsbegrensningException, IntegrasjonException {
        if (dato == null) {
            throw new IntegrasjonException("Dato kan ikke være null");
        }

        HentPersonhistorikkRequest request = new HentPersonhistorikkRequest();
        NorskIdent norskIdent = new NorskIdent();
        norskIdent.setIdent(ident);

        PersonIdent personIdent = new PersonIdent();
        personIdent.setIdent(norskIdent);

        request.setAktoer(personIdent);

        try {
            XMLGregorianCalendar xmlDato = KonverteringsUtils.localDateToXMLGregorianCalendar(dato);
            Periode periode = new Periode();
            periode.setTom(xmlDato);
            periode.setFom(xmlDato);
            request.setPeriode(periode);

            HentPersonhistorikkResponse response = personConsumer.hentPersonhistorikk(request);
            List<StatsborgerskapPeriode> liste = response.getStatsborgerskapListe();

            if (liste.isEmpty()) {
                throw new IkkeFunnetException("Fant ikke statsborgerskap for dato " + dato);
            }

            liste.sort(endringstidspunktKomparator);

            Statsborgerskap statsborgerskap = liste.get(0).getStatsborgerskap();
            return statsborgerskap.getLand().getValue();
        } catch (DatatypeConfigurationException e) {
            throw new IntegrasjonException("Kunne ikke konvertere dato");
        } catch (HentPersonhistorikkPersonIkkeFunnet e) {
            throw new IkkeFunnetException(e);
        } catch (HentPersonhistorikkSikkerhetsbegrensning e) {
            throw new SikkerhetsbegrensningException(e);
        }
    }
}
