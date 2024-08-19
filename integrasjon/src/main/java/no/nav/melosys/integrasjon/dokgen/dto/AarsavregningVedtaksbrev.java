package no.nav.melosys.integrasjon.dokgen.dto;

import java.time.LocalDate;

import no.nav.melosys.domain.brev.AarsavregningVedtakBrevBestilling;
import no.nav.melosys.domain.kodeverk.Mottakerroller;
import no.nav.melosys.integrasjon.dokgen.dto.felles.SaksinfoBruker;

public class AarsavregningVedtaksbrev extends DokgenDto {

    private final String innledningFritekst;
    private final String begrunnelseFritekst;
    private final LocalDate datoMottatt;

    public AarsavregningVedtaksbrev(AarsavregningVedtakBrevBestilling brevbestilling) {
        super(brevbestilling, Mottakerroller.BRUKER);

        this.innledningFritekst = brevbestilling.getInnledningFritekst();
        this.begrunnelseFritekst = brevbestilling.getBegrunnelseFritekst();
        this.datoMottatt = instantTilLocalDate(brevbestilling.getForsendelseMottatt());
    }

    public String getBegrunnelseFritekst() {
        return begrunnelseFritekst;
    }

    public String getInnledningFritekst() {
        return innledningFritekst;
    }

    public LocalDate getDatoMottatt() {
        return datoMottatt;
    }

    @Override
    public SaksinfoBruker getSaksinfo() {
        return (SaksinfoBruker) super.getSaksinfo();
    }
}
