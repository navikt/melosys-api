package no.nav.melosys.integrasjon.aareg;

import java.io.StringWriter;
import java.time.LocalDate;
import java.time.LocalTime;
import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;

import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.SaksopplysningKildesystem;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AaregService implements AaregFasade {
    private static final String ARBEIDSFORHOLD_VERSJON = "3.0";
    private static final String REGELVERK_A_ORDNINGEN = "A_ORDNINGEN";

    private final ArbeidsforholdConsumer arbeidsforholdConsumer;
    private final DokumentFactory dokumentFactory;

    @Autowired
    AaregService(ArbeidsforholdConsumer arbeidsforholdConsumer, DokumentFactory dokumentFactory) {
        this.arbeidsforholdConsumer = arbeidsforholdConsumer;
        this.dokumentFactory = dokumentFactory;
    }

    @Override
    public Saksopplysning finnArbeidsforholdPrArbeidstaker(String ident, LocalDate fom, LocalDate tom) throws TekniskException, SikkerhetsbegrensningException {
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

        regelverker.setValue(REGELVERK_A_ORDNINGEN);
        request.setRapportertSomRegelverk(regelverker);
        request.setArbeidsforholdIPeriode(periode);

        return finnArbeidsforholdPrArbeidstaker(request);
    }

    private Saksopplysning finnArbeidsforholdPrArbeidstaker(FinnArbeidsforholdPrArbeidstakerRequest request) throws SikkerhetsbegrensningException, IntegrasjonException {
        // Kall til Aa-registret
        FinnArbeidsforholdPrArbeidstakerResponse response;
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
            dokumentFactory.createMarshaller().marshal(xmlRoot, xmlWriter);
        } catch (JAXBException e) {
            throw new IllegalStateException(e);
        }

        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.leggTilKildesystemOgMottattDokument(
            SaksopplysningKildesystem.AAREG, xmlWriter.toString());
        saksopplysning.setType(SaksopplysningType.ARBFORH);
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
        HentArbeidsforholdHistorikkResponse response;
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
            dokumentFactory.createMarshaller().marshal(xmlRoot, xmlWriter);
        } catch (JAXBException e) {
            throw new IntegrasjonException("Uventet IntegrasjonException under oppslag av arbeidsforhold", e);
        }

        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.leggTilKildesystemOgMottattDokument(
            SaksopplysningKildesystem.AAREG, xmlWriter.toString());
        saksopplysning.setType(SaksopplysningType.ARBFORH);
        saksopplysning.setVersjon(ARBEIDSFORHOLD_VERSJON);

        // xml -> java objekter
        dokumentFactory.lagDokument(saksopplysning);

        return saksopplysning;
    }
}
