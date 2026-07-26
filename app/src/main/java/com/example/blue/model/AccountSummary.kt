package com.example.blue.model

import com.example.blue.data.local.entity.AccountEntryEntity

data class AccountSummary(
    val incomeInCents: Long = 0L,
    val expenseInCents: Long = 0L,
) {
    val balanceInCents: Long get() = incomeInCents - expenseInCents
}

fun Iterable<AccountEntryEntity>.toAccountSummary(): AccountSummary = fold(AccountSummary()) { summary, entry ->
    when (entry.type) {
        AccountType.INCOME -> summary.copy(incomeInCents = summary.incomeInCents + entry.amountInCents)
        AccountType.EXPENSE -> summary.copy(expenseInCents = summary.expenseInCents + entry.amountInCents)
    }
}
