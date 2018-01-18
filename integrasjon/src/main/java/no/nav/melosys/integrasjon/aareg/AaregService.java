package no.nav.melosys.integrasjon.aareg;

import java.io.StringWriter;
import java.time.LocalDate;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.datatype.DatatypeConfigurationException;

import no.nav.melosys.integrasjon.KonverteringsUtils;
import no.nav.melosys.integrasjon.felles.exception.TekniskException;
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.informasjon.arbeidsforhold.Periode;
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.binding.HentArbeidsforholdHistorikkArbeidsforholdIkkeFunnet;
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.binding.HentArbeidsforholdHistorikkSikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.meldinger.HentArbeidsforholdHistorikkRequest;
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.meldinger.HentArbeidsforholdHistorikkResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.SaksopplysningKilde;
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.domain.dokument.DokumentFactory;
import no.nav.melosys.integrasjon.aareg.arbeidsforhold.ArbeidsforholdConsumer;
import no.nav.melosys.integrasjon.felles.exception.IntegrasjonException;
import no.nav.melosys.integrasjon.felles.exception.SikkerhetsbegrensningException;
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.binding.FinnArbeidsforholdPrArbeidstakerSikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.binding.FinnArbeidsforholdPrArbeidstakerUgyldigInput;
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.informasjon.arbeidsforhold.NorskIdent;
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.informasjon.arbeidsforhold.Regelverker;
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.meldinger.FinnArbeidsforholdPrArbeidstakerRequest;
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.meldinger.FinnArbeidsforholdPrArbeidstakerResponse;

@Service
public class AaregService implements AaregFasade {

    private static final Logger log = LoggerFactory.getLogger(AaregService.class);

    private static final String ARBEIDSFORHOLD_VERSJON = "3.0";

    private ArbeidsforholdConsumer arbeidsforholdConsumer;

    private DokumentFactory dokumentFactory;

    private final Marshaller marshaller;

    private final Marshaller historikkMarshaller;

