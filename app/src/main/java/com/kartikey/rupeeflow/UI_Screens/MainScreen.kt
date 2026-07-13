// HELPER FUNCTION: Naye users (Signup) aur failsafe setup ke liye
function setupBankingColumns(sheet) {
  var z2Value = sheet.getRange("Z2").getValue();
  var aa2Value = sheet.getRange("AA2").getValue(); // Bug catch karne ke liye

  // Agar Z2 khali hai YA galti se har cell me Banking Data likha gaya hai, toh fix karo
  if (z2Value !== "Banking Data" || aa2Value === "Banking Data") {
    
    var titleRange = sheet.getRange("Z2:AP2");
    
    // 1. Purani galti ko clear karo
    titleRange.breakApart(); 
    titleRange.clearContent();
    
    // 2. Sahi tarika: Sirf Z2 me likho, aur fir Z2:AP2 ko merge karo
    sheet.getRange("Z2").setValue("Banking Data");
    titleRange.mergeAcross()
              .setHorizontalAlignment("center")
              .setFontWeight("bold")
              .setBackground("#e3f2fd"); 
    
    // Row 3: Headers (AO aur AP empty rakhe hain future use ke liye)
    var headers = [
      "Bank Name", "Account No.", "Current Bal.", "Interest % (Yr)", "Qtr. Interest %", 
      "6D Bal. Blocks", "6D Avg.", "Monthly Avg.", "Qtr. Avg.", "Yearly Avg.", 
      "Exp. Qtr. Int.", "Accrued Qtr. Int.", "Exp. Yr. Int.", "Accrued Yr. Int.", "1-Day Int.",
      "", "" // AO aur AP (Blank)
    ];
    sheet.getRange("Z3:AP3").setValues([headers])
         .setFontWeight("bold").setHorizontalAlignment("center");
  }
}

