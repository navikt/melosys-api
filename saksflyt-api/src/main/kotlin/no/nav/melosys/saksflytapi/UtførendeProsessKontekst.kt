package no.nav.melosys.saksflytapi

import no.nav.melosys.saksflytapi.domain.ProsessPrioritet

/**
 * Bærer prioriteten til prosessinstansen som akkurat nå utføres av saksflyt-motoren, slik at
 * sub-prosesser opprettet under et saga-steg kan arve (løftes til) parentens prioritet i
 * [ProsessinstansService.lagre] – uten et ekstra databaseoppslag.
 *
 * Settes kun rundt utførelsen av et steg i `ProsessinstansBehandler`, det eneste stedet en ekte,
 * persistert prosessinstans utføres. System-/batch-kontekster (satsendring, årsavregning,
 * skattehendelser osv.) setter en syntetisk `UUID.randomUUID()` som prosess-id, men ikke denne –
 * de oppretter rot-prosesser uten en parent å arve fra. Propageringen blir da en ren no-op for dem,
 * uten et garantert-bom oppslag mot databasen per opprettede sub-prosess.
 *
 * Merk: en batch-type ([ProsessPrioritet.LAV]) kan også opprettes som et bivirkning-steg inne i en
 * HØY/NORMAL-flyt (der konteksten *ikke* er tom – f.eks. OPPRETT_NY_BEHANDLING_AARSAVREGNING fra steget
 * OPPRETTE_AARSAVREGNING_ENDRING). For at slike ikke skal løftes ut av batch-båndet, unntar
 * [ProsessinstansService] sin propagering LAV-typer fra løft eksplisitt: batch forblir alltid batch.
 */
object UtførendeProsessKontekst {
    private val prioritetHolder = ThreadLocal<ProsessPrioritet?>()

    @JvmStatic
    fun settPrioritet(prioritet: ProsessPrioritet) = prioritetHolder.set(prioritet)

    @JvmStatic
    fun nullstill() = prioritetHolder.remove()

    @JvmStatic
    fun gjeldendePrioritet(): ProsessPrioritet? = prioritetHolder.get()
}
