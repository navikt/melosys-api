package no.nav.melosys.service.medlemskapsperiode;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import no.nav.melosys.domain.Medlemskapsperiode;
import no.nav.melosys.domain.avgift.Inntektsperiode;
import no.nav.melosys.domain.avgift.SkatteforholdTilNorge;
import no.nav.melosys.domain.folketrygden.MedlemAvFolketrygden;
import no.nav.melosys.domain.kodeverk.InnvilgelsesResultat;
import no.nav.melosys.domain.kodeverk.Trygdedekninger;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.repository.MedlemskapsperiodeRepository;
import no.nav.melosys.service.MedlemAvFolketrygdenService;
import no.nav.melosys.service.avgift.TrygdeavgiftsgrunnlagService;
import no.nav.melosys.service.avgift.dto.InntektskildeRequest;
import no.nav.melosys.service.avgift.dto.OppdaterTrygdeavgiftsgrunnlagRequest;
import no.nav.melosys.service.avgift.dto.SkatteforholdTilNorgeRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static no.nav.melosys.domain.kodeverk.Trygdedekninger.*;
import static no.nav.melosys.service.kontroll.regler.PeriodeRegler.feilIPeriode;

@Service
public class MedlemskapsperiodeService {

    private static final Collection<Trygdedekninger> GYLDIGE_TRYGDEDEKNINGER = Set.of(FTRL_2_9_FØRSTE_LEDD_A_HELSE, FTRL_2_9_FØRSTE_LEDD_A_ANDRE_LEDD_HELSE_SYKE_FORELDREPENGER,
        FTRL_2_9_FØRSTE_LEDD_B_PENSJON, FTRL_2_9_FØRSTE_LEDD_C_HELSE_PENSJON, FTRL_2_9_FØRSTE_LEDD_C_ANDRE_LEDD_HELSE_PENSJON_SYKE_FORELDREPENGER);

    private final MedlemskapsperiodeRepository medlemskapsperiodeRepository;
    private final MedlemAvFolketrygdenService medlemAvFolketrygdenService;
    private final TrygdeavgiftsgrunnlagService trygdeavgiftsgrunnlagService;

    public MedlemskapsperiodeService(MedlemskapsperiodeRepository medlemskapsperiodeRepository,
                                     MedlemAvFolketrygdenService medlemAvFolketrygdenService, TrygdeavgiftsgrunnlagService trygdeavgiftsgrunnlagService) {
        this.medlemskapsperiodeRepository = medlemskapsperiodeRepository;
        this.medlemAvFolketrygdenService = medlemAvFolketrygdenService;
        this.trygdeavgiftsgrunnlagService = trygdeavgiftsgrunnlagService;
    }

    @Transactional(readOnly = true)
    public Collection<Medlemskapsperiode> hentMedlemskapsperioder(long behandlingsresultatID) {
        return medlemAvFolketrygdenService.finnMedlemAvFolketrygden(behandlingsresultatID)
            .map(medlemAvFolketrygden -> medlemAvFolketrygden.getMedlemskapsperioder().stream().toList())
            .orElse(Collections.emptyList());
    }

    @Transactional
    public Medlemskapsperiode opprettMedlemskapsperiode(long behandlingsresultatID,
                                                        LocalDate fom,
                                                        LocalDate tom,
                                                        InnvilgelsesResultat innvilgelsesResultat,
                                                        Trygdedekninger trygdedekning) {
        final var medlemAvFolketrygden = medlemAvFolketrygdenService.hentMedlemAvFolketrygden(behandlingsresultatID);
        final var eksisterendeMedlemsperiode = medlemAvFolketrygden
            .getMedlemskapsperioder()
            .stream()
            .findFirst()
            .orElseThrow(() -> new FunksjonellException("Behandling " + behandlingsresultatID + " har ingen medlemskapsperiode"));

        final var nyMedlemskapsperiode = new Medlemskapsperiode();
        oppdaterMedlemskapsperiode(nyMedlemskapsperiode, fom, tom, innvilgelsesResultat, trygdedekning);
        nyMedlemskapsperiode.setArbeidsland(eksisterendeMedlemsperiode.getArbeidsland());
        nyMedlemskapsperiode.setMedlemskapstype(eksisterendeMedlemsperiode.getMedlemskapstype());
        medlemAvFolketrygden.addMedlemskapsperiode(nyMedlemskapsperiode);


        Medlemskapsperiode medlemskapsperiode = medlemskapsperiodeRepository.save(nyMedlemskapsperiode);
        oppdatereTrygdeavgiftsgrunnlag(behandlingsresultatID, medlemAvFolketrygden);
        return medlemskapsperiode;
    }

