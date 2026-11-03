package com.liangji.bodymeasure.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.liangji.bodymeasure.core.common.AppClock
import com.liangji.bodymeasure.domain.model.ThemeMode
import com.liangji.bodymeasure.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class AppViewModel @Inject constructor(
    settingsRepository: SettingsRepository,
    clock: AppClock,
) : ViewModel() {
    val themeMode = settingsRepository.themeMode.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ThemeMode.SYSTEM,
    )

    val today: LocalDate = clock.today()
}
