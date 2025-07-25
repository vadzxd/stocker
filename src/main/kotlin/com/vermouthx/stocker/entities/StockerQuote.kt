package com.vermouthx.stocker.entities

data class StockerQuote(
    var code: String,
    var name: String,
    var current: Double,
    var opening: Double,
    var close: Double,
    var low: Double,
    var high: Double,
    var change: Double,
    var percentage: Double,
    var buys: Array<Double> = emptyArray(),
    var sells: Array<Double> = emptyArray(),
    var updateAt: String,
    var shares: Double = 0.0,  // 持有份额
    var costBasis: Double = 0.0 // 持有成本
) {
    // 计算当日盈亏
    fun getDailyProfit(): Double {
        if (shares <= 0) return 0.0
        return (current - close) * shares
    }
    
    // 计算总盈亏
    fun getTotalProfit(): Double {
        if (shares <= 0 || costBasis <= 0) return 0.0
        return (current * shares) - (costBasis * shares)
    }
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as StockerQuote

        return code == other.code
    }

    override fun hashCode(): Int {
        return code.hashCode()
    }
}
