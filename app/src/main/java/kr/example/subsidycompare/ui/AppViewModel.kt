package kr.example.subsidycompare.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kr.example.subsidycompare.data.Carrier
import kr.example.subsidycompare.data.Device
import kr.example.subsidycompare.data.Plan
import kr.example.subsidycompare.data.SubsidyData
import kr.example.subsidycompare.data.SubsidyRepository
import kr.example.subsidycompare.domain.Comparison
import kr.example.subsidycompare.domain.CostCalculator

sealed interface Screen {
    data object DeviceList : Screen
    data class DeviceDetail(val deviceId: String) : Screen
    data object Info : Screen
}

data class UiState(
    val data: SubsidyData? = null,
    val refreshing: Boolean = false,
    val message: String? = null,
    val screen: Screen = Screen.DeviceList,
    val query: String = "",
    /** null 이면 전체 통신사 */
    val carrierFilter: Carrier? = null,
    /** 사용자가 입력한 유통점 추가지원금(원) */
    val extraSupport: Int = 0,
)

/** 기종 상세 화면의 요금제 한 줄 */
data class PlanRow(
    val plan: Plan,
    val carrier: Carrier?,
    val subsidyAmount: Int,
    val comparison: Comparison,
)

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = SubsidyRepository(app)
    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val local = runCatching { repo.loadLocal() }.getOrNull()
            _state.update { it.copy(data = local) }
            refresh(silent = true)
        }
    }

    fun refresh(silent: Boolean = false) {
        if (!repo.isRemoteConfigured) {
            if (!silent) _state.update { it.copy(message = "아직 샘플 데이터 모드입니다 (데이터 주소 미설정)") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(refreshing = true) }
            val result = repo.refresh()
            _state.update { s ->
                result.fold(
                    onSuccess = { s.copy(data = it, refreshing = false, message = if (silent) null else "최신 데이터로 갱신했어요") },
                    onFailure = { s.copy(refreshing = false, message = "갱신 실패: 저장된 데이터를 보여드려요") },
                )
            }
        }
    }

    fun consumeMessage() = _state.update { it.copy(message = null) }
    fun setQuery(q: String) = _state.update { it.copy(query = q) }
    fun setCarrierFilter(c: Carrier?) = _state.update { it.copy(carrierFilter = c) }
    fun setExtraSupport(won: Int) = _state.update { it.copy(extraSupport = won.coerceIn(0, 5_000_000)) }

    fun open(screen: Screen) = _state.update {
        // 다른 기종으로 들어가면 필터·추가지원금 초기화
        if (screen is Screen.DeviceDetail) it.copy(screen = screen, carrierFilter = null, extraSupport = 0)
        else it.copy(screen = screen)
    }

    fun back(): Boolean {
        if (_state.value.screen == Screen.DeviceList) return false
        _state.update { it.copy(screen = Screen.DeviceList) }
        return true
    }

    // ---- 화면용 계산 ----

    fun filteredDevices(s: UiState): List<Device> {
        val devices = s.data?.devices.orEmpty()
        val q = s.query.trim()
        if (q.isEmpty()) return devices
        return devices.filter {
            it.name.contains(q, ignoreCase = true) || it.maker.contains(q, ignoreCase = true)
        }
    }

    /** 기종별 최대 지원금과 그 통신사 */
    fun maxSubsidy(s: UiState, device: Device): Pair<Int, Carrier?>? {
        val plans = s.data?.plans.orEmpty().associateBy { it.id }
        val best = device.subsidies.maxByOrNull { it.amount } ?: return null
        return best.amount to plans[best.planId]?.let { Carrier.from(it.carrier) }
    }

    /** 선택한 기종의 요금제별 비교 결과 — 24개월 실부담 총액이 낮은 순 */
    fun planRows(s: UiState, device: Device): List<PlanRow> {
        val plans = s.data?.plans.orEmpty().associateBy { it.id }
        return device.subsidies.mapNotNull { sub ->
            val plan = plans[sub.planId] ?: return@mapNotNull null
            val carrier = Carrier.from(plan.carrier)
            if (s.carrierFilter != null && carrier != s.carrierFilter) return@mapNotNull null
            PlanRow(
                plan = plan,
                carrier = carrier,
                subsidyAmount = sub.amount,
                comparison = CostCalculator.compare(
                    releasePrice = device.releasePrice,
                    monthlyFee = plan.monthlyFee,
                    subsidy = sub.amount,
                    extraSupport = s.extraSupport,
                ),
            )
        }.sortedBy { it.comparison.bestTotal }
    }
}
