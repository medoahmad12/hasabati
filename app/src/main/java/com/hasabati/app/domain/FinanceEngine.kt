package com.hasabati.app.domain

import com.hasabati.app.data.db.entities.Currency
import com.hasabati.app.data.db.entities.Order
import com.hasabati.app.data.db.entities.OrderStatus
import com.hasabati.app.data.db.entities.PaymentMethod
import com.hasabati.app.data.db.entities.Transaction
import com.hasabati.app.data.db.entities.TransactionType

/**
 * محرك الحسابات المالية — كل الأرصدة في التطبيق تُشتق هنا من سجل العمليات
 * [Transaction] ومن [Order]، ولا تُخزَّن كأرقام مستقلة، تطبيقاً للقاعدة الأساسية:
 * "لا تقم بعمل حسابات شكلية أو أرقام ثابتة" (القسم 2).
 */
object FinanceEngine {

    /** أثر العملية على الخزينة: +1 تزيد / -1 تنقص / null لا تؤثر على الخزينة إطلاقاً */
    private fun treasurySign(type: TransactionType): Int = when (type) {
        TransactionType.CUSTOMER_PAYMENT -> 1
        TransactionType.CAPITAL_ADDITION -> 1
        TransactionType.CAPITAL_INITIAL -> 1
        TransactionType.AGENT_PAYMENT -> -1
        TransactionType.EXPENSE -> -1
        TransactionType.PERSONAL_WITHDRAWAL -> -1
        TransactionType.REFUND -> -1
        TransactionType.ADJUSTMENT -> 1 // amount نفسه قد يكون موجباً أو سالباً في حالة التصحيح
    }

    data class TreasurySnapshot(
        val cashUsd: Double = 0.0,
        val cashSyp: Double = 0.0,
        val shamCashUsd: Double = 0.0,
        val shamCashSyp: Double = 0.0
    )

    fun treasury(transactions: List<Transaction>): TreasurySnapshot {
        var cashUsd = 0.0; var cashSyp = 0.0; var shamUsd = 0.0; var shamSyp = 0.0
        for (t in transactions) {
            val signed = t.amount * treasurySign(t.type)
            when (t.method to t.currency) {
                PaymentMethod.CASH to Currency.USD -> cashUsd += signed
                PaymentMethod.CASH to Currency.SYP -> cashSyp += signed
                PaymentMethod.SHAM_CASH to Currency.USD -> shamUsd += signed
                PaymentMethod.SHAM_CASH to Currency.SYP -> shamSyp += signed
            }
        }
        return TreasurySnapshot(cashUsd, cashSyp, shamUsd, shamSyp)
    }

    /** صافي ما دفعته عميلة معيّنة بالدولار المحاسبي (دفعات ناقص أي استرداد) */
    fun customerNetPaidUsd(transactions: List<Transaction>, customerId: Long): Double {
        return transactions.filter { it.customerId == customerId }.sumOf {
            when (it.type) {
                TransactionType.CUSTOMER_PAYMENT -> it.usdEquivalent
                TransactionType.REFUND -> -it.usdEquivalent
                else -> 0.0
            }
        }
    }

    /** المتبقي على عميلة عبر كل طلباتها غير الملغاة */
    fun customerRemainingUsd(orders: List<Order>, transactions: List<Transaction>, customerId: Long): Double {
        val salesTotal = orders.filter { it.customerId == customerId && it.status != OrderStatus.CANCELLED }
            .sumOf { it.saleTotalUsd }
        return salesTotal - customerNetPaidUsd(transactions, customerId)
    }

    /** إجمالي مستحقات كل العملاء مجتمعين (لي عند العملاء) */
    fun totalCustomerReceivablesUsd(orders: List<Order>, transactions: List<Transaction>): Double {
        val byCustomer = orders.filter { it.status != OrderStatus.CANCELLED }.groupBy { it.customerId }
        return byCustomer.entries.sumOf { (customerId, custOrders) ->
            val sales = custOrders.sumOf { it.saleTotalUsd }
            val paid = customerNetPaidUsd(transactions, customerId)
            (sales - paid).coerceAtLeast(0.0)
        }
    }

    /**
     * المستحق للوكيلة: يُحسب فقط من الطلبات التي وصلت وتم تأكيد تكلفتها الفعلية
     * (القاعدة 9 و10: لا دين للوكيلة لمجرد إنشاء الطلب).
     */
    fun agentPayableUsd(orders: List<Order>, transactions: List<Transaction>): Double {
        val totalActualCost = orders.filter { it.actualCostUsd != null && it.status != OrderStatus.CANCELLED }
            .sumOf { it.actualCostUsd ?: 0.0 }
        val totalPaidToAgent = transactions.filter { it.type == TransactionType.AGENT_PAYMENT }
            .sumOf { it.usdEquivalent }
        return (totalActualCost - totalPaidToAgent)
    }

    data class CapitalSnapshot(
        val initial: Double,
        val additions: Double,
        val withdrawals: Double
    ) {
        val netCapital: Double get() = initial + additions - withdrawals
    }

    fun capital(transactions: List<Transaction>): CapitalSnapshot {
        val initial = transactions.filter { it.type == TransactionType.CAPITAL_INITIAL }.sumOf { it.usdEquivalent }
        val additions = transactions.filter { it.type == TransactionType.CAPITAL_ADDITION }.sumOf { it.usdEquivalent }
        val withdrawals = transactions.filter { it.type == TransactionType.PERSONAL_WITHDRAWAL }.sumOf { it.usdEquivalent }
        return CapitalSnapshot(initial, additions, withdrawals)
    }

    fun totalExpensesUsd(transactions: List<Transaction>): Double =
        transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.usdEquivalent }

    /** الربح المتوقع: للطلبات التي لم تصل بعد (لا تكلفة فعلية) */
    fun expectedProfitUsd(orders: List<Order>): Double =
        orders.filter { it.actualCostUsd == null && it.status != OrderStatus.CANCELLED }
            .sumOf { it.saleTotalUsd - it.expectedCostUsd }

    /** الربح الفعلي/المحقق: للطلبات التي أُكدت تكلفتها الفعلية (وصلت) */
    fun realizedProfitUsd(orders: List<Order>): Double =
        orders.filter { it.actualCostUsd != null && it.status != OrderStatus.CANCELLED }
            .sumOf { it.saleTotalUsd - (it.actualCostUsd ?: 0.0) }

    fun generateNextOrderNumber(maxSequence: Int?): String {
        val next = (maxSequence ?: 1000) + 1
        return "ORD-$next"
    }

    /** يحول مبلغاً بالليرة السورية إلى قيمته بالدولار حسب سعر صرف مُعطى */
    fun sypToUsd(amountSyp: Double, exchangeRate: Double): Double =
        if (exchangeRate <= 0) 0.0 else amountSyp / exchangeRate
}
