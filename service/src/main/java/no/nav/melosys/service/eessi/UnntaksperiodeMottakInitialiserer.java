package no.nav.melosys.service.eessi;

import java.util.Optional;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.dokument.medlemskap.Periode;
import no.nav.melosys.domain.dokument.sed.SedType;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.integrasjon.gsak.GsakFasade;
import no.nav.melosys.repository.AvklarteFaktaRepository;
import no.nav.melosys.repository.SaksopplysningRepository;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.kontroll.PeriodeKontroller;
import no.nav.melosys.service.sak.FagsakService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

//A003,A009,A010
@Service
public class UnntaksperiodeMottakInitialiserer implements BehandleMottattSedInitialiserer {

    private static final Logger log = LoggerFactory.getLogger(UnntaksperiodeMottakInitialiserer.class);

    private final FagsakService fagsakService;
    private final LovvalgsperiodeService lovvalgsperiodeService;
    private final GsakFasade gsakFasade;
    private final SaksopplysningRepository saksopplysningRepository;
    private final AvklarteFaktaRepository avklarteFaktaRepository;

    @Autowired
    public UnntaksperiodeMottakInitialiserer(FagsakService fagsakService,
                                             LovvalgsperiodeService lovvalgsperiodeService,
                                             @Qualifier("system") GsakFasade gsakFasade,
                                             SaksopplysningRepository saksopplysningRepository,
                                             AvklarteFaktaRepository avklarteFaktaRepository) {
        this.fagsakService = fagsakService;
        this.lovvalgsperiodeService = lovvalgsperiodeService;
        this.gsakFasade = gsakFasade;
        this.saksopplysningRepository = saksopplysningRepository;
        this.avklarteFaktaRepository = avklarteFaktaRepository;
    }

    @Override
    public InitialiseringResultat initialiserProsessinstans(Prosessinstans prosessinstans, Long gsakSaksnummer) throws FunksjonellException {
        MelosysEessiMelding melosysEessiMelding = prosessinstans.getData(ProsessDataKey.EESSI_MELDING, MelosysEessiMelding.class);

        if (gsakSaksnummer == null) {
            return InitialiseringResultat.NY_SAK;
        }

        Optional<Fagsak> fagsak = fagsakService.hentFagsakFraGsakSaksnummer(gsakSaksnummer);
        if (fagsak.isPresent()) {
            Behandling behandling = fagsak.get().getSistOppdaterteBehandling();
            if (periodeErEndret(melosysEessiMelding, behandling)) {
                return InitialiseringResultat.NY_BEHANDLING;
            } else {
                prosessinstans.setBehandling(behandling);
                return InitialiseringResultat.INGEN_BEHANDLING;
            }
        } else {
            return InitialiseringResultat.NY_SAK;
        }
    }

    @Override
    public boolean gjelderSedType(SedType sedType) {
        return sedType == SedType.A003
            || sedType == SedType.A009
            || sedType == SedType.A010;
    }

    @Override
    public Behandlingstyper hentBehandlingstype(MelosysEessiMelding melosysEessiMelding) {
        return hentBehandlingstypeForSedType(SedType.valueOf(melosysEessiMelding.getSedType()));
    }

    @Override
    public ProsessType hentAktuellProsessType() {
        return ProsessType.REGISTRERING_UNNTAK;
    }

    private Behandlingstyper hentBehandlingstypeForSedType(SedType sedType) {
        if (sedType == SedType.A009 || sedType == SedType.A010) {
            return Behandlingstyper.REGISTRERING_UNNTAK_NORSK_TRYGD;
        } else if (sedType == SedType.A003) {
            return Behandlingstyper.UTL_MYND_UTPEKT_SEG_SELV;
        }

        throw new IllegalArgumentException("UnntaksperiodeMottakInitialiserer støtter ikke sedtype " + sedType);
    }

    private boolean periodeErEndret(MelosysEessiMelding melosysEessiMelding, Behandling behandling) throws IkkeFunnetException {
        Periode periode = tilPeriode(melosysEessiMelding.getPeriode());

        Optional<Lovvalgsperiode> lovvalgsperiode = lovvalgsperiodeService.hentOpprinneligLovvalgsperiodeOptional(behandling.getId());
        return lovvalgsperiode.map(value -> !PeriodeKontroller.periodeErLik(value.getFom(), value.getTom(),
            periode.getFom(), periode.getTom())).orElse(true);
    }

    private static Periode tilPeriode(no.nav.melosys.domain.eessi.melding.Periode periode) {
        return new Periode(
            periode.getFom(),
            periode.getTom()
        );
    }
}
