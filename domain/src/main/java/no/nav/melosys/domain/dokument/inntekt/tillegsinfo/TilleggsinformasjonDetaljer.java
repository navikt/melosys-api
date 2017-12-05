package no.nav.melosys.domain.dokument.inntekt.tillegsinfo;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "TilleggsinformasjonDetaljer")
@XmlSeeAlso({
        AldersUfoereEtterlatteAvtalefestetOgKrigspensjon.class,
        BarnepensjonOgUnderholdsbidrag.class,
        BonusFraForsvaret.class,
        Etterbetalingsperiode.class,
        Inntjeningsforhold.class,
        Svalbardinntekt.class,
        ReiseKostOgLosji.class
})
public abstract class TilleggsinformasjonDetaljer {
}
