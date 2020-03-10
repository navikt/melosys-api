package no.nav.melosys.service.eessi;

import java.util.Optional;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.dokument.medlemskap.Periode;
import no.nav.melosys.domain.eessi.SedType;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessType;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
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
    public RutingResultat finnSakOgBestemRuting(Prosessinstans prosessinstans, Long gsakSaksnummer) {

        if (gsakSaksnummer == null) {
            return RutingResultat.NY_SAK;
        }

        MelosysEessiMelding melosysEessiMelding = prosessinstans.getData(ProsessDataKey.EESSI_MELDING, MelosysEessiMelding.class);

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
        if (sedType == SedType.A009) {
            return Behandlingstyper.REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING;
        } else if (sedType == SedType.A010) {
            return Behandlingstyper.REGISTRERING_UNNTAK_NORSK_TRYGD_ØVRIGE;
        } else if (sedType == SedType.A003) {
            return Behandlingstyper.BESLUTNING_LOVVALG_ANNET_LAND;
        }

        throw new IllegalArgumentException("UnntaksperiodeMottakInitialiserer støtter ikke sedtype " + sedType);
    }

    private boolean periodeErEndret(MelosysEessiMelding melosysEessiMelding, Behandling behandling) {
        Periode periode = tilPeriode(melosysEessiMelding.getPeriode());
        String lovvalgsLand = melosysEessiMelding.getLovvalgsland();

        return lovvalgsperiodeService.hentLovvalgsperioder(behandling.getId()).stream().findFirst().map(lovvalgsperiode ->
            !PeriodeKontroller.periodeErLik(lovvalgsperiode.getFom(), lovvalgsperiode.getTom(), periode.getFom(), periode.getTom())
            || !lovvalgsLand.equalsIgnoreCase(lovvalgsperiode.getLovvalgsland().getKode()))
            .orElse(true);
    }

    private static Periode tilPeriode(no.nav.melosys.domain.eessi.melding.Periode periode) {
        return new Periode(
            periode.getFom(),
            periode.getTom()
        );
    }
}