    @Autowired
    public AaregService(ArbeidsforholdConsumer arbeidsforholdConsumer, DokumentFactory dokumentFactory) {
        this.arbeidsforholdConsumer = arbeidsforholdConsumer;
        this.dokumentFactory = dokumentFactory;

        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(no.nav.tjeneste.virksomhet.arbeidsforhold.v3.FinnArbeidsforholdPrArbeidstakerResponse.class);
            marshaller = jaxbContext.createMarshaller();
            jaxbContext = JAXBContext.newInstance(no.nav.tjeneste.virksomhet.arbeidsforhold.v3.HentArbeidsforholdHistorikkResponse.class);
            historikkMarshaller = jaxbContext.createMarshaller();
        } catch (JAXBException e) {
            log.error("", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public Saksopplysning finnArbeidsforholdPrArbeidstaker(String ident, String regelverk, LocalDate fom, LocalDate tom) throws IntegrasjonException, TekniskException, SikkerhetsbegrensningException {
        FinnArbeidsforholdPrArbeidstakerRequest request = new FinnArbeidsforholdPrArbeidstakerRequest();

        NorskIdent norskIdent = new NorskIdent();
        norskIdent.setIdent(ident);
        request.setIdent(norskIdent);
        Regelverker regelverker = new Regelverker();

        Periode periode = new Periode();
        try {
            if (fom != null) {
                periode.setFom(KonverteringsUtils.localDateTimeToXMLGregorianCalendar(fom.atStartOfDay()));
            }
            if (tom != null) {
                periode.setTom(KonverteringsUtils.localDateTimeToXMLGregorianCalendar(tom.atStartOfDay()));
            }
        } catch (DatatypeConfigurationException DatatypeConfigurationException) {
            throw new TekniskException(DatatypeConfigurationException);
        }

        regelverker.setValue(regelverk);
        request.setRapportertSomRegelverk(regelverker);
        request.setArbeidsforholdIPeriode(periode);

        return finnArbeidsforholdPrArbeidstaker(request);
    }

    private Saksopplysning finnArbeidsforholdPrArbeidstaker(FinnArbeidsforholdPrArbeidstakerRequest request) throws SikkerhetsbegrensningException {
        // Kall til Aa-registret
        FinnArbeidsforholdPrArbeidstakerResponse response = null;
        try {
            response = arbeidsforholdConsumer.finnArbeidsforholdPrArbeidstaker(request);
        } catch (FinnArbeidsforholdPrArbeidstakerSikkerhetsbegrensning finnArbeidsforholdPrArbeidstakerSikkerhetsbegrensning) {
            throw new SikkerhetsbegrensningException(finnArbeidsforholdPrArbeidstakerSikkerhetsbegrensning);
        } catch (FinnArbeidsforholdPrArbeidstakerUgyldigInput finnArbeidsforholdPrArbeidstakerUgyldigInput) {
            throw new IntegrasjonException(finnArbeidsforholdPrArbeidstakerUgyldigInput);
        }

        // Response -> xml
        StringWriter xmlWriter = new StringWriter();
        try {
            no.nav.tjeneste.virksomhet.arbeidsforhold.v3.FinnArbeidsforholdPrArbeidstakerResponse xmlRoot = new no.nav.tjeneste.virksomhet.arbeidsforhold.v3.FinnArbeidsforholdPrArbeidstakerResponse();
            xmlRoot.setParameters(response);
            marshaller.marshal(xmlRoot, xmlWriter);
        } catch (JAXBException e) {
            log.error("", e);
            throw new RuntimeException(e);
        }

        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setDokumentXml(xmlWriter.toString());
        saksopplysning.setKilde(SaksopplysningKilde.AAREG);
        saksopplysning.setType(SaksopplysningType.ARBEIDSFORHOLD);
        saksopplysning.setVersjon(ARBEIDSFORHOLD_VERSJON);

        // xml -> java objekter
        dokumentFactory.lagDokument(saksopplysning);

        return saksopplysning;
    }

    @Override
    public Saksopplysning hentArbeidsforholdHistorikk(Long arbeidsforholdsID) throws IntegrasjonException, SikkerhetsbegrensningException {
        HentArbeidsforholdHistorikkRequest request = new HentArbeidsforholdHistorikkRequest();
        request.setArbeidsforholdId(arbeidsforholdsID);

        // Kall til Aa-registret
        HentArbeidsforholdHistorikkResponse response = null;
        try {
            response = arbeidsforholdConsumer.hentArbeidsforholdHistorikk(request);
        } catch (HentArbeidsforholdHistorikkSikkerhetsbegrensning hentArbeidsforholdHistorikkSikkerhetsbegrensning) {
            throw new SikkerhetsbegrensningException(hentArbeidsforholdHistorikkSikkerhetsbegrensning);
        } catch (HentArbeidsforholdHistorikkArbeidsforholdIkkeFunnet hentArbeidsforholdHistorikkArbeidsforholdIkkeFunnet) {
            throw new IntegrasjonException(hentArbeidsforholdHistorikkArbeidsforholdIkkeFunnet);
        }

        // Response -> xml
        StringWriter xmlWriter = new StringWriter();
        try {
            no.nav.tjeneste.virksomhet.arbeidsforhold.v3.HentArbeidsforholdHistorikkResponse xmlRoot = new no.nav.tjeneste.virksomhet.arbeidsforhold.v3.HentArbeidsforholdHistorikkResponse();
            xmlRoot.setParameters(response);
            historikkMarshaller.marshal(xmlRoot, xmlWriter);
        } catch (JAXBException e) {
            log.error("", e);
            throw new RuntimeException(e);
        }

        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setDokumentXml(xmlWriter.toString());
        saksopplysning.setKilde(SaksopplysningKilde.AAREG);
        saksopplysning.setType(SaksopplysningType.ARBEIDSFORHOLD);
        saksopplysning.setVersjon(ARBEIDSFORHOLD_VERSJON);

        // xml -> java objekter
        dokumentFactory.lagDokument(saksopplysning);

        return saksopplysning;
    }
}
