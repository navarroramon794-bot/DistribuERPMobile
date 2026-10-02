package com.distribuerp.mobile.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object MigracionesRoom {

    val MIGRACION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `outbox` (
                    `uuid` TEXT NOT NULL,
                    `tipo` TEXT NOT NULL,
                    `payload` TEXT NOT NULL,
                    `estado` TEXT NOT NULL,
                    `intentos` INTEGER NOT NULL,
                    `ultimo_error` TEXT,
                    `creado_en` INTEGER NOT NULL,
                    `enviar_despues` INTEGER NOT NULL,
                    PRIMARY KEY(`uuid`)
                )
                """.trimIndent()
            )
        }
    }

    val MIGRACION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `clientes` (
                    `empresa_id` INTEGER NOT NULL,
                    `id` INTEGER NOT NULL,
                    `nombre` TEXT NOT NULL,
                    `direccion` TEXT,
                    `telefono` TEXT,
                    `limite_credito` REAL NOT NULL,
                    `activo` INTEGER NOT NULL,
                    `fecha_creacion` TEXT,
                    `credito_autorizado` INTEGER NOT NULL,
                    `dias_credito` INTEGER NOT NULL,
                    `bloqueado` INTEGER NOT NULL,
                    PRIMARY KEY(`empresa_id`, `id`)
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_clientes_empresa_id_nombre` ON `clientes` (`empresa_id`, `nombre`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_clientes_empresa_id_id` ON `clientes` (`empresa_id`, `id`)")

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `productos` (
                    `empresa_id` INTEGER NOT NULL,
                    `id` INTEGER NOT NULL,
                    `codigo` TEXT NOT NULL,
                    `nombre` TEXT NOT NULL,
                    `descripcion` TEXT,
                    `costo` REAL NOT NULL,
                    `precio` REAL NOT NULL,
                    `existencia` REAL NOT NULL,
                    `codigo_barras` TEXT,
                    `tipo_codigo` TEXT,
                    `unidad_venta` TEXT NOT NULL,
                    `activo` INTEGER NOT NULL,
                    `fecha_creacion` TEXT,
                    PRIMARY KEY(`empresa_id`, `id`)
                )
                """.trimIndent()
            )
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_productos_empresa_id_codigo` ON `productos` (`empresa_id`, `codigo`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_productos_empresa_id_codigo_barras` ON `productos` (`empresa_id`, `codigo_barras`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_productos_empresa_id_nombre` ON `productos` (`empresa_id`, `nombre`)")
        }
    }

    val TODAS: Array<Migration> = arrayOf(MIGRACION_1_2, MIGRACION_2_3)
}