package com.haydra.autocare.data

/**
 * Identificadores y credenciales fijos usados por los repositorios Fake* para
 * precargar una cuenta y algunos registros de ejemplo, de modo que la app se
 * pueda explorar de inmediato sin tener que llenar cada formulario a mano.
 * Los usan los repositorios Fake* (pruebas) y DemoSeedCallback, que siembra la
 * cuenta demo en la base Room la primera vez que se crea.
 */
object DemoData {
    const val USER_ID = "demo-user"
    const val CORREO = "demo@autocare.com"
    const val PASSWORD = "123456"
    const val NOMBRE = "Chuina"

    const val VEHICLE_1_ID = "demo-vehicle-1"
    const val VEHICLE_2_ID = "demo-vehicle-2"
}
