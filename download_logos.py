import os
import urllib.request

# Aapke Android project ka drawable path
output_dir = "app/src/main/res/drawable"
os.makedirs(output_dir, exist_ok=True)

# Banks and Fintechs Domains Dictionary
banks = {
    "ic_sbi": "sbi.co.in",
    "ic_hdfc": "hdfcbank.com",
    "ic_icici": "icicibank.com",
    "ic_axis": "axisbank.com",
    "ic_kotak": "kotak.com",
    "ic_pnb": "pnbindia.in",
    "ic_bob": "bankofbaroda.in",
    "ic_boi": "bankofindia.co.in",
    "ic_union": "unionbankofindia.co.in",
    "ic_canara": "canarabank.com",
    "ic_central": "centralbankofindia.co.in",
    "ic_indian": "indianbank.in",
    "ic_iob": "iob.in",
    "ic_uco": "ucobank.com",
    "ic_bom": "bankofmaharashtra.in",
    "ic_idbi": "idbibank.in",
    "ic_yes": "yesbank.in",
    "ic_indusind": "indusind.com",
    "ic_federal": "federalbank.co.in",
    "ic_idfc": "idfcfirstbank.com",
    "ic_bandhan": "bandhanbank.com",
    "ic_rbl": "rblbank.com",
    "ic_au": "aubank.in",
    "ic_paytm": "paytm.com",
    "ic_airtel": "airtel.in",
    "ic_jio": "jio.com",
    "ic_fino": "finobank.com",
    "ic_amazon": "amazon.in",
    "ic_flipkart": "flipkart.com",
    "ic_kiwi": "gokiwi.in",
    "ic_supermoney": "super.money",
    "ic_onecard": "getonecard.app",
    "ic_slice": "sliceit.com",
    "ic_unicards": "uni.cards",
    "ic_tataneu": "tatadigital.com",
    "ic_swiggy": "swiggy.com",
    "ic_zomato": "zomato.com",
    "ic_sbicard": "sbicard.com",
    "ic_cred": "cred.club"
}

req_headers = {'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)'}

print("Downloading Ultra-HD Transparent Bank Logos...\n")

for name, domain in banks.items():
    url = f"https://logo.clearbit.com/{domain}?size=512"
    file_path = f"{output_dir}/{name}.png"
    
    try:
        req = urllib.request.Request(url, headers=req_headers)
        with urllib.request.urlopen(req) as response:
            with open(file_path, "wb") as out_file:
                out_file.write(response.read())
        print(f"✅ Saved: {name}.png")
    except Exception as e:
        print(f"❌ Failed: {name}.png (Domain not found or blocked)")

print("\n🎉 Sabhi HD Logos successfully drawable folder mein download ho gaye hain!")
