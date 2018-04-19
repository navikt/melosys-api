package no.nav.melosys.tjenester.gui.dto.journalforing;

import no.nav.melosys.domain.FagsakType;

public class JournalpostDto {
    private AktoerDto bruker;
    private boolean erBrukerAvsender;
    private AktoerDto avsender;
    private FagsakType sakstype;
    private DokumentDto dokument;

    public AktoerDto getBruker() {
        return bruker;
    }

    public void setBruker(AktoerDto bruker) {
        this.bruker = bruker;
    }

    public boolean isErBrukerAvsender() {
        return erBrukerAvsender;
    }

    public void setErBrukerAvsender(boolean erBrukerAvsender) {
        this.erBrukerAvsender = erBrukerAvsender;
    }

    public AktoerDto getAvsender() {
        return avsender;
    }

    public void setAvsender(AktoerDto avsender) {
        this.avsender = avsender;
    }

    public FagsakType getSakstype() {
        return sakstype;
    }

    public void setSakstype(FagsakType sakstype) {
        this.sakstype = sakstype;
    }

    public DokumentDto getDokument() {
        return dokument;
    }

    public void setDokument(DokumentDto dokument) {
        this.dokument = dokument;
    }
}
