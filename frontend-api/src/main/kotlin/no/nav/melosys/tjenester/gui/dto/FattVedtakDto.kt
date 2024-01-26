package no.nav.melosys.tjenester.gui.dto

import no.nav.melosys.domain.kodeverk.Vedtakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.integrasjon.faktureringskomponenten.dto.FaktureringsIntervall
import no.nav.melosys.service.dokument.brev.KopiMottakerDto
import java.time.LocalDate

class FattVedtakDto {
    @JvmField
    var behandlingsresultatTypeKode: Behandlingsresultattyper? = null
    @JvmField
    var vedtakstype: Vedtakstyper? = null
    @JvmField
    var fritekst: String? = null
    @JvmField
    var fritekstSed: String? = null
    @JvmField
    var mottakerinstitusjoner: Set<String>? = null
    @JvmField
    var nyVurderingBakgrunn: String? = null
    @JvmField
    var innledningFritekst: String? = null
    @JvmField
    var begrunnelseFritekst: String? = null
    @JvmField
    var ektefelleFritekst: String? = null
    @JvmField
    var barnFritekst: String? = null
    @JvmField
    var trygdeavgiftFritekst: String? = null
    @JvmField
    var kopiMottakere: List<KopiMottakerDto>? = null
    @JvmField
    var betalingsintervall: FaktureringsIntervall? = null
    @JvmField
    var kopiTilArbeidsgiver: Boolean? = null
    @JvmField
    var opphoerDato: LocalDate? = null
}
