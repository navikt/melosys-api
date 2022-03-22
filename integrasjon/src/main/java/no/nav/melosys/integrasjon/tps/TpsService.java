package no.nav.melosys.integrasjon.tps;

import java.io.StringWriter;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Set;
import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.XMLGregorianCalendar;

import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.SaksopplysningKildesystem;
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.domain.dokument.DokumentFactory;
import no.nav.melosys.domain.dokument.person.Familiemedlem;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.person.Informasjonsbehov;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.integrasjon.KonverteringsUtils;
import no.nav.melosys.integrasjon.kodeverk.KodeOppslag;
import no.nav.melosys.integrasjon.tps.mapper.PersonMapper;
import no.nav.melosys.integrasjon.tps.mapper.PersonMedKilde;
import no.nav.melosys.integrasjon.tps.person.PersonConsumer;
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
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Service
@Primary
public class TpsService implements TpsFasade {
    private static final String PERSON_VERSJON = "3.0";
    private static final String PERSONHISTORIKK_VERSJON = "3.4";

    private final PersonConsumer personConsumer;
    private final DokumentFactory dokumentFactory;
    private final KodeOppslag kodeOppslag;

    public TpsService(PersonConsumer personConsumer, DokumentFactory dokumentFactory, KodeOppslag kodeOppslag) {
        this.personConsumer = personConsumer;
        this.dokumentFactory = dokumentFactory;
        this.kodeOppslag = kodeOppslag;
    }

    @Override
    public Saksopplysning hentPerson(String ident, Informasjonsbehov behov) {
        PersonMedKilde personMedKilde = hentPersonMedKilde(ident, mapInformasjonsbehovTilTps(behov));
        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.leggTilKildesystemOgMottattDokument(
            SaksopplysningKildesystem.TPS, personMedKilde.dokumentXml);
        saksopplysning.setType(SaksopplysningType.PERSOPL);
        saksopplysning.setVersjon(PERSON_VERSJON);

        for (Familiemedlem familiemedlem : personMedKilde.dokument.getFamiliemedlemmer()) {
            PersonMedKilde personIFamilie = hentPersonMedKilde(familiemedlem.fnr, mapInformasjonsbehovTilTps(Informasjonsbehov.MED_FAMILIERELASJONER));
            PersonMapper.berikFamiliemedlemMedOpplysninger(familiemedlem, personIFamilie.dokument, ident);
            saksopplysning.leggTilKildesystemOgMottattDokument(
                SaksopplysningKildesystem.TPS, personIFamilie.dokumentXml);
        }
        saksopplysning.setDokument(personMedKilde.dokument);

        return saksopplysning;
    }

    private PersonMedKilde hentPersonMedKilde(String ident, Set<no.nav.tjeneste.virksomhet.person.v3.informasjon.Informasjonsbehov> behov) {
        HentPersonRequest request = new HentPersonRequest();
        NorskIdent norskIdent = new NorskIdent();
        norskIdent.setIdent(ident);

        PersonIdent personIdent = new PersonIdent();
        personIdent.setIdent(norskIdent);

        request.setAktoer(personIdent);
        request.getInformasjonsbehov().addAll(behov);

        HentPersonResponse response = hentPersonResponseFraTPS(request);

        StringWriter xmlWriter = new StringWriter();
        try {
            no.nav.tjeneste.virksomhet.person.v3.HentPersonResponse xmlRoot = new no.nav.tjeneste.virksomhet.person.v3.HentPersonResponse();
            xmlRoot.setResponse(response);
            dokumentFactory.createMarshaller().marshal(xmlRoot, xmlWriter);
        } catch (JAXBException e) {
            throw new IntegrasjonException(e);
        }
        PersonDokument dokument = PersonMapper.mapTilPerson(response.getPerson(), kodeOppslag);
        return new PersonMedKilde(dokument, xmlWriter.toString());
    }

