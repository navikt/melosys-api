package no.nav.melosys.integrasjon.aareg;

import java.io.StringWriter;
import java.time.LocalDate;
import java.time.LocalTime;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.SaksopplysningKilde;
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.domain.dokument.DokumentFactory;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.KonverteringsUtils;
import no.nav.melosys.integrasjon.aareg.arbeidsforhold.ArbeidsforholdConsumer;
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.binding.FinnArbeidsforholdPrArbeidstakerSikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.binding.FinnArbeidsforholdPrArbeidstakerUgyldigInput;
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.binding.HentArbeidsforholdHistorikkArbeidsforholdIkkeFunnet;
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.binding.HentArbeidsforholdHistorikkSikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.informasjon.arbeidsforhold.NorskIdent;
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.informasjon.arbeidsforhold.Periode;
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.informasjon.arbeidsforhold.Regelverker;
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.meldinger.FinnArbeidsforholdPrArbeidstakerRequest;
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.meldinger.FinnArbeidsforholdPrArbeidstakerResponse;
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.meldinger.HentArbeidsforholdHistorikkRequest;
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.meldinger.HentArbeidsforholdHistorikkResponse;

@Service
public class AaregService implements AaregFasade {

    private static final Logger log = LoggerFactory.getLogger(AaregService.class);

    private static final String ARBEIDSFORHOLD_VERSJON = "3.0";

    private ArbeidsforholdConsumer arbeidsforholdConsumer;

    private DokumentFactory dokumentFactory;

    final JAXBContext jaxbContext;

    @Autowired
    public AaregService(ArbeidsforholdConsumer arbeidsforholdConsumer, DokumentFactory dokumentFactory) {
        this(arbeidsforholdConsumer, dokumentFactory, lagJaxbContext());
    }

    private static JAXBContext lagJaxbContext() {
        try {
            return JAXBContext.newInstance(no.nav.tjeneste.virksomhet.arbeidsforhold.v3.FinnArbeidsforholdPrArbeidstakerResponse.class,
                    no.nav.tjeneste.virksomhet.arbeidsforhold.v3.HentArbeidsforholdHistorikkResponse.class);
        } catch (JAXBException e) {
            log.error("", e);
            throw new RuntimeException(e);
        }
    }

    AaregService(ArbeidsforholdConsumer arbeidsforholdConsumer, DokumentFactory dokumentFactory, JAXBContext jaxbContext) {
        this.arbeidsforholdConsumer = arbeidsforholdConsumer;
        this.dokumentFactory = dokumentFactory;
        this.jaxbContext = jaxbContext;
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
                periode.setTom(KonverteringsUtils.localDateTimeToXMLGregorianCalendar(tom.atTime(LocalTime.MAX)));
            }
        } catch (DatatypeConfigurationException e) {
            throw new IntegrasjonException(e);
        }

        regelverker.setValue(regelverk);
        request.setRapportertSomRegelverk(regelverker);
        request.setArbeidsforholdIPeriode(periode);

        return finnArbeidsforholdPrArbeidstaker(request);
    }

    private Saksopplysning finnArbeidsforholdPrArbeidstaker(FinnArbeidsforholdPrArbeidstakerRequest request) throws SikkerhetsbegrensningException, IntegrasjonException {
        // Kall til Aa-registret
        FinnArbeidsforholdPrArbeidstakerResponse response = null;
        try {
            response = arbeidsforholdConsumer.finnArbeidsforholdPrArbeidstaker(request);
        } catch (FinnArbeidsforholdPrArbeidstakerSikkerhetsbegrensning e) {
            throw new SikkerhetsbegrensningException(e);
        } catch (FinnArbeidsforholdPrArbeidstakerUgyldigInput e) {
            throw new IntegrasjonException(e);
        }

        // Response -> xml
        StringWriter xmlWriter = new StringWriter();
        try {
            no.nav.tjeneste.virksomhet.arbeidsforhold.v3.FinnArbeidsforholdPrArbeidstakerResponse xmlRoot = new no.nav.tjeneste.virksomhet.arbeidsforhold.v3.FinnArbeidsforholdPrArbeidstakerResponse();
            xmlRoot.setParameters(response);
            jaxbContext.createMarshaller().marshal(xmlRoot, xmlWriter);
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
    public Saksopplysning hentArbeidsforholdHistorikk(Long arbeidsforholdsID) throws IntegrasjonException, SikkerhetsbegrensningException, IkkeFunnetException {
        HentArbeidsforholdHistorikkRequest request = new HentArbeidsforholdHistorikkRequest();
        request.setArbeidsforholdId(arbeidsforholdsID);

        // Kall til Aa-registret
        HentArbeidsforholdHistorikkResponse response = null;
        try {
            response = arbeidsforholdConsumer.hentArbeidsforholdHistorikk(request);
        } catch (HentArbeidsforholdHistorikkSikkerhetsbegrensning e) {
            throw new SikkerhetsbegrensningException("SikkerhetsbegrensningException under oppslag av arbeidsforhold", e);
        } catch (HentArbeidsforholdHistorikkArbeidsforholdIkkeFunnet e) {
            throw new IkkeFunnetException(e);
        }

        // Response -> xml
        StringWriter xmlWriter = new StringWriter();
        try {
            no.nav.tjeneste.virksomhet.arbeidsforhold.v3.HentArbeidsforholdHistorikkResponse xmlRoot = new no.nav.tjeneste.virksomhet.arbeidsforhold.v3.HentArbeidsforholdHistorikkResponse();
            xmlRoot.setParameters(response);
            jaxbContext.createMarshaller().marshal(xmlRoot, xmlWriter);
        } catch (JAXBException e) {
            throw new IntegrasjonException("Uventet IntegrasjonException under oppslag av arbeidsforhold", e);
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
