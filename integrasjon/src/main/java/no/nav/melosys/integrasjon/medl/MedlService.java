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
import no.nav.melosys.domain.kodeverk.LovvalgBestemmelse;
import no.nav.melosys.domain.util.LandkoderUtils;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.ekstern.tjenester.medlemskapsunntak.api.v1.MedlemskapsunntakForGet;
import no.nav.melosys.ekstern.tjenester.medlemskapsunntak.api.v1.MedlemskapsunntakForPost;
import no.nav.melosys.ekstern.tjenester.medlemskapsunntak.api.v1.MedlemskapsunntakForPut;
import no.nav.melosys.ekstern.tjenester.medlemskapsunntak.api.v1.Sporingsinformasjon;
import org.springframework.stereotype.Service;

import static no.nav.melosys.integrasjon.medl.MedlPeriodeKonverter.*;

@Service
public class MedlService {
    private static final String MEDLEMSKAP_VERSJON = "2.0";

    private final MedlemskapRestConsumer medlemskapRestConsumer;
    private final ObjectMapper objectMapper;

    public MedlService(MedlemskapRestConsumer medlemskapRestConsumer, ObjectMapper objectMapper) {
        this.medlemskapRestConsumer = medlemskapRestConsumer;
        this.objectMapper = objectMapper;
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    public Saksopplysning hentPeriodeListe(String fnr, LocalDate fom, LocalDate tom) {
        List<MedlemskapsunntakForGet> periodeListeResponse = medlemskapRestConsumer.hentPeriodeListe(fnr, fom, tom);

        MedlemskapDokument medlemskapDokument = new MedlemskapDokument();
        List<Medlemsperiode> medlemsperioder = new ArrayList<>();

        for (MedlemskapsunntakForGet m : periodeListeResponse) {
            Medlemsperiode medlemsperiode = new Medlemsperiode();
            medlemsperiode.id = m.getUnntakId();
            medlemsperiode.periode = new Periode(m.getFraOgMed(), m.getTilOgMed());
            medlemsperiode.type = m.getMedlem() ? "PMMEDSKP" : "PUMEDSKP";
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

    public Long opprettPeriodeEndelig(String fnr, HarBestemmelse<?> bestemmelse, KildedokumenttypeMedl kildedokumenttypeMedl) {
        return opprettPeriode(fnr, bestemmelse, PeriodestatusMedl.GYLD, LovvalgMedl.ENDL, kildedokumenttypeMedl);
    }

    public Long opprettPeriodeUnderAvklaring(String fnr, PeriodeOmLovvalg periodeOmLovvalg, KildedokumenttypeMedl kildedokumenttypeMedl) {
        return opprettPeriode(fnr, periodeOmLovvalg, PeriodestatusMedl.UAVK, LovvalgMedl.UAVK, kildedokumenttypeMedl);
    }

    public Long opprettPeriodeForeløpig(String fnr, PeriodeOmLovvalg periodeOmLovvalg, KildedokumenttypeMedl kildedokumenttypeMedl) {
        return opprettPeriode(fnr, periodeOmLovvalg, PeriodestatusMedl.UAVK, LovvalgMedl.FORL, kildedokumenttypeMedl);
    }

    public void oppdaterPeriodeEndelig(Lovvalgsperiode lovvalgsperiode, KildedokumenttypeMedl kildedokumenttypeMedl) {
        oppdaterPeriode(lovvalgsperiode, PeriodestatusMedl.GYLD, LovvalgMedl.ENDL, kildedokumenttypeMedl);
    }

    public void oppdaterPeriodeForeløpig(Lovvalgsperiode lovvalgsperiode, KildedokumenttypeMedl kildedokumenttypeMedl) {
        oppdaterPeriode(lovvalgsperiode, PeriodestatusMedl.UAVK, LovvalgMedl.FORL, kildedokumenttypeMedl);
    }

    public void avvisPeriode(Long medlPeriodeID, StatusaarsakMedl årsak) {
        MedlemskapsunntakForGet eksisterendePeriode = hentEksisterendePeriode(medlPeriodeID);

        MedlemskapsunntakForPut.SporingsinformasjonForPut sporingsinformasjon = MedlemskapsunntakForPut.SporingsinformasjonForPut.builder()
            .kildedokument(eksisterendePeriode.getSporingsinformasjon().getKildedokument())
            .versjon(eksisterendePeriode.getSporingsinformasjon().getVersjon())
            .build();

        MedlemskapsunntakForPut request = MedlemskapsunntakForPut.builder()
            .unntakId(eksisterendePeriode.getUnntakId())
            .fraOgMed(eksisterendePeriode.getFraOgMed())
            .tilOgMed(eksisterendePeriode.getTilOgMed())
            .status(PeriodestatusMedl.AVST.getKode())
            .statusaarsak(årsak.getKode())
            .dekning(eksisterendePeriode.getDekning())
            .lovvalgsland(eksisterendePeriode.getLovvalgsland())
            .lovvalg(LovvalgMedl.ENDL.getKode())
            .grunnlag(eksisterendePeriode.getGrunnlag())
            .sporingsinformasjon(sporingsinformasjon)
            .build();

        medlemskapRestConsumer.oppdaterPeriode(request);
    }

    private Long opprettPeriode(String fnr, HarBestemmelse<?> bestemmelse, PeriodestatusMedl periodestatusMedl,
                                LovvalgMedl lovvalgMedl, KildedokumenttypeMedl kildedokumenttypeMedl) {

        MedlemskapsunntakForPost.MedlemskapsunntakForPostBuilder request = null;

        if (bestemmelse instanceof PeriodeOmLovvalg periodeOmLovvalg) {
            request = lovvalgRequest(periodeOmLovvalg);
        } else if (bestemmelse instanceof Medlemskapsperiode medlemskapsperiode) {
            request = medlemskapsperiodeRequest(medlemskapsperiode);
        }

        if (request == null) {
            throw new TekniskException("Oppretting av periode i MEDL feilet");
        }

        MedlemskapsunntakForPost.SporingsinformasjonForPost sporingsinformasjon = MedlemskapsunntakForPost.SporingsinformasjonForPost.builder()
            .kildedokument(kildedokumenttypeMedl.getKode())
            .build();

        request
            .sporingsinformasjon(sporingsinformasjon)
            .ident(fnr)
            .status(periodestatusMedl.getKode())
            .lovvalg(lovvalgMedl.getKode());

        return medlemskapRestConsumer.opprettPeriode(request.build()).getUnntakId();
    }

    private MedlemskapsunntakForPost.MedlemskapsunntakForPostBuilder lovvalgRequest(PeriodeOmLovvalg periodeOmLovvalg) {
        return MedlemskapsunntakForPost.builder()
            .fraOgMed(periodeOmLovvalg.getFom())
            .tilOgMed(periodeOmLovvalg.getTom())
            .dekning(tilMedlTrygdeDekningEos(periodeOmLovvalg.getDekning()).getKode())
            .lovvalgsland(LandkoderUtils.tilIso3(periodeOmLovvalg.getLovvalgsland().getKode()))
            .grunnlag(tilGrunnlagMedltype(hentLovvalgBestemmelse(periodeOmLovvalg)).getKode());
    }

    private MedlemskapsunntakForPost.MedlemskapsunntakForPostBuilder medlemskapsperiodeRequest(Medlemskapsperiode medlemskapsperiode) {
        return MedlemskapsunntakForPost.builder()
            .fraOgMed(medlemskapsperiode.getFom())
            .tilOgMed(medlemskapsperiode.getTom())
            .dekning(tilMedlTrygdeDekningFtrl(medlemskapsperiode.getDekning(), medlemskapsperiode.getBestemmelse()).getKode())
            .lovvalgsland(LandkoderUtils.tilIso3(medlemskapsperiode.getArbeidsland()))
            .grunnlag(tilGrunnlagMedltype(medlemskapsperiode.getBestemmelse()).getKode());
    }

    private void oppdaterPeriode(Lovvalgsperiode lovvalgsperiode, PeriodestatusMedl periodestatusMedl,
                                 LovvalgMedl lovvalgMedl, KildedokumenttypeMedl kildedokumenttypeMedl) {

        Long medlPeriodeID = lovvalgsperiode.getMedlPeriodeID();
        if (medlPeriodeID == null) {
            throw new TekniskException("Det er ikke lagret noen medlPeriodeID på lovvalgsperiode som skal oppdateres i MEDL");
        }

        MedlemskapsunntakForGet eksisterendePeriode = hentEksisterendePeriode(medlPeriodeID);

        MedlemskapsunntakForPut.SporingsinformasjonForPut sporingsinformasjon = MedlemskapsunntakForPut.SporingsinformasjonForPut.builder()
            .kildedokument(kildedokumenttypeMedl.getKode())
            .versjon(eksisterendePeriode.getSporingsinformasjon().getVersjon())
            .build();

        LovvalgBestemmelse bestemmelse = hentLovvalgBestemmelse(lovvalgsperiode);

        MedlemskapsunntakForPut request = MedlemskapsunntakForPut.builder()
            .unntakId(medlPeriodeID)
            .fraOgMed(lovvalgsperiode.getFom())
            .tilOgMed(lovvalgsperiode.getTom())
            .status(periodestatusMedl.getKode())
            .dekning(tilMedlTrygdeDekningEos(lovvalgsperiode.getDekning()).getKode())
            .lovvalgsland(LandkoderUtils.tilIso3(lovvalgsperiode.getLovvalgsland().getKode()))
            .lovvalg(lovvalgMedl.getKode())
            .grunnlag(tilGrunnlagMedltype(bestemmelse).getKode())
            .sporingsinformasjon(sporingsinformasjon)
            .build();

        medlemskapRestConsumer.oppdaterPeriode(request);
    }

    private MedlemskapsunntakForGet hentEksisterendePeriode(Long medlPeriodeID) {
        return medlemskapRestConsumer.hentPeriode(medlPeriodeID.toString());
    }
}
