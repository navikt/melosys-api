package no.nav.melosys.integrasjon.medl;

import javax.xml.datatype.DatatypeConfigurationException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import net.minidev.json.JSONArray;
import no.nav.melosys.domain.*;
import no.nav.melosys.domain.dokument.medlemskap.MedlemskapDokument;
import no.nav.melosys.domain.dokument.medlemskap.Medlemsperiode;
import no.nav.melosys.domain.dokument.medlemskap.Periode;
import no.nav.melosys.exception.*;
import no.nav.melosys.integrasjon.KonverteringsUtils;
import no.nav.melosys.integrasjon.medl.behandle.BehandleMedlemskapConsumer;
import no.nav.melosys.integrasjon.medl.medlemskap.MedlemskapConsumer;
import no.nav.tjeneste.virksomhet.behandlemedlemskap.v2.PeriodeIkkeFunnet;
import no.nav.tjeneste.virksomhet.behandlemedlemskap.v2.PeriodeUtdatert;
import no.nav.tjeneste.virksomhet.behandlemedlemskap.v2.UgyldigInput;
import no.nav.tjeneste.virksomhet.behandlemedlemskap.v2.meldinger.OppdaterPeriodeRequest;
import no.nav.tjeneste.virksomhet.behandlemedlemskap.v2.meldinger.OpprettPeriodeRequest;
import no.nav.tjeneste.virksomhet.behandlemedlemskap.v2.meldinger.OpprettPeriodeResponse;
import no.nav.tjeneste.virksomhet.medlemskap.v2.PersonIkkeFunnet;
import no.nav.tjeneste.virksomhet.medlemskap.v2.Sikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.medlemskap.v2.informasjon.Foedselsnummer;
import no.nav.tjeneste.virksomhet.medlemskap.v2.meldinger.HentPeriodeListeRequest;
import no.nav.tjeneste.virksomhet.medlemskap.v2.meldinger.HentPeriodeListeResponse;
import no.nav.tjeneste.virksomhet.medlemskap.v2.meldinger.HentPeriodeRequest;
import no.nav.tjeneste.virksomhet.medlemskap.v2.meldinger.HentPeriodeResponse;
import no.nav.tjenester.medlemskapsunntak.api.v1.MedlemskapsunntakForGet;
import no.nav.tjenester.medlemskapsunntak.api.v1.Sporingsinformasjon;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MedlRestService implements MedlFasade {
    private static final String MEDLEMSKAP_VERSJON = "2.0";

    private final MedlemskapConsumer medlemskapConsumer;
    private final BehandleMedlemskapConsumer behandleMedlemskapConsumer;
    private final MedlemskapRestConsumer medlemskapRestConsumer;
    private final ObjectMapper objectMapper;

    @Autowired
    public MedlRestService(MedlemskapConsumer medlemskapConsumer,
                           BehandleMedlemskapConsumer behandleMedlemskapConsumer,
                           MedlemskapRestConsumer medlemskapRestConsumer,
                           ObjectMapper objectMapper) {
        this.medlemskapConsumer = medlemskapConsumer;
        this.behandleMedlemskapConsumer = behandleMedlemskapConsumer;
        this.medlemskapRestConsumer = medlemskapRestConsumer;
        this.objectMapper = objectMapper;

        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    public Saksopplysning hentPeriodeListe(String fnr, LocalDate fom, LocalDate tom) throws TekniskException {
        List<MedlemskapsunntakForGet> medlemskapsPerioder = medlemskapRestConsumer.hentPeriodeListe(fnr, fom, tom);

        MedlemskapDokument medlemskapDokument = new MedlemskapDokument();
        List<Medlemsperiode> medlemsperioder = new ArrayList<>();

        for (MedlemskapsunntakForGet m : medlemskapsPerioder) {
            Medlemsperiode medlemsperiode = new Medlemsperiode();
            medlemsperiode.id = m.getUnntakId();
            medlemsperiode.periode = new Periode(m.getFraOgMed(), m.getTilOgMed());
            medlemsperiode.type = m.getMedlem() ? "PMMEDSKP" : "PUMEDSKP"; //TODO Sjekke at dette blir rett
            medlemsperiode.status = m.getStatus();
            medlemsperiode.grunnlagstype = m.getGrunnlag();
            medlemsperiode.land = m.getLovvalgsland();
            medlemsperiode.lovvalg = m.getLovvalg();
            medlemsperiode.trygdedekning = m.getDekning();
            Sporingsinformasjon sporingsinformasjon = m.getSporingsinformasjon();
            medlemsperiode.kildedokumenttype = sporingsinformasjon.getKildedokument();
            medlemsperiode.kilde = sporingsinformasjon.getKilde();

            medlemsperioder.add(medlemsperiode);
        }

        medlemskapDokument.medlemsperiode = medlemsperioder;

        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setType(SaksopplysningType.MEDL);
        saksopplysning.setVersjon(MEDLEMSKAP_VERSJON);
        saksopplysning.setDokument(medlemskapDokument);

        try {
            saksopplysning.leggTilKildesystemOgMottattDokument(SaksopplysningKildesystem.MEDL, objectMapper.writeValueAsString(medlemskapsPerioder));
        } catch (JsonProcessingException e) {
            throw new TekniskException("Kunne ikke lagre kildedokument fra MEDL");
        }

        return saksopplysning;
    }

    @Override
    public Long opprettPeriodeEndelig(String fnr, Lovvalgsperiode lovvalgsperiode, KildedokumenttypeMedl kildedokumenttypeMedl) throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException {
        return opprettPeriode(fnr, lovvalgsperiode, PeriodestatusMedl.GYLD, LovvalgMedl.ENDL, kildedokumenttypeMedl);
    }

    @Override
    public Long opprettPeriodeUnderAvklaring(String fnr, PeriodeOmLovvalg periodeOmLovvalg, KildedokumenttypeMedl kildedokumenttypeMedl) throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException {
        return opprettPeriode(fnr, periodeOmLovvalg, PeriodestatusMedl.UAVK, LovvalgMedl.UAVK, kildedokumenttypeMedl);
    }

    @Override
    public Long opprettPeriodeForeløpig(String fnr, PeriodeOmLovvalg periodeOmLovvalg, KildedokumenttypeMedl kildedokumenttypeMedl) throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException {
        return opprettPeriode(fnr, periodeOmLovvalg, PeriodestatusMedl.UAVK, LovvalgMedl.FORL, kildedokumenttypeMedl);
    }

    Long opprettPeriode(String fnr, PeriodeOmLovvalg periodeOmLovvalg, PeriodestatusMedl periodestatusMedl, LovvalgMedl lovvalgMedl, KildedokumenttypeMedl kildedokumenttypeMedl) throws TekniskException, SikkerhetsbegrensningException, IkkeFunnetException {
        try {
            OpprettPeriodeRequest request = MedlPeriodeKonverter.konverterTilOpprettPeriodRequest(fnr, periodeOmLovvalg, periodestatusMedl, lovvalgMedl, kildedokumenttypeMedl);
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
    public void oppdaterPeriodeEndelig(Lovvalgsperiode lovvalgsperiode, KildedokumenttypeMedl kildedokumenttypeMedl) throws TekniskException, FunksjonellException {
        oppdaterPeriode(lovvalgsperiode, PeriodestatusMedl.GYLD, LovvalgMedl.ENDL, kildedokumenttypeMedl);
    }

    @Override
    public void oppdaterPeriodeForeløpig(Lovvalgsperiode lovvalgsperiode, KildedokumenttypeMedl kildedokumenttypeMedl) throws FunksjonellException, TekniskException {
        oppdaterPeriode(lovvalgsperiode, PeriodestatusMedl.UAVK, LovvalgMedl.FORL, kildedokumenttypeMedl);
    }

    private void oppdaterPeriode(Lovvalgsperiode lovvalgsperiode,
                                 PeriodestatusMedl periodestatusMedl,
                                 LovvalgMedl lovvalgMedl,
                                 KildedokumenttypeMedl kildedokumenttypeMedl) throws FunksjonellException, TekniskException {
        try {
            Long medlPeriodeID = lovvalgsperiode.getMedlPeriodeID();
            if (medlPeriodeID == null) {
                throw new TekniskException("Det er ikke lagret noen medlPeriodeID på lovvalgsperiode som skal oppdateres i MEDL");
            }
            HentPeriodeResponse hentPeriodeResponse = medlemskapConsumer.hentPeriode(lagHentPeriodeRequest(medlPeriodeID));
            no.nav.tjeneste.virksomhet.medlemskap.v2.informasjon.Medlemsperiode periode = Optional.ofNullable(hentPeriodeResponse.getPeriode())
                .orElseThrow(() -> new TekniskException("Fant ingen eksisterende medlPeriode med id " + medlPeriodeID));
            OppdaterPeriodeRequest request = MedlPeriodeKonverter.konverterTilOppdaterPeriodeRequest(lovvalgsperiode, periodestatusMedl, lovvalgMedl, kildedokumenttypeMedl, periode.getVersjon());
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

    @Override
    public void avvisPeriode(Long medlPeriodeID, StatusaarsakMedl årsak) throws SikkerhetsbegrensningException, IkkeFunnetException {
        try {
            behandleMedlemskapConsumer.avvisPeriode(
                MedlPeriodeKonverter.konverterTilAvvisPeriodeRequest(medlPeriodeID, årsak)
            );
        } catch (no.nav.tjeneste.virksomhet.behandlemedlemskap.v2.Sikkerhetsbegrensning ex) {
            throw new SikkerhetsbegrensningException(ex);
        } catch (PeriodeIkkeFunnet ex) {
            throw new IkkeFunnetException(ex);
        }
    }

    private HentPeriodeRequest lagHentPeriodeRequest(long medlPeriodeID) {
        HentPeriodeRequest hentPeriodeRequest = new HentPeriodeRequest();
        hentPeriodeRequest.setPeriodeId(medlPeriodeID);
        return hentPeriodeRequest;
    }


    private HentPeriodeListeResponse hentPeriodeListeResponse(String fnr, LocalDate fom, LocalDate tom) throws SikkerhetsbegrensningException, IkkeFunnetException, IntegrasjonException {
        Foedselsnummer ident = new Foedselsnummer();
        ident.setValue(fnr);

        HentPeriodeListeRequest req = new HentPeriodeListeRequest();
        req.setIdent(ident);

        try {
            req.setInkluderPerioderFraOgMed(KonverteringsUtils.localDateToXMLGregorianCalendar(fom));
            req.setInkluderPerioderTilOgMed(KonverteringsUtils.localDateToXMLGregorianCalendar(tom));
            return medlemskapConsumer.hentPeriodeListe(req);
        } catch (DatatypeConfigurationException e) {
            throw new IntegrasjonException(e);
        } catch (Sikkerhetsbegrensning sikkerhetsbegrensning) {
            throw new SikkerhetsbegrensningException(sikkerhetsbegrensning);
        } catch (PersonIkkeFunnet personIkkeFunnet) {
            throw new IkkeFunnetException(personIkkeFunnet);
        }
    }
}
