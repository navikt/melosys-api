package no.nav.melosys.service.eessi;

import java.util.Optional;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.dokument.medlemskap.Periode;
import no.nav.melosys.domain.dokument.sed.SedType;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.kontroll.PeriodeKontroller;
import no.nav.melosys.service.sak.FagsakService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

//A003,A009,A010
@Service
public class UnntaksperiodeMottakInitialiserer implements AutomatiskSedBehandlingInitialiserer {

    private final FagsakService fagsakService;
    private final LovvalgsperiodeService lovvalgsperiodeService;

    @Autowired
    public UnntaksperiodeMottakInitialiserer(FagsakService fagsakService,
                                             LovvalgsperiodeService lovvalgsperiodeService) {
        this.fagsakService = fagsakService;
        this.lovvalgsperiodeService = lovvalgsperiodeService;
    }

    @Override
    public RutingResultat finnSakOgBestemRuting(Prosessinstans prosessinstans, Long gsakSaksnummer) throws FunksjonellException {
        MelosysEessiMelding melosysEessiMelding = prosessinstans.getData(ProsessDataKey.EESSI_MELDING, MelosysEessiMelding.class);

        if (gsakSaksnummer == null) {
            return RutingResultat.NY_SAK;
        }

        Optional<Fagsak> fagsak = fagsakService.finnFagsakFraGsakSaksnummer(gsakSaksnummer);
        if (fagsak.isPresent()) {
            Behandling behandling = fagsak.get().getSistOppdaterteBehandling();
            if (periodeErEndret(melosysEessiMelding, behandling)) {
                return RutingResultat.NY_BEHANDLING;
            } else {
                prosessinstans.setBehandling(behandling);
                return RutingResultat.INGEN_BEHANDLING;
            }
        } else {
            return RutingResultat.NY_SAK;
        }
    }

    @Override
    public boolean gjelderSedType(SedType sedType, Landkoder lovvalgsland) {
        return (sedType == SedType.A003 && lovvalgsland != Landkoder.NO)
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

        Optional<Lovvalgsperiode> lovvalgsperiode = lovvalgsperiodeService.finnOpprinneligLovvalgsperiode(behandling.getId());
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
