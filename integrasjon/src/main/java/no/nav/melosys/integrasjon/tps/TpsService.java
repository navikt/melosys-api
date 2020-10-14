package no.nav.melosys.integrasjon.tps;

import java.io.StringWriter;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.XMLGregorianCalendar;

import no.nav.melosys.domain.person.Informasjonsbehov;
import no.nav.melosys.domain.Personopplysning;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.SaksopplysningKilde;
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.domain.dokument.DokumentFactory;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.integrasjon.KonverteringsUtils;
import no.nav.melosys.integrasjon.tps.aktoer.AktoerIdCache;
import no.nav.melosys.integrasjon.tps.aktoer.AktorConsumer;
import no.nav.melosys.integrasjon.tps.mapper.PersonopplysningMapper;
import no.nav.melosys.integrasjon.tps.person.PersonConsumer;
import no.nav.tjeneste.virksomhet.aktoer.v2.binding.HentAktoerIdForIdentPersonIkkeFunnet;
import no.nav.tjeneste.virksomhet.aktoer.v2.binding.HentIdentForAktoerIdPersonIkkeFunnet;
import no.nav.tjeneste.virksomhet.aktoer.v2.meldinger.HentAktoerIdForIdentRequest;
import no.nav.tjeneste.virksomhet.aktoer.v2.meldinger.HentAktoerIdForIdentResponse;
import no.nav.tjeneste.virksomhet.aktoer.v2.meldinger.HentIdentForAktoerIdRequest;
import no.nav.tjeneste.virksomhet.aktoer.v2.meldinger.HentIdentForAktoerIdResponse;
import no.nav.tjeneste.virksomhet.person.v3.binding.HentPersonPersonIkkeFunnet;
import no.nav.tjeneste.virksomhet.person.v3.binding.HentPersonSikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.person.v3.binding.HentPersonhistorikkPersonIkkeFunnet;
import no.nav.tjeneste.virksomhet.person.v3.binding.HentPersonhistorikkSikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.NorskIdent;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Periode;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.PersonIdent;
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentPersonRequest;
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentPersonResponse;
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentPersonhistorikkRequest;
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentPersonhistorikkResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Service
@Primary
public class TpsService implements TpsFasade {
    private static final String PERSON_VERSJON = "3.0";
    private static final String PERSONHISTORIKK_VERSJON = "3.4";

    private final AktorConsumer aktorConsumer;
    private final PersonConsumer personConsumer;
    private final DokumentFactory dokumentFactory;
    private final AktoerIdCache aktørIdCache;

    @Autowired
    public TpsService(AktorConsumer aktorConsumer,
                      PersonConsumer personConsumer,
                      DokumentFactory dokumentFactory,
                      AktoerIdCache aktørIdCache) {
        this.aktorConsumer = aktorConsumer;
        this.personConsumer = personConsumer;
        this.dokumentFactory = dokumentFactory;
        this.aktørIdCache = aktørIdCache;
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

    private Saksopplysning hentPerson(String ident, Collection<no.nav.tjeneste.virksomhet.person.v3.informasjon.Informasjonsbehov> behov) throws IkkeFunnetException, SikkerhetsbegrensningException, IntegrasjonException {
        HentPersonRequest request = new HentPersonRequest();
        NorskIdent norskIdent = new NorskIdent();
        norskIdent.setIdent(ident);

        PersonIdent personIdent = new PersonIdent();
        personIdent.setIdent(norskIdent);

        request.setAktoer(personIdent);
        request.getInformasjonsbehov().addAll(behov);

        // Kall til TPS
        HentPersonResponse response;
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
            dokumentFactory.createMarshaller().marshal(xmlRoot, xmlWriter);
        } catch (JAXBException e) {
            throw new IntegrasjonException(e);
        }

        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setDokumentXml(xmlWriter.toString());
        saksopplysning.setKilde(SaksopplysningKilde.TPS);
        saksopplysning.setType(SaksopplysningType.PERSOPL);
        saksopplysning.setVersjon(PERSON_VERSJON);

        // xml -> java objekter
        dokumentFactory.lagDokument(saksopplysning);

        return saksopplysning;
    }

    @Override
    public Saksopplysning hentPerson(String ident, Informasjonsbehov behov) throws IkkeFunnetException, SikkerhetsbegrensningException, IntegrasjonException {
        return hentPerson(ident, mapInformasjonsbehovTilTps(behov));
    }

