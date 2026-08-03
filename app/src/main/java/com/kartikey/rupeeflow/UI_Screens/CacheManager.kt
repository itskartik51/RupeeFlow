package com.kartikey.rupeeflow.UI_Screens

import android.content.Context
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.kartikey.rupeeflow.UI_Screens.Add.TransactionModel
import com.kartikey.rupeeflow.UI_Screens.Assets.InvestmentItem
import com.kartikey.rupeeflow.UI_Screens.Assets.BankAccountItem
import com.kartikey.rupeeflow.UI_Screens.Assets.Finance.CashItem
import com.kartikey.rupeeflow.UI_Screens.Assets.Finance.CreditCardItem
import com.kartikey.rupeeflow.UI_Screens.Assets.Finance.FDItem
import com.kartikey.rupeeflow.UI_Screens.Home.ContriRoomModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class AppData(
    val userFullName: String,
    val userEmail: String,
    val userMobile: String,
    val userPassword: String,
    val userDob: String,
    val thisMonthExpenses: Double,
    val thisYearExpenses: Double,
    val budgetLimit: Double,
    val transactionList: List<TransactionModel>,
    val investmentList: List<InvestmentItem>,
    val bankList: List<BankAccountItem>,
    val cashData: CashItem,
    val fdList: List<FDItem>,
    val ccList: List<CreditCardItem>,
    val contriRoomsList: List<ContriRoomModel>
)

object CacheManager {
    private const val PREFS_NAME = "RupeeFlow_GlobalCache"
    
