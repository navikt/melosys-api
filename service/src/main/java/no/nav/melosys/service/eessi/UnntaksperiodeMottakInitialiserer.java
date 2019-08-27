package no.nav.melosys.service.eessi;

import java.util.Optional;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.dokument.medlemskap.Periode;
import no.nav.melosys.domain.dokument.sed.SedType;
import no.nav.melosys.domain.kodeverk.Behandlingstyper;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.repository.AvklarteFaktaRepository;
import no.nav.melosys.repository.SaksopplysningRepository;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.kafka.model.MelosysEessiMelding;
import no.nav.melosys.service.kontroll.PeriodeKontroller;
import no.nav.melosys.service.oppgave.OppgaveService;
import no.nav.melosys.service.sak.FagsakService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

//A003,A009,A010
@Service
public class UnntaksperiodeMottakInitialiserer implements BehandleMottattSedInitialiserer {

    private static final Logger log = LoggerFactory.getLogger(UnntaksperiodeMottakInitialiserer.class);

    private final FagsakService fagsakService;
    private final LovvalgsperiodeService lovvalgsperiodeService;
    private final OppgaveService oppgaveService;
    private final SaksopplysningRepository saksopplysningRepository;
    private final AvklarteFaktaRepository avklarteFaktaRepository;

    @Autowired
    public UnntaksperiodeMottakInitialiserer(FagsakService fagsakService,
                                             LovvalgsperiodeService lovvalgsperiodeService,
                                             @Qualifier("system") OppgaveService oppgaveService,
                                             SaksopplysningRepository saksopplysningRepository,
                                             AvklarteFaktaRepository avklarteFaktaRepository) {
        this.fagsakService = fagsakService;
        this.lovvalgsperiodeService = lovvalgsperiodeService;
        this.oppgaveService = oppgaveService;
        this.saksopplysningRepository = saksopplysningRepository;
        this.avklarteFaktaRepository = avklarteFaktaRepository;
    }

    @Override
    @Transactional
    public void initialiserProsessinstans(Prosessinstans prosessinstans) throws FunksjonellException, TekniskException {
        MelosysEessiMelding melosysEessiMelding = prosessinstans.getData(ProsessDataKey.EESSI_MELDING, MelosysEessiMelding.class);
        SedType sedType = SedType.valueOf(melosysEessiMelding.getSedType());

        if (skalBehandlesPåEksisterendeBehandling(melosysEessiMelding)) {
            log.info("Behandler mottatt EESSI-medling på en eksisterende behandling. Buc: {}, SED: {}", melosysEessiMelding.getRinaSaksnummer(), melosysEessiMelding.getSedId());
            initialiserForEksisterendeBehandling(prosessinstans, melosysEessiMelding, sedType);
        } else if (skalBehandlesPåNyBehandling(melosysEessiMelding)) {
            log.info("Behandler mottatt EESSI-medling. Buc: {}, SED: {}", melosysEessiMelding.getRinaSaksnummer(), melosysEessiMelding.getSedId());
            prosessinstans.setData(ProsessDataKey.BEHANDLINGSTYPE, hentBehandlingstypeForSedType(sedType));
            prosessinstans.setType(ProsessType.REGISTRERING_UNNTAK);
            prosessinstans.setSteg(ProsessSteg.REG_UNNTAK_OPPRETT_SAK_OG_BEH);
        } else {
            prosessinstans.setSteg(ProsessSteg.FERDIG);
        }
    }

    private void initialiserForEksisterendeBehandling(Prosessinstans prosessinstans, MelosysEessiMelding melosysEessiMelding, SedType sedType) throws TekniskException, FunksjonellException {
        Optional<Behandling> behandlingOptional = hentSistOppdaterteBehandling(melosysEessiMelding);
        if (!behandlingOptional.isPresent()) {
            throw new IkkeFunnetException("Finner ikke sist oppdaterte behandling for gsak " + melosysEessiMelding.getGsakSaksnummer());
        }

        Behandling behandling = behandlingOptional.get();
        oppgaveService.ferdigstillOppgaveMedSaksnummer(behandling.getFagsak().getSaksnummer());
        saksopplysningRepository.deleteAllByBehandling(behandling);
        avklarteFaktaRepository.deleteByBehandlingsresultatId(behandling.getId());

        prosessinstans.setBehandling(behandling);
        prosessinstans.setData(ProsessDataKey.BEHANDLINGSTYPE, hentBehandlingstypeForSedType(sedType));
        prosessinstans.setType(ProsessType.REGISTRERING_UNNTAK);
        prosessinstans.setSteg(ProsessSteg.REG_UNNTAK_OPPRETT_SEDDOKUMENT);
    }

    private static Behandlingstyper hentBehandlingstypeForSedType(SedType sedType) {
        switch (sedType) {
            case A003:
                return Behandlingstyper.UTL_MYND_UTPEKT_SEG_SELV;
            case A009:
            case A010:
                return Behandlingstyper.REGISTRERING_UNNTAK_NORSK_TRYGD;
            default:
                throw new IllegalArgumentException("UnntaksperiodeMottakInitialiserer støtter ikke sedtype " + sedType);
        }
    }

    @Override
    public boolean gjelderSedType(SedType sedType) {
        return sedType == SedType.A003
            || sedType == SedType.A009
            || sedType == SedType.A010;
    }

    private boolean skalBehandlesPåNyBehandling(MelosysEessiMelding melosysEessiMelding) {
        return !melosysEessiMelding.getErEndring() || periodeErEndret(melosysEessiMelding);
    }

    private boolean periodeErEndret(MelosysEessiMelding melosysEessiMelding) {
        Periode periode = tilPeriode(melosysEessiMelding.getPeriode());
        Lovvalgsperiode lovvalgsperiode;

        try {
            Optional<Behandling> behandling = hentSistOppdaterteBehandling(melosysEessiMelding);

            if (behandling.isPresent()) {
                lovvalgsperiode = lovvalgsperiodeService.hentOpprinneligLovvalgsperiode(behandling.get().getId());
                return !PeriodeKontroller.periodeErLik(lovvalgsperiode.getFom(), lovvalgsperiode.getTom(),
                    periode.getFom(), periode.getTom());
            }
        } catch (IkkeFunnetException ex) {
            // Om ikke finner fagsak -> behandle på nytt
            return true;
        }

        return true;
    }

    private boolean skalBehandlesPåEksisterendeBehandling(MelosysEessiMelding melosysEessiMelding) {
        return sisteOppdaterteBehandlingErAktiv(melosysEessiMelding) &&
            (!melosysEessiMelding.getErEndring() || periodeErEndret(melosysEessiMelding));
    }

    private boolean sisteOppdaterteBehandlingErAktiv(MelosysEessiMelding melosysEessiMelding) {
        return hentSistOppdaterteBehandling(melosysEessiMelding).map(Behandling::erAktiv).orElse(false);
    }

    private Optional<Behandling> hentSistOppdaterteBehandling(MelosysEessiMelding melosysEessiMelding) {
        Optional<Fagsak> eksisterendeFagsak = fagsakService.hentFagsakFraGsakSaksnummer(melosysEessiMelding.getGsakSaksnummer());

        if (eksisterendeFagsak.isPresent()) {
            Fagsak fagsak = eksisterendeFagsak.get();
            return Optional.ofNullable(fagsak.getSistOppdaterteBehandling());
        }

        return Optional.empty();
    }

    private static Periode tilPeriode(no.nav.melosys.service.kafka.model.Periode periode) {
        return new Periode(
            periode.getFom(),
            periode.getTom()
        );
    }
}
