package no.nav.melosys.integrasjon.medl;

import java.io.StringWriter;
import java.time.LocalDate;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;

import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.SaksopplysningKilde;
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.domain.dokument.DokumentFactory;
import no.nav.melosys.domain.dokument.medlemskap.Medlemsperiode;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.integrasjon.KonverteringsUtils;
import no.nav.melosys.integrasjon.medl.behandle.BehandleMedlemskapConsumer;
import no.nav.melosys.integrasjon.medl.medlemskap.HentPeriodeListeResponseWrapper;
import no.nav.melosys.integrasjon.medl.medlemskap.MedlemskapConsumer;
import no.nav.melosys.integrasjon.medl.medlemskap.MedlemskapConsumerConfig;
import no.nav.tjeneste.virksomhet.behandlemedlemskap.v2.UgyldigInput;
import no.nav.tjeneste.virksomhet.behandlemedlemskap.v2.meldinger.OpprettPeriodeRequest;
import no.nav.tjeneste.virksomhet.behandlemedlemskap.v2.meldinger.OpprettPeriodeResponse;
import no.nav.tjeneste.virksomhet.medlemskap.v2.PersonIkkeFunnet;
import no.nav.tjeneste.virksomhet.medlemskap.v2.Sikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.medlemskap.v2.informasjon.Foedselsnummer;
import no.nav.tjeneste.virksomhet.medlemskap.v2.meldinger.HentPeriodeListeRequest;
import no.nav.tjeneste.virksomhet.medlemskap.v2.meldinger.HentPeriodeListeResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MedlService implements MedlFasade {

    private static final Logger log = LoggerFactory.getLogger(MedlService.class);

    private static final String MEDLEMSKAP_VERSJON = "2.0";

    private final MedlemskapConsumer medlemskapConsumer;

    private final BehandleMedlemskapConsumer behandleMedlemskapConsumer;

    private DokumentFactory dokumentFactory;

    private final JAXBContext jaxbContext;

    @Autowired
    public MedlService(MedlemskapConsumer medlemskapConsumer,
                       BehandleMedlemskapConsumer behandleMedlemskapConsumer,
                       DokumentFactory dokumentFactory) {
        this.medlemskapConsumer = medlemskapConsumer;
        this.behandleMedlemskapConsumer = behandleMedlemskapConsumer;
        this.dokumentFactory = dokumentFactory;

        try {
            jaxbContext = JAXBContext.newInstance(HentPeriodeListeResponseWrapper.class);
        } catch (JAXBException e) {
            log.error("", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public Saksopplysning hentPeriodeListe(String fnr, LocalDate fom, LocalDate tom) throws IntegrasjonException, SikkerhetsbegrensningException, IkkeFunnetException {
        HentPeriodeListeResponse response = hentPeriodeListeResponse(fnr, fom, tom);

        // Response -> xml
        StringWriter xmlWriter = new StringWriter();
        try {
            HentPeriodeListeResponseWrapper wrapper
                = new HentPeriodeListeResponseWrapper().withPeriodeListe(response.getPeriodeListe());
            JAXBElement<HentPeriodeListeResponseWrapper> xmlRoot
                = new JAXBElement<>(MedlemskapConsumerConfig.getResponse(), HentPeriodeListeResponseWrapper.class, wrapper);

            jaxbContext.createMarshaller().marshal(xmlRoot, xmlWriter);
        } catch (JAXBException e) {
            log.error("", e);
            throw new IntegrasjonException(e);
        }

        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setDokumentXml(xmlWriter.toString());
        saksopplysning.setKilde(SaksopplysningKilde.MEDL);
        saksopplysning.setType(SaksopplysningType.MEDLEMSKAP);
        saksopplysning.setVersjon(MEDLEMSKAP_VERSJON);

        // xml -> java objekter
        dokumentFactory.lagDokument(saksopplysning);

        return saksopplysning;
    }

    @Override
    public Long opprettPeriode(String fnr, Medlemsperiode medlemsperiode) throws IntegrasjonException, SikkerhetsbegrensningException, IkkeFunnetException {
        OpprettPeriodeRequest request = new OpprettPeriodeRequest();

        no.nav.tjeneste.virksomhet.behandlemedlemskap.v2.informasjon.Foedselsnummer ident = new no.nav.tjeneste.virksomhet.behandlemedlemskap.v2.informasjon.Foedselsnummer();
        ident.setValue(fnr);

        no.nav.tjeneste.virksomhet.behandlemedlemskap.v2.informasjon.Medlemsperiode periode = new no.nav.tjeneste.virksomhet.behandlemedlemskap.v2.informasjon.Medlemsperiode();
        try {
            periode.setFraOgMed(KonverteringsUtils.localDateToXMLGregorianCalendar(medlemsperiode.getPeriode().getFom()));
            periode.setTilOgMed(KonverteringsUtils.localDateToXMLGregorianCalendar(medlemsperiode.getPeriode().getTom()));
        } catch (DatatypeConfigurationException e) {
            e.printStackTrace();
        }
        // FIXME sett kodeverdier på medlemsperiode

        request.setIdent(ident);
        request.setPeriode(periode);

        try {
            OpprettPeriodeResponse response = behandleMedlemskapConsumer.opprettPeriode(request);
            return response.getPeriodeId();
        } catch (no.nav.tjeneste.virksomhet.behandlemedlemskap.v2.PersonIkkeFunnet e) {
            throw new IkkeFunnetException(e);
        } catch (no.nav.tjeneste.virksomhet.behandlemedlemskap.v2.Sikkerhetsbegrensning e) {
            throw new SikkerhetsbegrensningException(e);
        } catch (UgyldigInput e) {
            throw new IntegrasjonException(e);
        }
    }

    private HentPeriodeListeResponse hentPeriodeListeResponse(String fnr, LocalDate fom, LocalDate tom) throws SikkerhetsbegrensningException, IkkeFunnetException {
        Foedselsnummer ident = new Foedselsnummer();
        ident.setValue(fnr);

        HentPeriodeListeRequest req = new HentPeriodeListeRequest();
        req.setIdent(ident);

        req.setInkluderPerioderFraOgMed(KonverteringsUtils.javaLocalDateToJodaLocalDate(fom));
        req.setInkluderPerioderTilOgMed(KonverteringsUtils.javaLocalDateToJodaLocalDate(tom));

        try {
            return medlemskapConsumer.hentPeriodeListe(req);
        } catch (Sikkerhetsbegrensning sikkerhetsbegrensning) {
            throw new SikkerhetsbegrensningException(sikkerhetsbegrensning);
        } catch (PersonIkkeFunnet personIkkeFunnet) {
            throw new IkkeFunnetException(personIkkeFunnet);
        }
    }
}
