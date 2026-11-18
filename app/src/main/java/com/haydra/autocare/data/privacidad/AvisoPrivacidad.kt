package com.haydra.autocare.data.privacidad

/**
 * Aviso de privacidad de AutoCare, con los apartados que pide la Ley Federal de Protección de
 * Datos Personales en Posesión de los Particulares (2025): responsable, datos, finalidades,
 * transferencias, medidas de seguridad y derechos ARCO.
 *
 * Si el texto cambia hay que subir [VERSION]: es la que se guarda en el perfil de Firestore
 * cuando el usuario lo acepta al registrarse.
 */
object AvisoPrivacidad {

    const val VERSION = "2026-11-17"
    const val ACTUALIZADO = "17 de noviembre de 2026"

    data class Apartado(val titulo: String, val texto: String)

    val apartados = listOf(
        Apartado(
            "Responsable",
            "AutoCare es una aplicación hecha como proyecto escolar. Sus integrantes son los responsables " +
                "de tus datos personales y los tratan conforme a la Ley Federal de Protección de Datos " +
                "Personales en Posesión de los Particulares."
        ),
        Apartado(
            "Datos que pedimos",
            "Para tu cuenta: nombre, correo electrónico y contraseña. La contraseña la valida Firebase " +
                "Authentication y en el teléfono solo se guarda cifrada. Para el servicio: los vehículos, " +
                "mantenimientos, gastos, recordatorios y documentos que tú registras. Las fotos de los " +
                "comprobantes se quedan solo en tu teléfono. No pedimos datos sensibles, ubicación, " +
                "contactos ni datos de pago."
        ),
        Apartado(
            "Para qué los usamos",
            "Solo para que la app funcione: identificarte, guardar el historial de tus vehículos, " +
                "respaldarlo en la nube y avisarte de tus recordatorios. No usamos tus datos para " +
                "publicidad ni los vendemos."
        ),
        Apartado(
            "Con quién se comparten",
            "Tus datos se guardan en Firebase, de Google, que solo los almacena como proveedor de " +
                "servicios en la nube. Cuando buscas un vehículo en el catálogo vPIC de la NHTSA solo se " +
                "envía el número de serie o la marca y el año, nunca tu nombre ni tu correo. No hacemos " +
                "otras transferencias."
        ),
        Apartado(
            "Cómo los protegemos",
            "Las conexiones van cifradas por HTTPS, las reglas de la base de datos solo dejan a cada " +
                "cuenta leer y escribir sus propios datos, la contraseña se guarda cifrada en el teléfono " +
                "y la app no hace copias automáticas en otros servicios."
        ),
        Apartado(
            "Tus derechos ARCO",
            "Puedes consultar y corregir tus datos desde la app, en Mi perfil y en cada registro. Para " +
                "cancelarlos usa Borrar mi cuenta en Mi perfil: se eliminan tu cuenta y todos tus datos " +
                "del teléfono y de la nube. Si te opones a que los usemos, borra tu cuenta y deja de usar " +
                "la app."
        ),
        Apartado(
            "Cuánto tiempo los guardamos",
            "Mientras tengas tu cuenta. Al borrarla, los datos se eliminan en ese momento de Firebase y " +
                "del teléfono."
        ),
        Apartado(
            "Cambios a este aviso",
            "Si el aviso cambia, la versión nueva aparecerá en esta pantalla con su fecha."
        )
    )
}
