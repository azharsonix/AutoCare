package com.haydra.autocare

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * Sustituye Dispatchers.Main por un dispatcher de pruebas mientras corre cada
 * test, necesario porque viewModelScope usa Dispatchers.Main.immediate y ese
 * dispatcher no existe en un entorno JVM sin Android. Los tests deben usar
 * `runTest(mainDispatcherRule.testDispatcher) { ... }` para que el reloj
 * virtual de las corrutinas del ViewModel y el del test sean el mismo.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
