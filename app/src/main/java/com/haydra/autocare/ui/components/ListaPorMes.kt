package com.haydra.autocare.ui.components

import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.haydra.autocare.ui.common.formatMesAnio
import com.haydra.autocare.ui.common.formatMoneda
import java.time.LocalDate
import java.time.YearMonth

/** Registros agrupados por mes, del más reciente al más antiguo. */
fun <T> agruparPorMes(elementos: List<T>, fecha: (T) -> LocalDate): List<Pair<YearMonth, List<T>>> =
    elementos
        .sortedByDescending(fecha)
        .groupBy { YearMonth.from(fecha(it)) }
        .toList()

/**
 * Pinta [grupos] como secciones "Mes año" con sus filas unidas en un bloque.
 * El total del mes solo aparece si [conTotales] y el mes tiene más de un
 * registro (con uno sería el mismo monto de la fila).
 */
fun <T> LazyListScope.seccionesPorMes(
    grupos: List<Pair<YearMonth, List<T>>>,
    clave: (T) -> Any,
    monto: (T) -> Double,
    conTotales: Boolean = true,
    fila: @Composable LazyItemScope.(elemento: T, posicion: PosicionEnGrupo) -> Unit
) {
    grupos.forEach { (mes, elementos) ->
        item(key = "mes-$mes") {
            EncabezadoGrupo(
                titulo = formatMesAnio(mes.atDay(1)),
                total = if (conTotales && elementos.size > 1) formatMoneda(elementos.sumOf(monto)) else null,
                modifier = Modifier.animateItem()
            )
        }
        itemsIndexed(elementos, key = { _, elemento -> clave(elemento) }) { indice, elemento ->
            fila(elemento, PosicionEnGrupo.de(indice, elementos.size))
        }
    }
}