    @Override
    public Saksopplysning hentPersonhistorikk(String ident, LocalDate dato) {
        HentPersonhistorikkRequest request = new HentPersonhistorikkRequest();
        NorskIdent norskIdent = new NorskIdent();
        norskIdent.setIdent(ident);

        PersonIdent personIdent = new PersonIdent();
        personIdent.setIdent(norskIdent);
        request.setAktoer(personIdent);

        Periode periode = hentAltForegaaendeHistorikkFraTPS(dato);
        request.setPeriode(periode);

        HentPersonhistorikkResponse response = hentPersonhistorikkResponseFraTPS(request);

        StringWriter xmlWriter = new StringWriter();
        try {
            no.nav.tjeneste.virksomhet.person.v3.HentPersonhistorikkResponse xmlRoot = new no.nav.tjeneste.virksomhet.person.v3.HentPersonhistorikkResponse();
            xmlRoot.setResponse(response);
            dokumentFactory.createMarshaller().marshal(xmlRoot, xmlWriter);
        } catch (JAXBException e) {
            throw new IntegrasjonException(e);
        }

        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.leggTilKildesystemOgMottattDokument(
            SaksopplysningKildesystem.TPS, xmlWriter.toString());
        saksopplysning.setType(SaksopplysningType.PERSHIST);
        saksopplysning.setVersjon(PERSONHISTORIKK_VERSJON);

        dokumentFactory.lagDokument(saksopplysning);

        return saksopplysning;
    }


    @Override
    public String hentSammensattNavn(String fnr) {
        Saksopplysning tpsOpplysning = hentPerson(fnr, Informasjonsbehov.INGEN);
        PersonDokument personDokument = (PersonDokument) tpsOpplysning.getDokument();
        return personDokument != null ? personDokument.getSammensattNavn() : null;
    }

    @Override
    public boolean harStrengtFortroligAdresse(String fnr) {
        Saksopplysning saksopplysning = hentPerson(fnr, Informasjonsbehov.INGEN);
        PersonDokument personDokument = (PersonDokument) saksopplysning.getDokument();
        return personDokument.getDiskresjonskode() != null && personDokument.getDiskresjonskode().erKode6();
    }

    private HentPersonResponse hentPersonResponseFraTPS(HentPersonRequest request) {
        try {
            return personConsumer.hentPerson(request);
        } catch (HentPersonSikkerhetsbegrensning hentPersonSikkerhetsbegrensning) {
            throw new SikkerhetsbegrensningException(hentPersonSikkerhetsbegrensning);
        } catch (HentPersonPersonIkkeFunnet hentPersonPersonIkkeFunnet) {
            throw new IkkeFunnetException(hentPersonPersonIkkeFunnet);
        }
    }

    private HentPersonhistorikkResponse hentPersonhistorikkResponseFraTPS(HentPersonhistorikkRequest request) {
        try {
            return personConsumer.hentPersonhistorikk(request);
        } catch (HentPersonhistorikkSikkerhetsbegrensning hentPersonhistorikkSikkerhetsbegrensning) {
            throw new SikkerhetsbegrensningException(hentPersonhistorikkSikkerhetsbegrensning);
        } catch (HentPersonhistorikkPersonIkkeFunnet hentPersonhistorikkPersonIkkeFunnet) {
            throw new IkkeFunnetException(hentPersonhistorikkPersonIkkeFunnet);
        }
    }

    private Periode hentAltForegaaendeHistorikkFraTPS(LocalDate dato) throws IntegrasjonException {
        try {
            Periode periode = new Periode();
            XMLGregorianCalendar xmlDato = KonverteringsUtils.localDateToXMLGregorianCalendar(dato);
            periode.setFom(xmlDato); // TPS gir all foregående historikk når fom == tom
            periode.setTom(xmlDato);
            return periode;
        } catch (DatatypeConfigurationException e) {
            throw new IntegrasjonException(e);
        }
    }

    private Set<no.nav.tjeneste.virksomhet.person.v3.informasjon.Informasjonsbehov> mapInformasjonsbehovTilTps(Informasjonsbehov behov) {
        return switch (behov) {
            case STANDARD -> Set.of(no.nav.tjeneste.virksomhet.person.v3.informasjon.Informasjonsbehov.ADRESSE);
            case MED_FAMILIERELASJONER -> Set.of(
                no.nav.tjeneste.virksomhet.person.v3.informasjon.Informasjonsbehov.ADRESSE,
                no.nav.tjeneste.virksomhet.person.v3.informasjon.Informasjonsbehov.FAMILIERELASJONER);
            default -> Collections.emptySet();
        };
    }
}
