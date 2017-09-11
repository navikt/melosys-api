package no.nav.melosys.domain.dokument;

import java.time.LocalDate;

public class PersonopplysningDokument extends SaksopplysningDokument {

    // FIXME: Har kun kopiert feltene fra gui-tjenester
    
    private String fnr;

    private String sivilstand;

    private String statsborgerskap;

    private String kjoenn;

    private String fornavn;

    private String etternavn;

    private String sammensattNavn;

    private LocalDate foedselsdato;

}