function doPost(e) {
  try {
    var data = JSON.parse(e.postData.contents);
    var action = data.action;
    var ss = SpreadsheetApp.getActiveSpreadsheet();
    var userSheet = ss.getSheetByName("Users");

    // ==========================================
    // 1. SIGNUP LOGIC
    // ==========================================
    if (action === "signup") {
      var rows = userSheet.getDataRange().getValues();
      for (var i = 1; i < rows.length; i++) {
        if (rows[i][1] == data.mobile) return ContentService.createTextOutput(JSON.stringify({"status": "error", "message": "Mobile already registered!"})).setMimeType(ContentService.MimeType.JSON);
        if (rows[i][2] == data.username) return ContentService.createTextOutput(JSON.stringify({"status": "error", "message": "Username already taken!"})).setMimeType(ContentService.MimeType.JSON);
      }
      
      userSheet.appendRow([data.name, data.mobile, data.username, data.password, ""]);
      var newSheet = ss.insertSheet(data.username);
      
      // Expense aur Investment Layout
      newSheet.getRange("A1").setValue("Expenses Data");
      newSheet.getRange("A1:G1").mergeAcross().setHorizontalAlignment("center").setFontWeight("bold").setBackground("#e0f2f1");
      
      newSheet.getRange("I1").setValue("Investment Data");
      newSheet.getRange("I1:X1").mergeAcross().setHorizontalAlignment("center").setFontWeight("bold").setBackground("#fff9c4");

      var expensesHeaders = ["Date", "Amount", "Category", "Detail_1", "Detail_2", "Mode", ""];
      newSheet.getRange("A2:G2").setValues([expensesHeaders]).setFontWeight("bold");

      var invHeaders = ["Inv_Date", "Asset_Name", "Asset_Type", "Quantity", "Avg_Buy_Price", "Invested_Value", "Current_Price", "Current_Value", "1_Day_Return", "Total_Return_₹", "Total_Return_%", "Broker_Platform", "Notes", "", "", ""];
      newSheet.getRange("I2:X2").setValues([invHeaders]).setFontWeight("bold");

      // Naye user ki sheet me Banking layout bhi automatic ban jayega
      setupBankingColumns(newSheet);

      return ContentService.createTextOutput(JSON.stringify({"status": "success", "message": "Account Created!", "username": data.username})).setMimeType(ContentService.MimeType.JSON);
    }

    // ==========================================
    // 2. LOGIN LOGIC
    // ==========================================
    if (action === "login") {
      var rows = userSheet.getDataRange().getValues();
      for (var i = 1; i < rows.length; i++) {
        if ((rows[i][1] == data.mobile || rows[i][2] == data.mobile) && rows[i][3] == data.password) {
          return ContentService.createTextOutput(JSON.stringify({
            "status": "success", 
            "message": "Login Successful!", 
            "username": rows[i][2],
            "email": rows[i][4] ? rows[i][4] : ""
          })).setMimeType(ContentService.MimeType.JSON);
        }
      }
      return ContentService.createTextOutput(JSON.stringify({"status": "error", "message": "Invalid Mobile or Password!"})).setMimeType(ContentService.MimeType.JSON);
    }

    // ==========================================
    // 3. ADD EXPENSE LOGIC 
    // ==========================================
    if (action === "add_expense") {
      var targetSheet = ss.getSheetByName(data.username);
      if (!targetSheet) return ContentService.createTextOutput(JSON.stringify({"status": "error", "message": "User sheet not found!"})).setMimeType(ContentService.MimeType.JSON);
      
      if (targetSheet.getRange("F2").getValue() === "") targetSheet.getRange("F2").setValue("Mode").setFontWeight("bold");

      var timestamp = Utilities.formatDate(new Date(), "Asia/Kolkata", "dd-MM-yyyy hh:mm a");
      var aValues = targetSheet.getRange("A:A").getValues();
      var nextExpenseRow = 3; 
      for (var i = aValues.length - 1; i >= 0; i--) {
        if (aValues[i][0] != "") { nextExpenseRow = i + 2; break; }
      }
      
      var expenseData = [timestamp, data.amount, data.category, data.detail1, data.detail2, data.payment_method || "", ""];
      targetSheet.getRange(nextExpenseRow, 1, 1, expenseData.length).setValues([expenseData]);
      return ContentService.createTextOutput(JSON.stringify({"status": "success", "message": "Saved!"})).setMimeType(ContentService.MimeType.JSON);
    }

    // ==========================================
    // 4. ADD INVESTMENT LOGIC 
    // ==========================================
    if (action === "add_investment") {
      var targetSheet = ss.getSheetByName(data.username);
      if (!targetSheet) return ContentService.createTextOutput(JSON.stringify({"status": "error", "message": "User sheet not found!"})).setMimeType(ContentService.MimeType.JSON);
      
      var jValues = targetSheet.getRange("J:J").getValues();
      var nextRow = 3; 
      for (var k = jValues.length - 1; k >= 0; k--) {
        if (jValues[k][0] != "") { nextRow = k + 2; break; }
      }
      
      var f_invested = "=L" + nextRow + "*M" + nextRow;  
      var f_currentPrice = '=IF(OR(K'+nextRow+'="Stock", K'+nextRow+'="ETF"), GOOGLEFINANCE("NSE:"&J'+nextRow+', "price"), M'+nextRow+')'; 
      var f_currentValue = "=L" + nextRow + "*O" + nextRow;
      var f_1DayReturn = '=IF(OR(K'+nextRow+'="Stock", K'+nextRow+'="ETF"), GOOGLEFINANCE("NSE:"&J'+nextRow+', "change"), 0)';
      var f_totalReturn = "=P" + nextRow + "-N" + nextRow;
      var f_totalReturnPct = "=IF(N"+nextRow+">0, (R"+nextRow+"/N"+nextRow+")*100, 0)";

      var invData = [
        data.inv_date,                  
        data.asset_name,                
        data.asset_type,                
        data.quantity,                  
        data.buy_price,                 
        f_invested,            
        f_currentPrice,        
        f_currentValue,        
        f_1DayReturn,          
        f_totalReturn,         
        f_totalReturnPct,      
        data.broker || "",                    
        data.notes || "",
        "", "", "" 
      ];
      
      targetSheet.getRange(nextRow, 9, 1, invData.length).setValues([invData]);
      return ContentService.createTextOutput(JSON.stringify({"status": "success", "message": "Investment Saved!"})).setMimeType(ContentService.MimeType.JSON);
    }

    // ==========================================
    // 5. ADD BANK LOGIC
    // ==========================================
    if (action === "add_bank") {
      var targetSheet = ss.getSheetByName(data.username);
      if (!targetSheet) return ContentService.createTextOutput(JSON.stringify({"status": "error", "message": "User sheet not found!"})).setMimeType(ContentService.MimeType.JSON);
      
      // Purane/Failsafe users ke liye check aur setup karega
      setupBankingColumns(targetSheet);

      var zValues = targetSheet.getRange("Z:Z").getValues();
      var nextBankRow = 4; // Default pehla bank 4th row me jayega
      var lastFilledRow = -1;
      
      for (var k = zValues.length - 1; k >= 3; k--) {
        if (zValues[k][0] != "") { 
          lastFilledRow = k + 1; // Array index 0-based hota hai, Row 1-based hoti hai
          break; 
        }
      }
      
      if (lastFilledRow !== -1) {
        nextBankRow = lastFilledRow + 10; 
      }

      var f_qtrIntPct = "=AC" + nextBankRow + "/4";
      var f_1DayInt = "=(AB" + nextBankRow + "*(AC" + nextBankRow + "/100))/365";
      var initialBlocks = JSON.stringify({"1-6":0, "7-12":0, "13-18":0, "19-24":0, "25-31":0});

      var bankData = [
        data.bank_name,
        data.account_no,
        data.current_bal,
        data.interest_rate,
        f_qtrIntPct,       
        initialBlocks,     
        0,                 
        0,                 
        0,                 
        0,                 
        0,                 
        0,                 
        0,                 
        0,                 
        f_1DayInt          
      ];

      targetSheet.getRange(nextBankRow, 26, 1, bankData.length).setValues([bankData]).setHorizontalAlignment("center");
      
      return ContentService.createTextOutput(JSON.stringify({"status": "success", "message": "Bank Account Added!"})).setMimeType(ContentService.MimeType.JSON);
    }

    // ==========================================
    // 6. GET ALL DATA
    // ==========================================
    if (action === "get_all_data") {
      var targetSheet = ss.getSheetByName(data.username);
      if (!targetSheet) return ContentService.createTextOutput(JSON.stringify({"status": "success", "expenses": [], "investments": [], "banks": []})).setMimeType(ContentService.MimeType.JSON);
      
      // Data fetch ke time bhi columns setup verify kar lenge as failsafe
      setupBankingColumns(targetSheet);

      var rows = targetSheet.getDataRange().getValues();
      var userExpenses = [];
      var userInvestments = [];
      var userBanks = [];
      
      for (var i = 2; i < rows.length; i++) {
        // --- Expenses ---
        if(rows[i][0] != "") { 
            var dateStr = (rows[i][0] instanceof Date) ? Utilities.formatDate(rows[i][0], "Asia/Kolkata", "dd-MM-yyyy hh:mm a") : rows[i][0].toString();
            userExpenses.push({
              date: dateStr, amount: rows[i][1], category: rows[i][2],
              detail1: rows[i][3] ? rows[i][3].toString() : "", detail2: rows[i][4] ? rows[i][4].toString() : "",
              mode: rows[i][5] ? rows[i][5].toString() : ""
            });
        }
        
        // --- Investments ---
        if(rows[i][9] && rows[i][9] !== "") { 
            userInvestments.push({
              inv_date: rows[i][8] ? rows[i][8].toString() : "",      
              asset_name: rows[i][9].toString(),    
              asset_type: rows[i][10] ? rows[i][10].toString() : "Stock",
              quantity: parseFloat(rows[i][11]) || 0,
              buy_price: parseFloat(rows[i][12]) || 0,
              current_price: parseFloat(rows[i][14]) || 0, 
              one_day_change: parseFloat(rows[i][16]) || 0 
            });
        }

        // --- Banks (Z column is index 25) ---
        if(i >= 3 && rows[i][25] && rows[i][25] !== "") { 
            userBanks.push({
              bank_name: rows[i][25].toString(),
              account_no: rows[i][26].toString(),
              current_bal: parseFloat(rows[i][27]) || 0,
              interest_rate: parseFloat(rows[i][28]) || 0,
              qtr_interest_pct: parseFloat(rows[i][29]) || 0,
              one_day_int: parseFloat(rows[i][39]) || 0 
            });
        }
      }
      return ContentService.createTextOutput(JSON.stringify({"status": "success", "expenses": userExpenses, "investments": userInvestments, "banks": userBanks})).setMimeType(ContentService.MimeType.JSON);
    }

  } catch(error) {
    return ContentService.createTextOutput(JSON.stringify({"status": "error", "message": error.message})).setMimeType(ContentService.MimeType.JSON);
  }
}

