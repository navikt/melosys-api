package no.nav.melosys.service.eessi;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.dokument.medlemskap.Periode;
import no.nav.melosys.domain.dokument.sed.SedType;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.kafka.model.MelosysEessiMelding;
import no.nav.melosys.service.kontroll.PeriodeKontroller;
import no.nav.melosys.service.sak.FagsakService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

//A003,A009,A010
@Service
public class UnntaksperiodeMottakInitialiserer implements BehandleMottattSedInitialiserer {

    private static final Logger log = LoggerFactory.getLogger(UnntaksperiodeMottakInitialiserer.class);
    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final FagsakService fagsakService;
    private final LovvalgsperiodeService lovvalgsperiodeService;

    @Autowired
    public UnntaksperiodeMottakInitialiserer(FagsakService fagsakService, LovvalgsperiodeService lovvalgsperiodeService) {
        this.fagsakService = fagsakService;
        this.lovvalgsperiodeService = lovvalgsperiodeService;
    }

    @Override
    @Transactional
    public void initialiserProsessinstans(Prosessinstans prosessinstans) {
        MelosysEessiMelding melosysEessiMelding = prosessinstans.getData(ProsessDataKey.EESSI_MELDING, MelosysEessiMelding.class);

        if (skalBehandles(melosysEessiMelding)) {
            log.info("Behandler mottatt EESSI-medling. Buc: {}, SED: {}", melosysEessiMelding.getRinaSaksnummer(), melosysEessiMelding.getSedId());
            prosessinstans.setType(ProsessType.REGISTRERING_UNNTAK);
            prosessinstans.setSteg(ProsessSteg.REG_UNNTAK_OPPRETT_SAK_OG_BEH);
        } else {
            prosessinstans.setSteg(ProsessSteg.FERDIG);
        }
    }

    @Override
    public boolean gjelderSedType(SedType sedType) {
        return sedType == SedType.A003
            || sedType == SedType.A009
            || sedType == SedType.A010;
    }

    private boolean skalBehandles(MelosysEessiMelding melosysEessiMelding) {
        return !melosysEessiMelding.getErEndring() || periodeErEndret(melosysEessiMelding);
    }

    private boolean periodeErEndret(MelosysEessiMelding melosysEessiMelding) {
        Periode periode = tilPeriode(melosysEessiMelding.getPeriode());
        Lovvalgsperiode lovvalgsperiode;

        try {
            Optional<Fagsak> eksisterendeFagsak = fagsakService.hentFagsakFraGsakSaksnummer(melosysEessiMelding.getGsakSaksnummer());
            if (eksisterendeFagsak.isPresent()) {
                Fagsak fagsak = eksisterendeFagsak.get();
                Behandling behandling = fagsak.getTidligsteInaktiveBehandling();
                lovvalgsperiode = lovvalgsperiodeService.hentOpprinneligLovvalgsperiode(behandling.getId());
                return !PeriodeKontroller.periodeErLik(lovvalgsperiode.getFom(), lovvalgsperiode.getTom(),
                    periode.getFom(), periode.getTom());
            }
        } catch (IkkeFunnetException ex) {
            // Om ikke finner fagsak -> behandle på nytt
            return true;
        }

        return true;
    }

    private Periode tilPeriode(no.nav.melosys.service.kafka.model.Periode periode) {
        return new Periode(
            LocalDate.parse(periode.getFom(), dateTimeFormatter),
            periode.getTom() != null ? LocalDate.parse(periode.getTom(), dateTimeFormatter) : null
        );
    }
}
