package com.kartikey.rupeeflow.Cloud_Database

object Constants {
    const val GOOGLE_SHEET_API_URL = "https://script.google.com/macros/s/AKfycbxq6GiNwSEDI85iWu8Zl1mMokr0TPidrIKyM5eHfnnPumnhq4Z1szCLjg-JcuTK_gr6Aw/exec"

    val IndianBanksList = listOf(
        "State Bank of India (SBI)", "HDFC Bank", "ICICI Bank", "Axis Bank", "Kotak Mahindra Bank",
        "Punjab National Bank (PNB)", "Bank of Baroda", "Bank of India", "Union Bank of India",
        "Canara Bank", "Central Bank of India", "Indian Bank", "Indian Overseas Bank", "UCO Bank",
        "Bank of Maharashtra", "IDBI Bank", "Yes Bank", "IndusInd Bank", "Federal Bank",
        "South Indian Bank", "IDFC First Bank", "Bandhan Bank", "RBL Bank", "AU Small Finance Bank",
        "Equitas Small Finance Bank", "Ujjivan Small Finance Bank", "Paytm Payments Bank",
        "Airtel Payments Bank", "India Post Payments Bank", "Standard Chartered Bank", "Citi Bank", "HSBC Bank", "Other Bank"
    )

    // HD Logos fetch karne ke liye domains
    val BankDomainMap = mapOf(
        "State Bank of India (SBI)" to "sbi.co.in",
        "HDFC Bank" to "hdfcbank.com",
        "ICICI Bank" to "icicibank.com",
        "Axis Bank" to "axisbank.com",
        "Kotak Mahindra Bank" to "kotak.com",
        "Punjab National Bank (PNB)" to "pnbindia.in",
        "Bank of Baroda" to "bankofbaroda.in",
        "Bank of India" to "bankofindia.co.in",
        "Union Bank of India" to "unionbankofindia.co.in",
        "Canara Bank" to "canarabank.com",
        "Central Bank of India" to "centralbankofindia.co.in",
        "Indian Bank" to "indianbank.in",
        "Indian Overseas Bank" to "iob.in",
        "UCO Bank" to "ucobank.com",
        "Bank of Maharashtra" to "bankofmaharashtra.in",
        "IDBI Bank" to "idbibank.in",
        "Yes Bank" to "yesbank.in",
        "IndusInd Bank" to "indusind.com",
        "Federal Bank" to "federalbank.co.in",
        "South Indian Bank" to "southindianbank.com",
        "IDFC First Bank" to "idfcfirstbank.com",
        "Bandhan Bank" to "bandhanbank.com",
        "RBL Bank" to "rblbank.com",
        "AU Small Finance Bank" to "aubank.in",
        "Equitas Small Finance Bank" to "equitasbank.com",
        "Ujjivan Small Finance Bank" to "ujjivansfb.in",
        "Paytm Payments Bank" to "paytmbank.com",
        "Airtel Payments Bank" to "airtel.in",
        "India Post Payments Bank" to "ippbonline.com",
        "Standard Chartered Bank" to "sc.com",
        "Citi Bank" to "citibank.co.in",
        "HSBC Bank" to "hsbc.co.in"
    )
}
