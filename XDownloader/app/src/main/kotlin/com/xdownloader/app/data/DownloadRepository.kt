package com.xdownloader.app.data

import com.xdownloader.app.data.db.DownloadDao
import com.xdownloader.app.data.db.toDomain
import com.xdownloader.app.data.db.toEntity
import com.xdownloader.app.domain.model.DownloadJob
import com.xdownloader.app.domain.model.DownloadStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadRepository @Inject constructor(
    private val dao: DownloadDao
) {
    fun observeAll(): Flow<List<DownloadJob>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun insert(job: DownloadJob): Long = dao.insert(job.toEntity())

    suspend fun updateStatus(id: Long, status: DownloadStatus) =
        dao.updateStatus(id, status.name)

    suspend fun updateStatusAndDmId(id: Long, status: DownloadStatus, dmId: Long) =
        dao.updateStatusAndDmId(id, status.name, dmId)

    suspend fun updateStatusWithError(id: Long, status: DownloadStatus, error: String) =
        dao.updateStatusWithError(id, status.name, error)

    suspend fun deleteById(id: Long) = dao.deleteById(id)

    suspend fun findByDownloadManagerId(dmId: Long): DownloadJob? =
        dao.findByDownloadManagerId(dmId)?.toDomain()

    suspend fun findById(id: Long): DownloadJob? =
        dao.findById(id)?.toDomain()
}
