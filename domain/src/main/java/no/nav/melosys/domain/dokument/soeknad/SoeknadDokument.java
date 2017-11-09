package no.nav.melosys.domain.dokument.soeknad;

import javax.xml.bind.annotation.XmlRootElement;

import no.nav.melosys.domain.dokument.SaksopplysningDokument;
import no.nav.melosys.domain.dokument.felles.Periode;

/**
 * FIXME: EESSI2-424
 */
@XmlRootElement
public class SoeknadDokument extends SaksopplysningDokument {
    
    public Periode periode;

}
