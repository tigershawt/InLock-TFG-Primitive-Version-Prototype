package com.jetbrains.kmpapp.mvi

import kotlinx.coroutines.flow.Flow

interface Repository<Request, Response> {
    suspend fun execute(request: Request): Flow<Response>
}