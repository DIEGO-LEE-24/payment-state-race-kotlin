package dev.diegolee.staterace

/** 읽기와 쓰기 사이에 끼워 넣는 지점. 시험에서 경합 창을 벌리는 데 쓴다. 운영 코드에서는 아무 일도 하지 않는다. */
fun interface RaceWindow {
    fun betweenReadAndWrite()

    companion object {
        val NONE = RaceWindow { }
    }
}

interface StateChangeService {
    fun confirm(paymentId: Long, item: String, window: RaceWindow = RaceWindow.NONE)
    fun cancel(paymentId: Long, item: String, window: RaceWindow = RaceWindow.NONE)
}
