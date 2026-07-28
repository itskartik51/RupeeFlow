package com.kartikey.rupeeflow.UI_Screens

import android.content.Context
import com.kartikey.rupeeflow.Cloud_Database.Constants
import com.kartikey.rupeeflow.UI_Screens.Add.TransactionModel
import com.kartikey.rupeeflow.UI_Screens.Assets.InvestmentItem
import com.kartikey.rupeeflow.UI_Screens.Assets.BankAccountItem
import com.kartikey.rupeeflow.UI_Screens.Assets.Finance.CashItem
import com.kartikey.rupeeflow.UI_Screens.Assets.Finance.CreditCardItem
import com.kartikey.rupeeflow.UI_Screens.Assets.Finance.FDItem
import com.kartikey.rupeeflow.UI_Screens.Home.ContriRoomModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

// Naya Singleton Network Engine (Connection Pooling & Keep-Alive ke liye)
object NetworkClient {
    val instance: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true) // Network drop hone par auto-retry
            .build()
    }
}

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
                val json = JSONObject().apply { 
                    put("action", "get_all_data")
                    put("username", username) 
                    put("force_refresh", forceRefresh) 
                }
                
                // Ab naya client nahi banega, purana fast connection reuse hoga
                val client = NetworkClient.instance 
                val body = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
                val request = Request.Builder().url(Constants.GOOGLE_SHEET_API_URL).post(body).build()
                val response = client.newCall(request).execute()
                val responseData = response.body?.string() ?: ""

                if (response.isSuccessful && responseData.trim().startsWith("{")) {
                    val jsonResponse = JSONObject(responseData)
                    if (jsonResponse.optString("status") == "success") {
                        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                        prefs.edit().putString("data_$username", responseData).apply()
                        return@withContext parseJsonToAppData(responseData)
                    }
                }
                null
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
            thisMonthExpenses = if (tempMonth > 0) tempMonth else tempTotal,
            thisYearExpenses = if (tempYear > 0) tempYear else tempTotal,
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