    fun getCachedData(context: Context, username: String): AppData? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val cachedJson = prefs.getString("data_$username", null) ?: return null
        return try {
            parseJsonToAppData(cachedJson)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun fetchAndCacheData(context: Context, username: String, forceRefresh: Boolean = false): AppData? {
        return withContext(Dispatchers.IO) {
            try {
                val db = FirebaseFirestore.getInstance()

                // 1. Fetch User Profile Document by Username
                val userQuery = db.collection("Users")
                    .whereEqualTo("username", username)
                    .get()
                    .await()

                if (userQuery.isEmpty) return@withContext null

                val userDoc = userQuery.documents[0]
                val userRef = userDoc.reference

                // Profile Fields
                val profileObj = JSONObject().apply {
                    put("name", userDoc.getString("name") ?: "")
                    put("email", userDoc.getString("email") ?: "")
                    put("mobile", userDoc.getString("mobile_no_") ?: userDoc.getString("mobile") ?: "")
                    put("password", userDoc.getString("password") ?: "")
                    
                    val rawDob = userDoc.get("dob")
                    val formattedDob = when (rawDob) {
                        is Timestamp -> SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(rawDob.toDate())
                        is String -> rawDob
                        else -> ""
                    }
                    put("dob", formattedDob)
                }

                val budgetLimit = userDoc.getDouble("budget_limit") ?: 0.0

                // 2. Fetch Expenses Sub-collection
                val expensesDocs = userRef.collection("Expenses").get().await()
                val expensesArray = JSONArray()

                for (doc in expensesDocs) {
                    val expObj = JSONObject()
                    val rawDate = doc.get("date")
                    val dateStr = when (rawDate) {
                        is Timestamp -> SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(rawDate.toDate())
                        is String -> rawDate
                        else -> doc.id
                    }

                    expObj.put("date", dateStr)
                    expObj.put("amount", doc.getDouble("amount") ?: 0.0)
                    expObj.put("category", doc.getString("category") ?: "")
                    expObj.put("detail1", doc.getString("detail_1") ?: doc.getString("detail1") ?: "")
                    expObj.put("detail2", doc.getString("detail_2") ?: doc.getString("detail2") ?: "")
                    
                    val paymentDetail = doc.getString("payment_detail") ?: ""
                    val splitPayment = paymentDetail.split("|").map { it.trim() }
                    
                    expObj.put("mode", if (splitPayment.isNotEmpty()) splitPayment[0] else "")
                    expObj.put("source_type", if (splitPayment.size > 1) splitPayment[1] else "")
                    expObj.put("source_id", if (splitPayment.size > 2) splitPayment[2] else "")

                    expensesArray.put(expObj)
                }

                // 3. Fetch Finances Sub-collection
                val financesDocs = userRef.collection("Finances").get().await()
                var cashObj = JSONObject().apply {
                    put("amount", 0.0)
                    put("last_updated", "")
                }
                val banksArray = JSONArray()
                val fdArray = JSONArray()
                val ccArray = JSONArray()

                for (doc in financesDocs) {
                    when (doc.id) {
                        "Cash" -> {
                            cashObj.put("amount", doc.getDouble("total_cash") ?: doc.getDouble("amount") ?: 0.0)
                            cashObj.put("last_updated", doc.getString("last_updated") ?: "")
                        }
                        "Banking_Data" -> {
                            val dataMap = doc.data
                            dataMap.forEach { (accNo, rawBank) ->
                                if (rawBank is Map<*, *>) {
                                    val bName = rawBank["bank_name"]?.toString() ?: ""
                                    val curBal = (rawBank["current_bal"] as? Number)?.toDouble() ?: 0.0
                                    val rate = (rawBank["interest_rate"] as? Number)?.toDouble() ?: 0.0
                                    
                                    val qtrPct = rate / 4.0
                                    val expQtr = curBal * (qtrPct / 100.0)
                                    val expYr = curBal * (rate / 100.0)
                                    val oneDay = (curBal * (rate / 100.0)) / 365.0
                                    val accruedQtr = (rawBank["accrued_qtr_int"] as? Number)?.toDouble() ?: (expQtr / 2.0)
                                    val accruedYr = (rawBank["accrued_yr_int"] as? Number)?.toDouble() ?: (expYr / 2.0)

                                    val bObj = JSONObject().apply {
                                        put("bank_name", bName)
                                        put("account_no", accNo)
                                        put("current_bal", curBal)
                                        put("interest_rate", rate)
                                        put("qtr_interest_pct", qtrPct)
                                        put("exp_qtr_int", expQtr)
                                        put("accrued_qtr_int", accruedQtr)
                                        put("exp_yr_int", expYr)
                                        put("accrued_yr_int", accruedYr)
                                        put("one_day_int", oneDay)
                                    }
                                    banksArray.put(bObj)
                                }
                            }
                        }
                        "Fixed_Deposits" -> {
                            val dataMap = doc.data
                            dataMap.forEach { (accNo, rawFd) ->
                                if (rawFd is Map<*, *>) {
                                    val invAmt = (rawFd["invested_amt"] as? Number)?.toDouble() ?: 0.0
                                    val rate = (rawFd["interest_rate"] as? Number)?.toDouble() ?: 0.0
                                    val matVal = (rawFd["maturity_value"] as? Number)?.toDouble() ?: invAmt
                                    val accVal = (rawFd["accrued_value"] as? Number)?.toDouble() ?: invAmt
                                    val accInt = (rawFd["accrued_int"] as? Number)?.toDouble() ?: (accVal - invAmt)
                                    val oneDay = (invAmt * (rate / 100.0)) / 365.0

                                    val fdObj = JSONObject().apply {
                                        put("bank_name", rawFd["bank_name"]?.toString() ?: "")
                                        put("account_no", accNo)
                                        put("create_date", rawFd["create_date"]?.toString() ?: "")
                                        put("maturity_date", rawFd["maturity_date"]?.toString() ?: "")
                                        put("days_to_maturity", (rawFd["days_to_maturity"] as? Number)?.toInt() ?: 0)
                                        put("invested_amt", invAmt)
                                        put("interest_rate", rate)
                                        put("maturity_value", matVal)
                                        put("accrued_value", accVal)
                                        put("accrued_int", accInt)
                                        put("one_day_int", oneDay)
                                    }
                                    fdArray.put(fdObj)
                                }
                            }
                        }
                        "Credit_Cards" -> {
                            val dataMap = doc.data
                            dataMap.forEach { (cardNo, rawCc) ->
                                if (rawCc is Map<*, *>) {
                                    val limit = (rawCc["limit"] as? Number)?.toDouble() ?: 0.0
                                    val out = (rawCc["outstanding"] as? Number)?.toDouble() ?: 0.0
                                    val avail = (rawCc["available"] as? Number)?.toDouble() ?: (limit - out)
                                    val util = if (limit > 0) (out / limit) * 100.0 else 0.0

                                    val ccObj = JSONObject().apply {
                                        put("issuer", rawCc["issuer"]?.toString() ?: "")
                                        put("card_no", cardNo)
                                        put("type", rawCc["type"]?.toString() ?: "Visa")
                                        put("limit", limit)
                                        put("outstanding", out)
                                        put("available", avail)
                                        put("utilization", util)
                                        put("cibil_status", rawCc["cibil_status"]?.toString() ?: "Good")
                                        put("billing_day", (rawCc["billing_day"] as? Number)?.toInt() ?: 0)
                                        put("due_day", (rawCc["due_day"] as? Number)?.toInt() ?: 0)
                                        put("reminder_day", (rawCc["reminder_day"] as? Number)?.toInt() ?: 0)
                                        put("annual_fee", (rawCc["annual_fee"] as? Number)?.toDouble() ?: 0.0)
                                        put("joining_fee", (rawCc["joining_fee"] as? Number)?.toDouble() ?: 0.0)
                                        put("last_used", rawCc["last_used"]?.toString() ?: "")
                                    }
                                    ccArray.put(ccObj)
                                }
                            }
                        }
                    }
                }

                // 4. Fetch Investments Sub-collection
                val invDocs = userRef.collection("Investments").get().await()
                val invArray = JSONArray()
                for (doc in invDocs) {
                    val invObj = JSONObject().apply {
                        put("asset_name", doc.getString("asset_name") ?: doc.id)
                        put("asset_type", doc.getString("asset_type") ?: "Stock")
                        put("quantity", doc.getDouble("quantity") ?: 0.0)
                        put("buy_price", doc.getDouble("buy_price") ?: 0.0)
                        put("current_price", doc.getDouble("current_price") ?: doc.getDouble("buy_price") ?: 0.0)
                        put("one_day_change", doc.getDouble("one_day_change") ?: 0.0)
                    }
                    invArray.put(invObj)
                }

                // 5. Fetch Contri Rooms
                val contriArray = JSONArray()
                val contriDocs = db.collection("Contri")
                    .whereArrayContains("members", username)
                    .get()
                    .await()

                for (doc in contriDocs) {
                    val cObj = JSONObject().apply {
                        put("room_name", doc.getString("room_name") ?: "")
                        put("room_code", doc.id)
                        put("passkey", doc.getString("passkey") ?: "123456")
                        put("expenses", JSONArray())
                    }
                    contriArray.put(cObj)
                }

                // Construct Master Payload
                val masterJson = JSONObject().apply {
                    put("status", "success")
                    put("profile", profileObj)
                    put("budget_limit", budgetLimit)
                    put("expenses", expensesArray)
                    put("cash", cashObj)
                    put("banks", banksArray)
                    put("fds", fdArray)
                    put("credit_cards", ccArray)
                    put("investments", invArray)
                    put("contri_rooms", contriArray)
                }

                val responseData = masterJson.toString()

                // Local Cache Update
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                prefs.edit().putString("data_$username", responseData).apply()

                return@withContext parseJsonToAppData(responseData)

            } catch (e: Exception) {
                null
            }
        }
    }

    private fun parseJsonToAppData(responseData: String): AppData {
        val jsonResponse = JSONObject(responseData)
        
        val cal = Calendar.getInstance()
        val currM = cal.get(Calendar.MONTH) + 1
        val currY = cal.get(Calendar.YEAR)

        val profileObj = jsonResponse.optJSONObject("profile")
        val tempName = profileObj?.optString("name", "") ?: ""
        val tempEmail = profileObj?.optString("email", "") ?: ""
        val tempMobile = profileObj?.optString("mobile", "") ?: ""
        val tempPass = profileObj?.optString("password", "") ?: ""
        val tempDob = profileObj?.optString("dob", "") ?: ""

        val expensesArray = jsonResponse.optJSONArray("expenses")
        var tempTotal = 0.0
        var tempMonth = 0.0
        var tempYear = 0.0
        val tempHistory = mutableListOf<TransactionModel>()

        if (expensesArray != null && expensesArray.length() > 0) {
            val currMonthStr = String.format(Locale.US, "%02d", currM)
            val currYearStr = currY.toString()
            
            for (i in 0 until expensesArray.length()) {
                val item = expensesArray.getJSONObject(i)
                val rawDate = item.optString("date", "").trim()
                val rawAmt = item.optString("amount", "0")
                val amt = rawAmt.replace("[^\\d.]".toRegex(), "").toDoubleOrNull() ?: item.optDouble("amount", 0.0)
                
                if (amt > 0.0) {
                    tempTotal += amt 
                    tempHistory.add(
                        TransactionModel(
                            date = rawDate, 
                            amount = amt, 
                            category = item.optString("category", "Unknown"), 
                            remark1 = item.optString("detail1", ""), 
                            remark2 = item.optString("detail2", ""), 
                            mode = item.optString("mode", ""), 
                            sourceType = item.optString("source_type", ""), 
                            sourceId = item.optString("source_id", "")
                        )
                    )
                    
                    if (rawDate.contains(currYearStr)) {
                        tempYear += amt
                        if (rawDate.contains("-$currMonthStr-") || rawDate.contains("/$currMonthStr/") || rawDate.startsWith("$currMonthStr-") || rawDate.startsWith("$currMonthStr/")) {
                            tempMonth += amt
                        }
                    }
                }
            }
        }

        val invArray = jsonResponse.optJSONArray("investments")
        val fetchedInvList = mutableListOf<InvestmentItem>()
        if (invArray != null) {
            for (i in 0 until invArray.length()) { 
                val item = invArray.getJSONObject(i)
                fetchedInvList.add(
                    InvestmentItem(
                        assetName = item.optString("asset_name", ""),
                        assetType = item.optString("asset_type", "Stock"),
                        quantity = item.optDouble("quantity", 0.0), 
                        avgBuyPrice = item.optDouble("buy_price", 0.0), 
                        currentPrice = item.optDouble("current_price", item.optDouble("buy_price", 0.0)), 
                        oneDayChangePrice = item.optDouble("one_day_change", 0.0)
                    )
                ) 
            }
        }

        val banksArray = jsonResponse.optJSONArray("banks")
        val fetchedBankList = mutableListOf<BankAccountItem>()
        if (banksArray != null) {
            for (i in 0 until banksArray.length()) { 
                val item = banksArray.getJSONObject(i)
                fetchedBankList.add(
                    BankAccountItem(
                        bankName = item.optString("bank_name", ""), 
                        accountNo = item.optString("account_no", ""), 
                        currentBalance = item.optDouble("current_bal", 0.0), 
                        interestRate = item.optDouble("interest_rate", 0.0), 
                        qtrInterestPct = item.optDouble("qtr_interest_pct", 0.0), 
                        expQtrInt = item.optDouble("exp_qtr_int", 0.0), 
                        accruedQtrInt = item.optDouble("accrued_qtr_int", 0.0), 
                        expYrInt = item.optDouble("exp_yr_int", 0.0), 
                        accruedYrInt = item.optDouble("accrued_yr_int", 0.0), 
                        oneDayInt = item.optDouble("one_day_int", 0.0)
                    )
                ) 
            }
        }

        val cashObj = jsonResponse.optJSONObject("cash")
        val fetchedCash = if (cashObj != null) { 
            CashItem(amount = cashObj.optDouble("amount", 0.0), lastUpdated = cashObj.optString("last_updated", "")) 
        } else {
            CashItem(0.0, "")
        }

        val fdArray = jsonResponse.optJSONArray("fds")
        val fetchedFDList = mutableListOf<FDItem>()
        if (fdArray != null) {
            for (i in 0 until fdArray.length()) { 
                val item = fdArray.getJSONObject(i)
                fetchedFDList.add(
                    FDItem(
                        bankName = item.optString("bank_name", ""), 
                        accountNo = item.optString("account_no", ""), 
                        createDate = item.optString("create_date", ""), 
                        maturityDate = item.optString("maturity_date", ""), 
                        daysToMaturity = item.optInt("days_to_maturity", 0), 
                        investedAmt = item.optDouble("invested_amt", 0.0), 
                        interestRate = item.optDouble("interest_rate", 0.0), 
                        maturityValue = item.optDouble("maturity_value", 0.0), 
                        accruedValue = item.optDouble("accrued_value", 0.0), 
                        accruedInt = item.optDouble("accrued_int", 0.0), 
                        oneDayInt = item.optDouble("one_day_int", 0.0)
                    )
                ) 
            }
        }

        val ccArray = jsonResponse.optJSONArray("credit_cards")
        val fetchedCCList = mutableListOf<CreditCardItem>()
        if (ccArray != null) {
            for (i in 0 until ccArray.length()) { 
                val item = ccArray.getJSONObject(i)
                fetchedCCList.add(
                    CreditCardItem(
                        issuer = item.optString("issuer", ""), 
                        cardNo = item.optString("card_no", ""), 
                        type = item.optString("type", ""), 
                        limit = item.optDouble("limit", 0.0), 
                        outstanding = item.optDouble("outstanding", 0.0), 
                        available = item.optDouble("available", 0.0), 
                        utilization = item.optDouble("utilization", 0.0), 
                        cibilStatus = item.optString("cibil_status", ""), 
                        billingDay = item.optInt("billing_day", 0), 
                        dueDay = item.optInt("due_day", 0), 
                        reminderDay = item.optInt("reminder_day", 0), 
                        annualFee = item.optDouble("annual_fee", 0.0), 
                        joiningFee = item.optDouble("joining_fee", 0.0), 
                        lastUsed = item.optString("last_used", "")
                    )
                ) 
            }
        }

        val contriArray = jsonResponse.optJSONArray("contri_rooms")
        val fetchedContriRooms = mutableListOf<ContriRoomModel>()
        if (contriArray != null) {
            for (i in 0 until contriArray.length()) {
                val item = contriArray.getJSONObject(i)
                val rName = item.optString("room_name", "")
                val rCode = item.optString("room_code", "")
                val rPin = item.optString("passkey", "123456") 
                val expArray = item.optJSONArray("expenses")
                var lastDate = ""
                if (expArray != null && expArray.length() > 0) {
                    val lastExp = expArray.getJSONObject(0)
                    lastDate = lastExp.optString("date", "")
                }
                fetchedContriRooms.add(ContriRoomModel(rName, rCode, lastDate, rPin))
            }
        }

        val fetchedBudgetLimit = jsonResponse.optDouble("budget_limit", 0.0)
        
        return AppData(
            userFullName = tempName,
            userEmail = tempEmail,
            userMobile = tempMobile,
            userPassword = tempPass,
            userDob = tempDob,
            thisMonthExpenses = tempMonth,
            thisYearExpenses = tempYear,
            budgetLimit = fetchedBudgetLimit,
            transactionList = tempHistory.reversed(),
            investmentList = fetchedInvList,
            bankList = fetchedBankList,
            cashData = fetchedCash,
            fdList = fetchedFDList,
            ccList = fetchedCCList,
            contriRoomsList = fetchedContriRooms
        )
    }
}
