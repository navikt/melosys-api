package no.nav.melosys.integrasjon.medl;

import java.io.StringWriter;
import java.time.LocalDate;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;

import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.SaksopplysningKilde;
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.domain.dokument.DokumentFactory;
import no.nav.melosys.exception.*;
import no.nav.melosys.integrasjon.KonverteringsUtils;
import no.nav.melosys.integrasjon.medl.behandle.BehandleMedlemskapConsumer;
import no.nav.melosys.integrasjon.medl.medlemskap.HentPeriodeListeResponseWrapper;
import no.nav.melosys.integrasjon.medl.medlemskap.MedlemskapConsumer;
import no.nav.melosys.integrasjon.medl.medlemskap.MedlemskapConsumerConfig;
import no.nav.tjeneste.virksomhet.behandlemedlemskap.v2.PeriodeIkkeFunnet;
import no.nav.tjeneste.virksomhet.behandlemedlemskap.v2.PeriodeUtdatert;
import no.nav.tjeneste.virksomhet.behandlemedlemskap.v2.UgyldigInput;
import no.nav.tjeneste.virksomhet.behandlemedlemskap.v2.meldinger.OppdaterPeriodeRequest;
import no.nav.tjeneste.virksomhet.behandlemedlemskap.v2.meldinger.OpprettPeriodeRequest;
import no.nav.tjeneste.virksomhet.behandlemedlemskap.v2.meldinger.OpprettPeriodeResponse;
import no.nav.tjeneste.virksomhet.medlemskap.v2.PersonIkkeFunnet;
import no.nav.tjeneste.virksomhet.medlemskap.v2.Sikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.medlemskap.v2.informasjon.Foedselsnummer;
import no.nav.tjeneste.virksomhet.medlemskap.v2.informasjon.Medlemsperiode;
import no.nav.tjeneste.virksomhet.medlemskap.v2.meldinger.HentPeriodeListeRequest;
import no.nav.tjeneste.virksomhet.medlemskap.v2.meldinger.HentPeriodeListeResponse;
import no.nav.tjeneste.virksomhet.medlemskap.v2.meldinger.HentPeriodeRequest;
import no.nav.tjeneste.virksomhet.medlemskap.v2.meldinger.HentPeriodeResponse;
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
    public Long opprettPeriodeEndelig(String fnr, Lovvalgsperiode lovvalgsperiode) throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException {
        return opprettPeriode(fnr, lovvalgsperiode, PeriodestatusMedl.GYLD, LovvalgMedl.ENDL) ;
    }

    @Override
    public Long opprettPeriodeUnderAvklaring(String fnr, Lovvalgsperiode lovvalgsperiode) throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException {
        return opprettPeriode(fnr, lovvalgsperiode, PeriodestatusMedl.UAVK, LovvalgMedl.UAVK) ;
    }

    public Long opprettPeriode(String fnr, Lovvalgsperiode lovvalgsperiode, PeriodestatusMedl periodestatusMedl, LovvalgMedl lovvalgMedl) throws TekniskException, SikkerhetsbegrensningException, IkkeFunnetException {
        try {
            OpprettPeriodeRequest request = MedlPeriodeKonverter.konverterTilOpprettPeriodRequest(fnr, lovvalgsperiode, periodestatusMedl, lovvalgMedl);
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

    @Override
    public void oppdaterPeriodeEndelig(Lovvalgsperiode lovvalgsperiode) throws FunksjonellException, TekniskException {
        oppdaterPeriode(lovvalgsperiode);
    }

    public void oppdaterPeriode(Lovvalgsperiode lovvalgsperiode) throws TekniskException, FunksjonellException {
        try {
            HentPeriodeResponse hentPeriodeResponse = medlemskapConsumer.hentPeriode(lagHentPeriodeRequest(lovvalgsperiode.getMedlPeriodeID()));
            Medlemsperiode periode = hentPeriodeResponse.getPeriode();
            OppdaterPeriodeRequest request = MedlPeriodeKonverter.konverterTilOppdaterPeriodeRequest(lovvalgsperiode, PeriodestatusMedl.GYLD, LovvalgMedl.ENDL, periode.getVersjon());
            behandleMedlemskapConsumer.oppdaterPeriode(request);
        } catch (no.nav.tjeneste.virksomhet.behandlemedlemskap.v2.Sikkerhetsbegrensning | Sikkerhetsbegrensning e) {
            throw new SikkerhetsbegrensningException(e);
        } catch (UgyldigInput e) {
            throw new IntegrasjonException(e);
        } catch (PeriodeUtdatert e) {
            throw new FunksjonellException(e);
        } catch (PeriodeIkkeFunnet e) {
            throw new IkkeFunnetException(e);
        }

    }

    private HentPeriodeRequest lagHentPeriodeRequest(long medlPeriodeID) {
        HentPeriodeRequest hentPeriodeRequest = new HentPeriodeRequest();
        hentPeriodeRequest.setPeriodeId(medlPeriodeID);
        return hentPeriodeRequest;
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