    @Override
    public Personopplysning hentPersonopplysning(String ident) throws SikkerhetsbegrensningException, IkkeFunnetException {
        HentPersonRequest request = new HentPersonRequest();
        NorskIdent norskIdent = new NorskIdent();
        norskIdent.setIdent(ident);

        PersonIdent personIdent = new PersonIdent();
        personIdent.setIdent(norskIdent);

        request.setAktoer(personIdent);

        // Kall til TPS
        HentPersonResponse response;
        try {
            response = personConsumer.hentPerson(request);
        } catch (HentPersonSikkerhetsbegrensning hentPersonSikkerhetsbegrensning) {
            throw new SikkerhetsbegrensningException(hentPersonSikkerhetsbegrensning);
        } catch (HentPersonPersonIkkeFunnet hentPersonPersonIkkeFunnet) {
            throw new IkkeFunnetException(hentPersonPersonIkkeFunnet);
        }
        return PersonopplysningMapper.mapTilPersonopplysning(response.getPerson());
    }

    @Override
    public Saksopplysning hentPersonhistorikk(String ident, LocalDate dato) throws SikkerhetsbegrensningException, IkkeFunnetException, IntegrasjonException {
        HentPersonhistorikkRequest request = new HentPersonhistorikkRequest();
        NorskIdent norskIdent = new NorskIdent();
        norskIdent.setIdent(ident);

        PersonIdent personIdent = new PersonIdent();
        personIdent.setIdent(norskIdent);
        request.setAktoer(personIdent);

        Periode periode = new Periode();
        try {
            XMLGregorianCalendar xmlDato = KonverteringsUtils.localDateToXMLGregorianCalendar(dato);
            /*
            Når fom == tom leverer TPS all foregående historikk, mens fom < tom gir historikk med gyldighetsdato
            innenfor perioden det søkes på.
            */
            periode.setFom(xmlDato);
            periode.setTom(xmlDato);
        } catch (DatatypeConfigurationException e) {
            throw new IntegrasjonException(e);
        }
        request.setPeriode(periode);

        // Kall til TPS
        HentPersonhistorikkResponse response;
        try {
            response = personConsumer.hentPersonhistorikk(request);
        } catch (HentPersonhistorikkSikkerhetsbegrensning hentPersonhistorikkSikkerhetsbegrensning) {
            throw new SikkerhetsbegrensningException(hentPersonhistorikkSikkerhetsbegrensning);
        } catch (HentPersonhistorikkPersonIkkeFunnet hentPersonhistorikkPersonIkkeFunnet) {
            throw new IkkeFunnetException(hentPersonhistorikkPersonIkkeFunnet);
        }

        StringWriter xmlWriter = new StringWriter();
        try {
            no.nav.tjeneste.virksomhet.person.v3.HentPersonhistorikkResponse xmlRoot = new no.nav.tjeneste.virksomhet.person.v3.HentPersonhistorikkResponse();
            xmlRoot.setResponse(response);
            dokumentFactory.createMarshaller().marshal(xmlRoot, xmlWriter);
        } catch (JAXBException e) {
            throw new IntegrasjonException(e);
        }

        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setDokumentXml(xmlWriter.toString());
        saksopplysning.setKilde(SaksopplysningKilde.TPS);
        saksopplysning.setType(SaksopplysningType.PERSHIST);
        saksopplysning.setVersjon(PERSONHISTORIKK_VERSJON);

        // xml -> java objekter
        dokumentFactory.lagDokument(saksopplysning);

        return saksopplysning;
    }

    @Override
    public String hentSammensattNavn(String fnr) throws FunksjonellException, IntegrasjonException {
        Saksopplysning tpsOpplysning = hentPerson(fnr, Informasjonsbehov.INGEN);
        PersonDokument personDokument = (PersonDokument) tpsOpplysning.getDokument();
        return personDokument != null ? personDokument.sammensattNavn : null;
    }

    private Set<no.nav.tjeneste.virksomhet.person.v3.informasjon.Informasjonsbehov> mapInformasjonsbehovTilTps(Informasjonsbehov behov) {
        switch (behov) {
            case STANDARD:
                return Set.of(no.nav.tjeneste.virksomhet.person.v3.informasjon.Informasjonsbehov.ADRESSE);
            case MED_FAMILIERELASJONER:
                return Set.of(no.nav.tjeneste.virksomhet.person.v3.informasjon.Informasjonsbehov.ADRESSE,
                    no.nav.tjeneste.virksomhet.person.v3.informasjon.Informasjonsbehov.FAMILIERELASJONER);
            default:
                return Collections.emptySet();
        }
    }
}