    @Transactional
    public Medlemskapsperiode oppdaterMedlemskapsperiode(long behandlingsresultatID,
                                                         long medlemskapsperiodeID,
                                                         LocalDate fom,
                                                         LocalDate tom,
                                                         InnvilgelsesResultat innvilgelsesResultat,
                                                         Trygdedekninger trygdedekning) {
        var medlemskapsperiode = medlemAvFolketrygdenService.hentMedlemAvFolketrygden(behandlingsresultatID)
            .getMedlemskapsperioder()
            .stream()
            .filter(m -> m.getId() == medlemskapsperiodeID)
            .findFirst()
            .orElseThrow(() -> new IkkeFunnetException("Behandling " + behandlingsresultatID + " har ingen medlemskapsperiode med id " + medlemskapsperiodeID));

        oppdaterMedlemskapsperiode(medlemskapsperiode, fom, tom, innvilgelsesResultat, trygdedekning);

        MedlemAvFolketrygden medlemAvFolketrygden = medlemskapsperiode.getMedlemAvFolketrygden();
        oppdatereTrygdeavgiftsgrunnlag(behandlingsresultatID, medlemAvFolketrygden);

        return medlemskapsperiodeRepository.save(medlemskapsperiode);
    }

    private void oppdatereTrygdeavgiftsgrunnlag(Long behandlingsresultatID, MedlemAvFolketrygden medlemAvFolketrygden) {
        if (medlemAvFolketrygden.getFastsattTrygdeavgift() != null && medlemAvFolketrygden.getFastsattTrygdeavgift().getTrygdeavgiftsgrunnlag() != null) {
            List<Inntektsperiode> inntektsperioder =
                medlemAvFolketrygden.getFastsattTrygdeavgift().getTrygdeavgiftsgrunnlag().getInntektsperioder();
            List<SkatteforholdTilNorge> skatteforholdTilNorge =
                medlemAvFolketrygden.getFastsattTrygdeavgift().getTrygdeavgiftsgrunnlag().getSkatteforholdTilNorge();
            trygdeavgiftsgrunnlagService.oppdaterTrygdeavgiftsgrunnlag(behandlingsresultatID,
                new OppdaterTrygdeavgiftsgrunnlagRequest(skatteforholdTilNorge.stream().map(SkatteforholdTilNorgeRequest::new).toList(),
                    inntektsperioder.stream().map(InntektskildeRequest::new).toList()));
        }
    }

    private void oppdaterMedlemskapsperiode(Medlemskapsperiode medlemskapsperiode,
                                            LocalDate fom,
                                            LocalDate tom,
                                            InnvilgelsesResultat innvilgelsesResultat,
                                            Trygdedekninger trygdedekning) {
        if (fom == null || innvilgelsesResultat == null || trygdedekning == null) {
            throw new FunksjonellException("Fom-dato, innvilgelsesresultat og trygdedekning er påkrevd");
        } else if (!GYLDIGE_TRYGDEDEKNINGER.contains(trygdedekning)) {
            throw new FunksjonellException("Trygedekning " + trygdedekning + " støttes ikke for en medlemskapsperiode");
        } else if (feilIPeriode(fom, tom)) {
            throw new FunksjonellException("Tom-dato kan ikke være før fom-dato");
        }

        medlemskapsperiode.setTom(tom);
        medlemskapsperiode.setFom(fom);
        medlemskapsperiode.setInnvilgelsesresultat(innvilgelsesResultat);
        medlemskapsperiode.setTrygdedekning(trygdedekning);
    }

    @Transactional
    public void slettMedlemskapsperiode(long behandlingsresultatID, long medlemskapsperiodeID) {
        MedlemAvFolketrygden medlemAvFolketrygden = medlemAvFolketrygdenService.hentMedlemAvFolketrygden(behandlingsresultatID);
        Collection<Medlemskapsperiode> medlemskapsperioder = medlemAvFolketrygden.getMedlemskapsperioder();

        if (medlemskapsperioder.size() == 1) {
            throw new FunksjonellException("Behandlingen må ha minst en medlemskapsperiode");
        }

        var medlemskapsperiode = medlemskapsperioder.stream()
            .filter(m -> m.getId() == medlemskapsperiodeID)
            .findFirst()
            .orElseThrow(() -> new IkkeFunnetException("Finner ingen medlemskapsperiode med id " + medlemskapsperiodeID + " for behandling " + behandlingsresultatID));

        medlemAvFolketrygden.removeMedlemskapsperioder(medlemskapsperiode);
        oppdatereTrygdeavgiftsgrunnlag(behandlingsresultatID, medlemAvFolketrygden);
    }

    public Collection<Trygdedekninger> hentGyldigeTrygdedekninger() {
        return GYLDIGE_TRYGDEDEKNINGER;
    }

}