// ==========================================
// BACKGROUND AUTOMATION: 3:50 PM FREEZE LOGIC
// ==========================================
function freezeMarketData() {
  var ss = SpreadsheetApp.getActiveSpreadsheet();
  var sheets = ss.getSheets();
  
  for (var i = 0; i < sheets.length; i++) {
    var sheet = sheets[i];
    if (sheet.getName() !== "Users") {
      var lastRow = sheet.getLastRow();
      if (lastRow > 2) {
        var priceRange = sheet.getRange(3, 15, lastRow - 2, 1); 
        var returnRange = sheet.getRange(3, 17, lastRow - 2, 1); 
        
        priceRange.setValues(priceRange.getValues());
        returnRange.setValues(returnRange.getValues());
      }
    }
  }
}

// ==========================================
// BACKGROUND AUTOMATION: 9:00 AM RESUME LOGIC
// ==========================================
function resumeMarketData() {
  var ss = SpreadsheetApp.getActiveSpreadsheet();
  var sheets = ss.getSheets();
  
  for (var i = 0; i < sheets.length; i++) {
    var sheet = sheets[i];
    if (sheet.getName() !== "Users") {
      var lastRow = sheet.getLastRow();
      if (lastRow > 2) {
        for (var row = 3; row <= lastRow; row++) {
          var assetType = sheet.getRange(row, 11).getValue(); 
          if (assetType === "Stock" || assetType === "ETF") {
            var formulaPrice = '=GOOGLEFINANCE("NSE:"&J'+row+', "price")';
            var formulaChange = '=GOOGLEFINANCE("NSE:"&J'+row+', "change")';
            
            sheet.getRange(row, 15).setFormula(formulaPrice); 
            sheet.getRange(row, 17).setFormula(formulaChange); 
          }
        }
      }
    }
  }
}
