package com.kartikey.rupeeflow.UI_Screens

import android.content.Context
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
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

                val userQuery = db.collection("Users")
                    .whereEqualTo("username", username)
                    .get()
                    .await()

                if (userQuery.isEmpty) return@withContext null

                val userDoc = userQuery.documents[0]
                val userRef = userDoc.reference

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

                val expensesDocs = userRef.collection("Expenses").get().await()
                val expensesArray = JSONArray()

                for (doc in expensesDocs) {
                    val dataMap = doc.data
                    
                    for ((key, value) in dataMap) {
                        if (value is Map<*, *>) {
                            val expObj = JSONObject()
                            val rawDate = value["date"]
                            val dateStr = when (rawDate) {
                                is Timestamp -> SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(rawDate.toDate())
                                is String -> rawDate.toString()
                                else -> ""
                            }

                            expObj.put("date", dateStr)
                            expObj.put("amount", (value["amount"] as? Number)?.toDouble() ?: 0.0)
                            expObj.put("category", value["category"]?.toString() ?: "")
                            expObj.put("detail1", value["detail_1"]?.toString() ?: value["detail1"]?.toString() ?: "")
                            expObj.put("detail2", value["detail_2"]?.toString() ?: value["detail2"]?.toString() ?: "")
                            
                            val paymentDetail = value["payment_detail"]?.toString() ?: ""
                            val splitPayment = paymentDetail.split("|").map { it.trim() }
                            
                            expObj.put("mode", if (splitPayment.isNotEmpty()) splitPayment[0] else "")
                            expObj.put("source_type", if (splitPayment.size > 1) splitPayment[1] else "")
                            expObj.put("source_id", if (splitPayment.size > 2) splitPayment[2] else "")

                            expensesArray.put(expObj)
                        }
                    }
                }

                val financesDocs = userRef.collection("Finances").get().await()
                
                // Initialize Cash with defaults
                var cashObj = JSONObject().apply {
                    put("amount", 0.0)
                    put("last_updated", "")
                }
                
                val banksArray = JSONArray()
                val fdArray = JSONArray()
                val ccArray = JSONArray()
                
                val todayMillis = System.currentTimeMillis()

                for (doc in financesDocs) {
                    when (doc.id) {
                        // OLD "Cash" DOCUMENT BLOCK IS COMPLETELY REMOVED

                        "Bank" -> {
                            val dataMap = doc.data
                            val updateMap = mutableMapOf<String, Any>()
                            
                            val lastUpdatedTs = dataMap["last_updated"] as? Timestamp
                            val calToday = Calendar.getInstance().apply {
                                set(Calendar.HOUR_OF_DAY, 0)
                                set(Calendar.MINUTE, 0)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }
                            
                            var yearShifted = false
                            var gapDays = 0L
                            
                            if (lastUpdatedTs != null) {
                                val calLast = Calendar.getInstance().apply {
                                    time = lastUpdatedTs.toDate()
                                    set(Calendar.HOUR_OF_DAY, 0)
                                    set(Calendar.MINUTE, 0)
                                    set(Calendar.SECOND, 0)
                                    set(Calendar.MILLISECOND, 0)
                                }
                                if (calToday.get(Calendar.YEAR) > calLast.get(Calendar.YEAR)) {
                                    yearShifted = true
                                }
                                if (calToday.timeInMillis > calLast.timeInMillis) {
                                    gapDays = (calToday.timeInMillis - calLast.timeInMillis) / (1000 * 60 * 60 * 24)
                                }
                            }

                            if (gapDays > 0) {
                                updateMap["last_updated"] = FieldValue.serverTimestamp()
                            }

                            val startOfQtr = Calendar.getInstance().apply {
                                set(Calendar.MONTH, (calToday.get(Calendar.MONTH) / 3) * 3)
                                set(Calendar.DAY_OF_MONTH, 1)
                                set(Calendar.HOUR_OF_DAY, 0)
                                set(Calendar.MINUTE, 0)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }
                            val daysPassedQtr = ((calToday.timeInMillis - startOfQtr.timeInMillis) / (1000 * 60 * 60 * 24)).toInt() + 1
                            
                            val startOfYear = Calendar.getInstance().apply {
                                set(Calendar.MONTH, Calendar.JANUARY)
                                set(Calendar.DAY_OF_MONTH, 1)
                                set(Calendar.HOUR_OF_DAY, 0)
                                set(Calendar.MINUTE, 0)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }
                            val daysPassedYr = ((calToday.timeInMillis - startOfYear.timeInMillis) / (1000 * 60 * 60 * 24)).toInt() + 1

                            val currentMonth = calToday.get(Calendar.MONTH)
                            val qtrStr = "q${(currentMonth / 3) + 1}"

                            dataMap.forEach { (key, rawData) ->
                                // ==========================================
                                // NEW CASH LOGIC INSIDE BANK DOCUMENT
                                // ==========================================
                                if (key == "cash" && rawData is Map<*, *>) {
                                    val amnt = (rawData["amnt"] as? Number)?.toDouble() ?: 0.0
                                    val lastUpd = rawData["last update"]
                                    val lastUpdStr = when (lastUpd) {
                                        is Timestamp -> SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(lastUpd.toDate())
                                        is String -> lastUpd
                                        else -> ""
                                    }
                                    cashObj.put("amount", amnt)
                                    cashObj.put("last_updated", lastUpdStr)
                                } 
                                // ==========================================
                                // EXISTING BANK ACCOUNTS LOGIC
                                // ==========================================
                                else if (key != "last_updated" && key != "cash" && rawData is Map<*, *>) {
                                    val rawBank = rawData
                                    val bName = rawBank["bank"]?.toString() ?: ""
                                    val accNo = rawBank["account no."]?.toString() ?: ""
                                    val curBal = (rawBank["current bal."] as? Number)?.toDouble() ?: 0.0
                                    val rateYr = (rawBank["intrest % (yr)"] as? Number)?.toDouble() ?: 0.0
                                    val rateQtr = (rawBank["intrest % (qtr)"] as? Number)?.toDouble() ?: (rateYr / 4.0)
                                    val oneDayInt = (curBal * (rateYr / 100.0)) / 365.0

                                    val qtrAvgMap = rawBank["qtr. avg."] as? Map<*,*> ?: emptyMap<String, Any>()
                                    val yrAvgMap = rawBank["yr avg"] as? Map<*,*> ?: emptyMap<String, Any>()

                                    val currentQtrAvg = (qtrAvgMap[qtrStr] as? Number)?.toDouble() ?: curBal
                                    var curYrAvg = (yrAvgMap["cur"] as? Number)?.toDouble() ?: curBal

                                    if (yearShifted) {
                                        val lastYrAvg = (yrAvgMap["cur"] as? Number)?.toDouble() ?: 0.0
                                        val secondLastYrAvg = (yrAvgMap["last"] as? Number)?.toDouble() ?: 0.0
                                        
                                        updateMap["$key.yr avg.2nd last"] = secondLastYrAvg
                                        updateMap["$key.yr avg.last"] = lastYrAvg
                                        updateMap["$key.yr avg.cur"] = curBal
                                        curYrAvg = curBal 
                                    }

                                    val expQtr = currentQtrAvg * (rateQtr / 100.0)
                                    val accruedQtr = expQtr * (daysPassedQtr / 90.0)
                                    val expYr = curYrAvg * (rateYr / 100.0)
                                    val accruedYr = expYr * (daysPassedYr / 365.0)

                                    if (gapDays > 0 || yearShifted) {
                                        updateMap["$key.exp qtr int"] = expQtr
                                        updateMap["$key.accrued qtr"] = accruedQtr
                                        updateMap["$key.exp yr int"] = expYr
                                        updateMap["$key.accrued yr"] = accruedYr
                                        updateMap["$key.1d int"] = oneDayInt
                                    }

                                    val bObj = JSONObject().apply {
                                        put("bank_name", bName)
                                        put("account_no", accNo)
                                        put("current_bal", curBal)
                                        put("interest_rate", rateYr)
                                        put("qtr_interest_pct", rateQtr)
                                        put("exp_qtr_int", expQtr)
                                        put("accrued_qtr_int", accruedQtr)
                                        put("exp_yr_int", expYr)
                                        put("accrued_yr_int", accruedYr)
                                        put("one_day_int", oneDayInt)
                                    }
                                    banksArray.put(bObj)
                                }
                            }
                            if (updateMap.isNotEmpty()) {
                                doc.reference.update(updateMap).await()
                            }
                        }

                        "CC FD" -> {
                            val dataMap = doc.data
                            
                            val ccMap = dataMap["CC"] as? Map<*, *>
                            ccMap?.forEach { (key, rawCc) ->
                                if (rawCc is Map<*, *>) {
                                    val issuer = rawCc["issuer"]?.toString() ?: ""
                                    val cardNo = rawCc["card no."]?.toString() ?: ""
                                    val type = rawCc["type"]?.toString() ?: ""
                                    val limit = (rawCc["limit"] as? Number)?.toDouble() ?: 0.0
                                    val outstanding = (rawCc["outstanding"] as? Number)?.toDouble() ?: 0.0
                                    
                                    val avail = limit - outstanding
                                    val util = if (limit > 0) (outstanding / limit) * 100.0 else 0.0
                                    val cibilStatus = if (util <= 30.0) "Safe" else "High Risk"

                                    val ccObj = JSONObject().apply {
                                        put("issuer", issuer)
                                        put("card_no", cardNo)
                                        put("type", type)
                                        put("limit", limit)
                                        put("outstanding", outstanding)
                                        put("available", avail)
                                        put("utilization", util)
                                        put("cibil_status", cibilStatus)
                                        put("billing_day", (rawCc["billing"] as? Number)?.toInt() ?: 0)
                                        put("due_day", (rawCc["due"] as? Number)?.toInt() ?: 0)
                                        put("reminder_day", (rawCc["rmndr"] as? Number)?.toInt() ?: 0)
                                        put("annual_fee", (rawCc["yr fee"] as? Number)?.toDouble() ?: 0.0)
                                        put("joining_fee", (rawCc["join fee"] as? Number)?.toDouble() ?: 0.0)
                                        
                                        val lastUseTs = rawCc["last use"] as? Timestamp
                                        val lastUseStr = lastUseTs?.let { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(it.toDate()) } ?: ""
                                        put("last_used", lastUseStr)
                                    }
                                    ccArray.put(ccObj)
                                }
                            }

                            val fdMap = dataMap["FD"] as? Map<*, *>
                            fdMap?.forEach { (key, rawFd) ->
                                if (rawFd is Map<*, *>) {
                                    val bank = rawFd["bank"]?.toString() ?: ""
                                    val accNo = rawFd["fd ac"]?.toString() ?: ""
                                    val amnt = (rawFd["amnt"] as? Number)?.toDouble() ?: 0.0
                                    val rate = (rawFd["int % yr"] as? Number)?.toDouble() ?: 0.0
                                    
                                    val createTs = rawFd["create"] as? Timestamp
                                    val maturTs = rawFd["matur"] as? Timestamp
                                    
                                    val createStr = createTs?.let { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(it.toDate()) } ?: ""
                                    val maturStr = maturTs?.let { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(it.toDate()) } ?: ""
                                    
                                    var daysToMat = 0
                                    var matVal = amnt
                                    var accVal = amnt
                                    var accInt = 0.0
                                    var oneDayInt = 0.0
                                    
                                    if (createTs != null && maturTs != null) {
                                        val createMillis = createTs.toDate().time
                                        val maturMillis = maturTs.toDate().time
                                        
                                        val totalDays = maxOf(0L, (maturMillis - createMillis) / (1000 * 60 * 60 * 24))
                                        daysToMat = maxOf(0L, (maturMillis - todayMillis) / (1000 * 60 * 60 * 24)).toInt()
                                        val daysPassed = maxOf(0L, (minOf(todayMillis, maturMillis) - createMillis) / (1000 * 60 * 60 * 24))
                                        
                                        matVal = amnt * Math.pow(1 + (rate / 100.0), totalDays / 365.0)
                                        accVal = amnt * Math.pow(1 + (rate / 100.0), daysPassed / 365.0)
                                        accInt = accVal - amnt
                                        
                                        oneDayInt = if (todayMillis >= maturMillis || todayMillis < createMillis) 0.0 else (accVal * (rate / 100.0)) / 365.0
                                    }

                                    val fdObj = JSONObject().apply {
                                        put("bank_name", bank)
                                        put("account_no", accNo)
                                        put("create_date", createStr)
                                        put("maturity_date", maturStr)
                                        put("days_to_maturity", daysToMat)
                                        put("invested_amt", amnt)
                                        put("interest_rate", rate)
                                        put("maturity_value", matVal)
                                        put("accrued_value", accVal)
                                        put("accrued_int", accInt)
                                        put("one_day_int", oneDayInt)
                                    }
                                    fdArray.put(fdObj)
                                }
                            }
                        }
                    }
                }

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

                val contriArray = JSONArray()
                val roomsArray = userDoc.get("rooms") as? List<*> ?: emptyList<Any>()

                for (roomItem in roomsArray) {
                    val roomStr = roomItem.toString()
                    val parts = roomStr.split("_", limit = 3)
                    if (parts.size >= 3) {
                        val cObj = JSONObject().apply {
                            put("room_name", parts[2])
                            put("room_code", parts[1])
                            put("passkey", "123456")
                            put("expenses", JSONArray()) 
                        }
                        contriArray.put(cObj)
                    }
                }

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
