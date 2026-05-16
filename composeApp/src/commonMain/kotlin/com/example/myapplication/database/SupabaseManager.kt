package com.example.myapplication.database

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.realtime.Realtime
import io.ktor.client.plugins.HttpTimeout
import io.github.jan.supabase.annotations.SupabaseInternal

object SupabaseManager {
    @OptIn(SupabaseInternal::class)
    val client: SupabaseClient = createSupabaseClient(
        supabaseUrl = "https://uzkwgtwrymjjisdirpolw.supabase.co",
        supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InV6a3dndHdyeW1qaXNkaXJwb2x3Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzgzMjUzNDMsImV4cCI6MjA5MzkwMTM0M30.7CjgC5fxmwdVIIuiRbFkOmFkOUtPij1ch6InK0ZEg9M"
    ) {
        install(Postgrest)
        install(Auth)
        install(Realtime)
        
        httpConfig {
            install(HttpTimeout) {
                requestTimeoutMillis = 30000
                connectTimeoutMillis = 30000
                socketTimeoutMillis = 30000
            }
        }
    }
}
