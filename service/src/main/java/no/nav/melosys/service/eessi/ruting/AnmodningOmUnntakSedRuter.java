package no.nav.melosys.service.eessi.ruting;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

import no.nav.melosys.domain.behandling.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.eessi.SedType;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.domain.kodeverk.Saksstatuser;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.service.sak.FagsakService;
import no.nav.melosys.service.saksflyt.ProsessinstansService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

//A001
@Service
public class AnmodningOmUnntakSedRuter implements SedRuterForSedTyper {

    private final ProsessinstansService prosessinstansService;
    private final FagsakService fagsakService;

    @Autowired
    public AnmodningOmUnntakSedRuter(ProsessinstansService prosessinstansService, FagsakService fagsakService) {
        this.prosessinstansService = prosessinstansService;
        this.fagsakService = fagsakService;
    }

    @Override
    public void rutSedTilBehandling(Prosessinstans prosessinstans, Long arkivsakID) throws FunksjonellException {
        final MelosysEessiMelding melosysEessiMelding = prosessinstans.getData(ProsessDataKey.EESSI_MELDING, MelosysEessiMelding.class);
        if (arkivsakID == null) {
            opprettNySak(prosessinstans, melosysEessiMelding);
            return;
        }

        // TODO: Avklares hva som skal gjøres ved oppdatert SED
        Optional<Fagsak> fagsak = fagsakService.finnFagsakFraArkivsakID(arkivsakID);
        if (fagsak.isPresent()) {
            Behandling behandling = fagsak.get().getSistOppdaterteBehandling();
            prosessinstans.setBehandling(behandling);
            if (fagsak.get().getStatus() != Saksstatuser.OPPRETTET) {
                opprettJournalføringProsess(melosysEessiMelding, behandling);
            } else {
                opprettNyBehandling(melosysEessiMelding, arkivsakID);

            }
        } else {
            opprettNySak(prosessinstans, melosysEessiMelding);
        }
    }

    private void opprettJournalføringProsess(MelosysEessiMelding melosysEessiMelding, Behandling sistAktiveBehandling) {
        prosessinstansService.opprettProsessinstansSedJournalføring(
            sistAktiveBehandling,
            melosysEessiMelding
        );
    }

    private void opprettNySak(Prosessinstans prosessinstans, MelosysEessiMelding melosysEessiMelding) {
        prosessinstansService.opprettProsessinstansNySakMottattAnmodningOmUnntak(
            melosysEessiMelding,
            prosessinstans.hentAktørIDFraDataEllerSED()
        );
    }

    private void opprettNyBehandling(MelosysEessiMelding melosysEessiMelding, Long arkivsakID) {
        prosessinstansService.opprettProsessinstansNyBehandlingMottattAnmodningUnntak(
            melosysEessiMelding,
            arkivsakID
        );
    }

    @Override
    public Collection<SedType> gjelderSedTyper() {
        return Collections.singleton(SedType.A001);
    }
}
