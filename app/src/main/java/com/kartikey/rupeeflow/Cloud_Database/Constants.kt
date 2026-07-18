package com.kartikey.rupeeflow.Cloud_Database

object Constants {
    const val GOOGLE_SHEET_API_URL = "https://script.google.com/macros/s/AKfycbxq6GiNwSEDI85iWu8Zl1mMokr0TPidrIKyM5eHfnnPumnhq4Z1szCLjg-JcuTK_gr6Aw/exec"

    val IndianBanksList = listOf(
        // Core Banks
        "State Bank of India (SBI)", "HDFC Bank", "ICICI Bank", "Axis Bank", "Kotak Mahindra Bank",
        "Punjab National Bank (PNB)", "Bank of Baroda", "Bank of India", "Union Bank of India",
        "Canara Bank", "Central Bank of India", "Indian Bank", "Indian Overseas Bank", "UCO Bank",
        "Bank of Maharashtra", "IDBI Bank", "Yes Bank", "IndusInd Bank", "Federal Bank",
        "South Indian Bank", "IDFC First Bank", "Bandhan Bank", "RBL Bank",
        
        // Small Finance & Cooperative Banks
        "AU Small Finance Bank", "Equitas Small Finance Bank", "Ujjivan Small Finance Bank",
        "Utkarsh Small Finance Bank", "Suryoday Small Finance Bank", "Shivalik Small Finance Bank",
        "Capital Small Finance Bank", "Jana Small Finance Bank", "Unity Small Finance Bank",
        "ESAF Small Finance Bank", "North East Small Finance Bank", "Saraswat Cooperative Bank", 
        "Cosmos Cooperative Bank",
        
        // Payments Banks
        "Paytm Payments Bank", "Airtel Payments Bank", "India Post Payments Bank", 
        "Jio Payments Bank", "Fino Payments Bank",
        
        // Foreign & Regional Banks
        "Standard Chartered Bank", "Citi Bank", "HSBC Bank", "DBS Bank", "Barclays Bank", 
        "Deutsche Bank", "J&K Bank", "Karnataka Bank", "Karur Vysya Bank", "City Union Bank", 
        "Dhanlaxmi Bank", "CSB Bank", "Nainital Bank", "Tamilnad Mercantile Bank",
        
        // NEW: Fintechs, Co-Branded & Credit Card Issuers
        "Amazon Pay", "Flipkart", "KIWI", "Super.money", "OneCard", "Slice", "Uni Cards",
        "Tata Neu", "Swiggy", "Zomato", "SBI Card", "Cred", "Other Bank"
    )

    // 100% Pure Root Domains for Maximum API Hit-Rate
    val BankDomainMap = mapOf(
        // Banks
        "State Bank of India (SBI)" to "sbi.co.in", "HDFC Bank" to "hdfcbank.com",
        "ICICI Bank" to "icicibank.com", "Axis Bank" to "axisbank.com", "Kotak Mahindra Bank" to "kotak.com",
        "Punjab National Bank (PNB)" to "pnbindia.in", "Bank of Baroda" to "bankofbaroda.in",
        "Bank of India" to "bankofindia.co.in", "Union Bank of India" to "unionbankofindia.co.in",
        "Canara Bank" to "canarabank.com", "Central Bank of India" to "centralbankofindia.co.in",
        "Indian Bank" to "indianbank.in", "Indian Overseas Bank" to "iob.in", "UCO Bank" to "ucobank.com",
        "Bank of Maharashtra" to "bankofmaharashtra.in", "IDBI Bank" to "idbibank.in",
        "Yes Bank" to "yesbank.in", "IndusInd Bank" to "indusind.com", "Federal Bank" to "federalbank.co.in",
        "South Indian Bank" to "southindianbank.com", "IDFC First Bank" to "idfcfirstbank.com",
        "Bandhan Bank" to "bandhanbank.com", "RBL Bank" to "rblbank.com", "AU Small Finance Bank" to "aubank.in",
        "Equitas Small Finance Bank" to "equitasbank.com", "Ujjivan Small Finance Bank" to "ujjivansfb.in",
        "Utkarsh Small Finance Bank" to "utkarsh.bank", "Paytm Payments Bank" to "paytm.com",
        "Airtel Payments Bank" to "airtel.in", "India Post Payments Bank" to "ippbonline.com",
        "Jio Payments Bank" to "jio.com", "Fino Payments Bank" to "finobank.com",
        "Standard Chartered Bank" to "sc.com", "Citi Bank" to "citibank.co.in",
        "HSBC Bank" to "hsbc.co.in", "DBS Bank" to "dbs.com", "Barclays Bank" to "barclays.in",
        "Deutsche Bank" to "db.com", "J&K Bank" to "jkbank.com", "Karnataka Bank" to "karnatakabank.com",
        "Karur Vysya Bank" to "kvb.co.in", "City Union Bank" to "cityunionbank.com",
        "Dhanlaxmi Bank" to "dhanbank.com", "CSB Bank" to "csb.co.in", "Nainital Bank" to "nainitalbank.co.in",
        "Tamilnad Mercantile Bank" to "tmbnet.in", "Suryoday Small Finance Bank" to "suryodaybank.com",
        "Shivalik Small Finance Bank" to "shivalikbank.com", "Capital Small Finance Bank" to "capitalbank.co.in",
        "Jana Small Finance Bank" to "janabank.com", "Unity Small Finance Bank" to "theunitybank.com",
        "ESAF Small Finance Bank" to "esafbank.com", "North East Small Finance Bank" to "nesfb.com",
        "Saraswat Cooperative Bank" to "saraswatbank.com", "Cosmos Cooperative Bank" to "cosmosbank.com",
        
        // NEW: Fintechs, Co-Branded & Credit Card Issuers Domains
        "Amazon Pay" to "amazon.in",
        "Flipkart" to "flipkart.com",
        "KIWI" to "gokiwi.in",
        "Super.money" to "super.money",
        "OneCard" to "getonecard.app",
        "Slice" to "sliceit.com",
        "Uni Cards" to "uni.cards",
        "Tata Neu" to "tatadigital.com",
        "Swiggy" to "swiggy.com",
        "Zomato" to "zomato.com",
        "SBI Card" to "sbicard.com",
        "Cred" to "cred.club",
        
        "Other Bank" to "rbi.org.in"
    )
}
