package no.nav.melosys.integrasjon.dokgen.dto;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;
import no.nav.melosys.domain.PeriodeOmLovvalg;
import no.nav.melosys.domain.brev.AvslagEftaStorbritanniaBrevbestilling;
import no.nav.melosys.domain.kodeverk.Mottakerroller;
import no.nav.melosys.integrasjon.dokgen.dto.felles.SaksinfoBruker;

import static com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING;

public class AvslagEftaStorbritannia extends DokgenDto {

    private final String innledningFritekst;
    private final String begrunnelseFritekst;
    private final String virksomhetNavn;
    @JsonFormat(shape = STRING)
    private final LocalDate periodeFom;
    @JsonFormat(shape = STRING)
    private final LocalDate periodeTom;
    @JsonFormat(shape = STRING)
    private final LocalDate datoMottatt;

    public AvslagEftaStorbritannia(AvslagEftaStorbritanniaBrevbestilling brevbestilling, PeriodeOmLovvalg lovvalgsperiode, String virksomhetNavn) {
        super(brevbestilling, Mottakerroller.BRUKER);

        this.innledningFritekst = brevbestilling.getInnledningFritekstAvslagEfta();
        this.begrunnelseFritekst = brevbestilling.getBegrunnelseFritekstAvslagEfta();
        this.datoMottatt = instantTilLocalDate(brevbestilling.getForsendelseMottatt());
        this.periodeFom = lovvalgsperiode.getFom();
        this.periodeTom = lovvalgsperiode.getTom();
        this.virksomhetNavn = virksomhetNavn;
    }

    public String getBegrunnelseFritekst() {
        return begrunnelseFritekst;
    }

    public String getVirksomhetNavn() {
        return virksomhetNavn;
    }

    public LocalDate getPeriodeFom() {
        return periodeFom;
    }

    public LocalDate getPeriodeTom() {
        return periodeTom;
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
