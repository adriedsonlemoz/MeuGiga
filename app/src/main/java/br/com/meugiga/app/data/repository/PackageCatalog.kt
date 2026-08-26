package br.com.meugiga.app.data.repository

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import br.com.meugiga.app.data.database.AppIdentityEntity
import br.com.meugiga.app.data.database.MobileUsageDao
import br.com.meugiga.app.domain.SpecialUidClassifier
import br.com.meugiga.app.domain.model.AppDescriptor
import br.com.meugiga.app.domain.model.AppKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class PackageCatalog(
    context: Context,
    private val dao: MobileUsageDao,
    private val packageManager: PackageManager = context.packageManager,
) {
    private val cacheMutex = Mutex()
    private val descriptorCache = mutableMapOf<Int, AppDescriptor>()
    private var launchableByUid: Map<Int, List<ApplicationInfo>> = emptyMap()
    private var catalogLoadedAtMillis: Long = 0

    suspend fun resolve(uids: Collection<Int>): Map<Int, AppDescriptor> =
        withContext(Dispatchers.IO) {
            cacheMutex.withLock {
                val now = System.currentTimeMillis()
                if (now - catalogLoadedAtMillis >= CATALOG_CACHE_MILLIS || launchableByUid.isEmpty()) {
                    launchableByUid = launchableApplications().groupBy { it.uid }
                    descriptorCache.clear()
                    catalogLoadedAtMillis = now
                }
                val persisted = dao.appIdentities(uids.filter { it >= 0 })
                    .associateBy { it.uid }
                val resolved = uids.associateWith { uid ->
                    descriptorCache.getOrPut(uid) {
                        descriptorFor(uid, launchableByUid[uid].orEmpty(), persisted[uid])
                    }
                }
                val identities = resolved.values.mapNotNull { descriptor ->
                    descriptor.packageName?.let { packageName ->
                        AppIdentityEntity(
                            uid = descriptor.uid,
                            packageName = packageName,
                            label = descriptor.label,
                            isSystem = descriptor.isSystem,
                            lastSeenAtMillis = now,
                        )
                    }
                }
                if (identities.isNotEmpty()) dao.upsertAppIdentities(identities)
                resolved
            }
        }

    private fun descriptorFor(
        uid: Int,
        launchable: List<ApplicationInfo>,
        persisted: AppIdentityEntity?,
    ): AppDescriptor {
        SpecialUidClassifier.classify(uid)?.let { special ->
            return AppDescriptor(
                uid = uid,
                packageName = null,
                label = special.label,
                isSystem = special.kind == AppKind.SYSTEM,
                isRemoved = special.kind == AppKind.REMOVED,
                kind = special.kind,
            )
        }
        val directPackages = packageManager.getPackagesForUid(uid).orEmpty().toList()
        val visibleInfos = (launchable + directPackages.mapNotNull(::applicationInfo))
            .distinctBy { it.packageName }
        val labels = visibleInfos.map { packageManager.getApplicationLabel(it).toString() }
        val packageNames = visibleInfos.map { it.packageName }

        if (visibleInfos.isNotEmpty()) {
            val primaryLabel = labels.first()
            val label = if (labels.size > 1) "$primaryLabel + ${labels.size - 1}" else primaryLabel
            return AppDescriptor(
                uid = uid,
                packageName = packageNames.first(),
                label = label,
                packageNames = packageNames,
                isSystem = visibleInfos.all { it.flags and ApplicationInfo.FLAG_SYSTEM != 0 },
                kind = if (visibleInfos.all { it.flags and ApplicationInfo.FLAG_SYSTEM != 0 }) {
                    AppKind.SYSTEM
                } else {
                    AppKind.APPLICATION
                },
            )
        }

        if (persisted != null) {
            return AppDescriptor(
                uid = uid,
                packageName = persisted.packageName,
                label = "${persisted.label} (removido)",
                packageNames = listOfNotNull(persisted.packageName),
                isSystem = persisted.isSystem,
                isRemoved = true,
                kind = AppKind.REMOVED,
            )
        }

        return AppDescriptor(
            uid = uid,
            packageName = null,
            label = "Aplicativo não identificado (UID $uid)",
            kind = AppKind.UNKNOWN,
        )
    }

    @Suppress("DEPRECATION")
    private fun launchableApplications(): List<ApplicationInfo> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolved = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0))
        } else {
            packageManager.queryIntentActivities(intent, 0)
        }
        return resolved.map { it.activityInfo.applicationInfo }.distinctBy { it.packageName }
    }

    @Suppress("DEPRECATION")
    private fun applicationInfo(packageName: String): ApplicationInfo? = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0))
        } else {
            packageManager.getApplicationInfo(packageName, 0)
        }
    }.getOrNull()

    companion object {
        private const val CATALOG_CACHE_MILLIS = 10 * 60_000L
    }
}
