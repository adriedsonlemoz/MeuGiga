package br.com.meugiga.app.domain

import br.com.meugiga.app.domain.model.AppKind

data class SpecialUid(
    val label: String,
    val kind: AppKind,
)

object SpecialUidClassifier {
    const val UID_ALL = -1
    const val UID_REMOVED = -4
    const val UID_TETHERING = -5
    const val SYSTEM_UID = 1_000
    const val FIRST_APPLICATION_UID = 10_000

    fun classify(uid: Int): SpecialUid? = when (uid) {
        UID_ALL -> SpecialUid("Consumo não especificado", AppKind.AGGREGATE)
        UID_REMOVED -> SpecialUid("Aplicativos removidos", AppKind.REMOVED)
        UID_TETHERING -> SpecialUid("Roteador e compartilhamento", AppKind.TETHERING)
        SYSTEM_UID -> SpecialUid("Sistema Android", AppKind.SYSTEM)
        in 0 until FIRST_APPLICATION_UID -> SpecialUid(
            "Componente do sistema (UID $uid)",
            AppKind.SYSTEM,
        )
        else -> null
    }
}
