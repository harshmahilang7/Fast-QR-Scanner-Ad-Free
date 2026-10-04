package com.example.data.repository

import com.example.data.local.ScanItemDao
import com.example.data.local.ScanItemEntity
import com.example.domain.model.BarcodeType
import com.example.domain.model.ParsedBarcodeResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ScanHistoryRepository(private val dao: ScanItemDao) {

    val allHistory: Flow<List<ParsedBarcodeResult>> = dao.getAllHistory().map { entities ->
        entities.map { it.toDomain() }
    }

    val favoriteHistory: Flow<List<ParsedBarcodeResult>> = dao.getFavoriteHistory().map { entities ->
        entities.map { it.toDomain() }
    }

    val recentScans: Flow<List<ParsedBarcodeResult>> = dao.getRecentScans(10).map { entities ->
        entities.map { it.toDomain() }
    }

    fun getHistoryByType(type: BarcodeType): Flow<List<ParsedBarcodeResult>> {
        return dao.getHistoryByType(type.name).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    fun searchHistory(query: String): Flow<List<ParsedBarcodeResult>> {
        return dao.searchHistory(query.trim()).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    /**
     * Saves a scan into history.
     * If the identical code was scanned very recently (< 5 seconds ago), we update timestamp
     * instead of inserting a spam duplicate row.
     */
    suspend fun saveScan(scanResult: ParsedBarcodeResult): ParsedBarcodeResult {
        val existing = dao.getLatestByRawValue(scanResult.rawValue)
        val now = System.currentTimeMillis()
        if (existing != null && (now - existing.timestamp) < 5000) {
            dao.updateTimestamp(existing.id, now)
            return existing.copy(timestamp = now).toDomain()
        }
        val entity = ScanItemEntity.fromDomain(scanResult.copy(timestamp = now))
        val newId = dao.insert(entity)
        return scanResult.copy(id = newId, timestamp = now)
    }

    suspend fun toggleFavorite(id: Long, isFavorite: Boolean) {
        dao.updateFavorite(id, isFavorite)
    }

    suspend fun deleteById(id: Long) {
        dao.deleteById(id)
    }

    suspend fun clearAll() {
        dao.clearAll()
    }
}
