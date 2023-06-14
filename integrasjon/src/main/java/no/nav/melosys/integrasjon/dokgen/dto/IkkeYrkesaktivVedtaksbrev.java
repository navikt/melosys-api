package no.nav.melosys.integrasjon.dokgen.dto;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.ser.InstantSerializer;
import no.nav.melosys.domain.brev.FritekstbrevBrevbestilling;
import no.nav.melosys.domain.brev.IkkeYrkesaktivBrevbestilling;
import no.nav.melosys.domain.kodeverk.Mottakerroller;
import no.nav.melosys.domain.kodeverk.begrunnelser.Ikkeyrkesaktivsituasjontype;
import no.nav.melosys.integrasjon.dokgen.dto.felles.SaksinfoBruker;
import no.nav.melosys.integrasjon.dokgen.dto.ikkeyrkesaktiv.IkkeYrkesaktivInnvilgelse;
import no.nav.melosys.integrasjon.dokgen.dto.ikkeyrkesaktiv.IkkeYrkesaktivPeriode;

import static com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING;

public class IkkeYrkesaktivVedtaksbrev extends DokgenDto {

    @JsonSerialize(using = InstantSerializer.class)
    @JsonFormat(shape = STRING)
    private final Instant datoMottatt;
    private final String sakstype;
    private final String behandlingstype;
    private final String nyVurderingBakgrunn;
    private final String oppholdsland;
    private final IkkeYrkesaktivPeriode periode;
    private final String artikkel;
    private final String bestemmelse;
    private final IkkeYrkesaktivInnvilgelse innvilgelse;
    private final Ikkeyrkesaktivsituasjontype ikkeYrkesaktivSituasjontype;

    protected IkkeYrkesaktivVedtaksbrev(IkkeYrkesaktivBrevbestilling brevbestilling) {
        super(brevbestilling, Mottakerroller.BRUKER); // TODO i MELOSYS-5738
        var fagsak = brevbestilling.getBehandling().getFagsak();

        this.datoMottatt = brevbestilling.getForsendelseMottatt();
        this.sakstype = fagsak.getType().getKode();
        this.behandlingstype = fagsak.hentSistOppdatertBehandling().getType().getKode();
        this.innvilgelse = new IkkeYrkesaktivInnvilgelse(brevbestilling.getInnledningFritekst(), brevbestilling.getBegrunnelseFritekst(), brevbestilling.getNyVurderingFritekst());
        this.nyVurderingBakgrunn = brevbestilling.getNyVurderingBakgrunn();
        this.oppholdsland = brevbestilling.getOppholdsLand();
        this.periode = new IkkeYrkesaktivPeriode(brevbestilling.getPeriodeFom(), brevbestilling.getPeriodeTom());
        this.bestemmelse = brevbestilling.getBestemmelse();
        this.artikkel = brevbestilling.getArtikkel();
        this.ikkeYrkesaktivSituasjontype = brevbestilling.getIkkeYrkesaktivSituasjontype();
    }

    public Instant getDatoMottatt() {
        return datoMottatt;
    }

    public String getSakstype() {
        return sakstype;
    }

    public String getBehandlingstype() {
        return behandlingstype;
    }

    public String getNyVurderingBakgrunn() {
        return nyVurderingBakgrunn;
    }

    public IkkeYrkesaktivPeriode getPeriode() {
        return periode;
    }

    public String getOppholdsland() {
        return oppholdsland;
    }

    @Override
    public SaksinfoBruker getSaksinfo() {
        return (SaksinfoBruker) super.getSaksinfo();
    }

    public Ikkeyrkesaktivsituasjontype getIkkeYrkesaktivSituasjontype() {
        return ikkeYrkesaktivSituasjontype;
    }

    public IkkeYrkesaktivInnvilgelse getInnvilgelse() {
        return innvilgelse;
    }

    public static IkkeYrkesaktivVedtaksbrev av(IkkeYrkesaktivBrevbestilling brevbestilling) {
        return new IkkeYrkesaktivVedtaksbrev(brevbestilling);
    }
}
