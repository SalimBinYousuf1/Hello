package com.iamhere.service

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.iamhere.data.network.MeshEngine
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val meshEngine: MeshEngine
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        meshEngine.startDiscoveryAndAdvertising()
        return Result.success()
    }
}
