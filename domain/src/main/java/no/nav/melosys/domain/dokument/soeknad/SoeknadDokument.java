package no.nav.melosys.domain.dokument.soeknad;

import javax.xml.bind.annotation.XmlRootElement;

import no.nav.melosys.domain.ErPeriode;
import no.nav.melosys.domain.HarPeriode;
import no.nav.melosys.domain.dokument.SaksopplysningDokument;
import no.nav.melosys.domain.dokument.felles.Periode;

/**
 * FIXME: EESSI2-424
 */
@XmlRootElement
public class SoeknadDokument extends SaksopplysningDokument implements HarPeriode {
    
    public Periode periode;
    
    @Override
    public ErPeriode getPeriode() {
        return periode;
    }

}
