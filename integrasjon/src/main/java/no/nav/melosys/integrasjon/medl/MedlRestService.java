package no.nav.melosys.integrasjon.medl;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import no.nav.melosys.domain.*;
import no.nav.melosys.domain.dokument.medlemskap.MedlemskapDokument;
import no.nav.melosys.domain.dokument.medlemskap.Medlemsperiode;
import no.nav.melosys.domain.dokument.medlemskap.Periode;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import no.nav.tjenester.medlemskapsunntak.api.v1.MedlemskapsunntakForGet;
import no.nav.tjenester.medlemskapsunntak.api.v1.Sporingsinformasjon;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MedlRestService implements MedlFasade {
    private static final String MEDLEMSKAP_VERSJON = "2.0";

    private final MedlemskapRestConsumer medlemskapRestConsumer;
    private final ObjectMapper objectMapper;

    @Autowired
    public MedlRestService(MedlemskapRestConsumer medlemskapRestConsumer,
                           ObjectMapper objectMapper) {
        this.medlemskapRestConsumer = medlemskapRestConsumer;
        this.objectMapper = objectMapper;

        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    public Saksopplysning hentPeriodeListe(String fnr, LocalDate fom, LocalDate tom) throws TekniskException {
        List<MedlemskapsunntakForGet> periodeListeResponse = medlemskapRestConsumer.hentPeriodeListe(fnr, fom, tom);

        MedlemskapDokument medlemskapDokument = new MedlemskapDokument();
        List<Medlemsperiode> medlemsperioder = new ArrayList<>();

        for (MedlemskapsunntakForGet m : periodeListeResponse) {
            Medlemsperiode medlemsperiode = new Medlemsperiode();
            medlemsperiode.id = m.getUnntakId();
            medlemsperiode.periode = new Periode(m.getFraOgMed(), m.getTilOgMed());
            medlemsperiode.type = m.getMedlem() ? "PMMEDSKP" : "PUMEDSKP"; //NOTE Sjekke at dette blir rett
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
            saksopplysning.leggTilKildesystemOgMottattDokument(SaksopplysningKildesystem.MEDL, objectMapper.writeValueAsString(periodeListeResponse));
        } catch (JsonProcessingException e) {
            throw new TekniskException("Kunne ikke lagre kildedokument fra MEDL");
        }

        return saksopplysning;
    }

    @Override
    public Long opprettPeriodeEndelig(String fnr, Lovvalgsperiode lovvalgsperiode, KildedokumenttypeMedl kildedokumenttypeMedl) throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException {
        throw new NotImplementedException("Ikke implementert");
    }

    @Override
    public Long opprettPeriodeUnderAvklaring(String fnr, PeriodeOmLovvalg periodeOmLovvalg, KildedokumenttypeMedl kildedokumenttypeMedl) throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException {
        throw new NotImplementedException("Ikke implementert");
    }

    @Override
    public Long opprettPeriodeForeløpig(String fnr, PeriodeOmLovvalg periodeOmLovvalg, KildedokumenttypeMedl kildedokumenttypeMedl) throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException {
        throw new NotImplementedException("Ikke implementert");
    }

    @Override
    public void oppdaterPeriodeEndelig(Lovvalgsperiode lovvalgsperiode, KildedokumenttypeMedl kildedokumenttypeMedl) throws TekniskException, FunksjonellException {
        throw new NotImplementedException("Ikke implementert");
    }

    @Override
    public void oppdaterPeriodeForeløpig(Lovvalgsperiode lovvalgsperiode, KildedokumenttypeMedl kildedokumenttypeMedl) throws FunksjonellException, TekniskException {
        throw new NotImplementedException("Ikke implementert");
    }

    @Override
    public void avvisPeriode(Long medlPeriodeID, StatusaarsakMedl årsak) throws SikkerhetsbegrensningException, IkkeFunnetException {
        throw new NotImplementedException("Ikke implementert");
    }
}
