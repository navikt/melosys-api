package no.nav.melosys.tjenester.gui.dto.behandlingsgrunnlag.data;

import no.nav.melosys.domain.dokument.soeknad.ArbeidsgiversBekreftelse;
import no.nav.melosys.domain.dokument.soeknad.Arbeidsinntekt;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;

public class SoeknadDokumentDto extends BehandlingsgrunnlagDataDto {
    private Arbeidsinntekt arbeidsinntekt = new Arbeidsinntekt();
    private ArbeidsgiversBekreftelse arbeidsgiversBekreftelse = new ArbeidsgiversBekreftelse();

    public SoeknadDokumentDto() {
    }

    public SoeknadDokumentDto(SoeknadDokument soeknadDokument) {
        super(soeknadDokument);
        arbeidsinntekt = soeknadDokument.arbeidsinntekt;
        arbeidsgiversBekreftelse = soeknadDokument.arbeidsgiversBekreftelse;
    }

    public Arbeidsinntekt getArbeidsinntekt() {
        return arbeidsinntekt;
    }

    public ArbeidsgiversBekreftelse getArbeidsgiversBekreftelse() {
        return arbeidsgiversBekreftelse;
    }
}
