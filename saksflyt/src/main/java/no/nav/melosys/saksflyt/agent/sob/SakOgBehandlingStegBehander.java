package no.nav.melosys.saksflyt.agent.sob;

import java.util.Map;

import no.nav.melosys.domain.Behandlingstype;
import no.nav.melosys.domain.Tema;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.feil.Feilkategori;
import no.nav.melosys.saksflyt.agent.AbstraktStegBehandler;
import no.nav.melosys.saksflyt.agent.UnntakBehandler;
import no.nav.melosys.saksflyt.agent.unntak.FeilStrategi;

import static no.nav.melosys.domain.Behandlingstype.SØKNAD;
import static no.nav.melosys.domain.Tema.MED;

public abstract class SakOgBehandlingStegBehander extends AbstraktStegBehandler {

    @Override
    protected Map<Feilkategori, UnntakBehandler> unntaksHåndtering() {
        return FeilStrategi.standardFeilHåndtering();
    }

    protected static Tema avgjørArkivTema(Behandlingstype behandlingstype) throws TekniskException {
        if (behandlingstype == SØKNAD) {
            return MED;
        } else {
            throw new TekniskException("Støtter ikke behandlingstype " + behandlingstype);
        }
    }
}
