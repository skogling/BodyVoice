package com.liangji.bodymeasure.data.local.datastore

interface DeviceIdProvider {
    suspend fun getOrCreate(): String
}
