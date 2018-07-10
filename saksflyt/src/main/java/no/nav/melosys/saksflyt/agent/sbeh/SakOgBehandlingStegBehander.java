package no.nav.melosys.saksflyt.agent.sbeh;

import java.util.Map;

import no.nav.melosys.domain.BehandlingType;
import no.nav.melosys.domain.Tema;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.feil.Feilkategori;
import no.nav.melosys.saksflyt.agent.AbstraktStegBehandler;
import no.nav.melosys.saksflyt.agent.UnntakBehandler;
import no.nav.melosys.saksflyt.agent.unntak.FeilStrategi;

import static no.nav.melosys.domain.BehandlingType.SØKNAD;
import static no.nav.melosys.domain.BehandlingType.UNNTAK_MEDL;
import static no.nav.melosys.domain.Tema.MED;
import static no.nav.melosys.domain.Tema.UFM;

public abstract class SakOgBehandlingStegBehander extends AbstraktStegBehandler {

    @Override
    protected Map<Feilkategori, UnntakBehandler> unntaksHåndtering() {
        return FeilStrategi.standardFeilHåndtering();
    }

    protected static Tema avgjørArkivTema(BehandlingType behandlingType) throws TekniskException {
        if (behandlingType == SØKNAD) {
            return MED;
        } else if (behandlingType == UNNTAK_MEDL) {
            return UFM;
        } else {
            throw new TekniskException("Støtter ikke behandlingstype " + behandlingType);
        }
    }
}
