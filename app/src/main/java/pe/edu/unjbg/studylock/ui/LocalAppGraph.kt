package pe.edu.unjbg.studylock.ui

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import pe.edu.unjbg.studylock.di.AppGraph

val LocalAppGraph = staticCompositionLocalOf<AppGraph> { error("AppGraph no disponible") }

inline fun <reified VM : ViewModel> viewModelFactory(crossinline create: () -> VM): ViewModelProvider.Factory =
    object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(VM::class.java)) {
                "No se puede crear ${modelClass.name} desde ${VM::class.java.name}"
            }
            @Suppress("UNCHECKED_CAST")
            return create() as T
        }
    }
