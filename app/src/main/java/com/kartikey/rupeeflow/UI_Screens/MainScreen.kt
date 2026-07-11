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
      
      newSheet.getRange("A1").setValue("Expenses Data");
      newSheet.getRange("A1:G1").mergeAcross().setHorizontalAlignment("center").setFontWeight("bold").setBackground("#e0f2f1");
      
      newSheet.getRange("I1").setValue("Investment Data");
      newSheet.getRange("I1:X1").mergeAcross().setHorizontalAlignment("center").setFontWeight("bold").setBackground("#fff9c4");

      var expensesHeaders = ["Date", "Amount", "Category", "Detail_1", "Detail_2", "", ""];
      newSheet.getRange("A2:G2").setValues([expensesHeaders]).setFontWeight("bold");

      var invHeaders = ["Inv_Date", "Asset_Name", "Asset_Type", "Quantity", "Avg_Buy_Price", "Invested_Value", "Current_Price", "Current_Value", "1_Day_Return", "Total_Return_₹", "Total_Return_%", "Broker_Platform", "Notes", "", "", ""];
      newSheet.getRange("I2:X2").setValues([invHeaders]).setFontWeight("bold");

      return ContentService.createTextOutput(JSON.stringify({"status": "success", "message": "Account Created!", "username": data.username})).setMimeType(ContentService.MimeType.JSON);
    }

    // ==========================================
    // 2. LOGIN LOGIC
    // ==========================================
    if (action === "login") {
      var rows = userSheet.getDataRange().getValues();
      for (var i = 1; i < rows.length; i++) {
        if ((rows[i][1] == data.mobile || rows[i][2] == data.mobile) && rows[i][3] == data.password) {
          var mailId = rows[i][4] ? rows[i][4] : ""; 
          return ContentService.createTextOutput(JSON.stringify({
            "status": "success", 
            "message": "Login Successful!", 
            "username": rows[i][2],
            "email": mailId
          })).setMimeType(ContentService.MimeType.JSON);
        }
      }
      return ContentService.createTextOutput(JSON.stringify({"status": "error", "message": "Invalid Mobile or Password!"})).setMimeType(ContentService.MimeType.JSON);
    }

    // ==========================================
    // 3. ADD EXPENSE LOGIC (FIXED)
    // ==========================================
    if (action === "add_expense") {
      var targetSheet = ss.getSheetByName(data.username);
      if (!targetSheet) {
        return ContentService.createTextOutput(JSON.stringify({"status": "error", "message": "User sheet not found!"})).setMimeType(ContentService.MimeType.JSON);
      }
      var timestamp = Utilities.formatDate(new Date(), "Asia/Kolkata", "dd-MM-yyyy hh:mm a");
      
      var aValues = targetSheet.getRange("A:A").getValues();
      var nextExpenseRow = 3; 
      for (var i = aValues.length - 1; i >= 0; i--) {
        if (aValues[i][0] != "") {
          nextExpenseRow = i + 2;
          break;
        }
      }
      
      var expenseData = [timestamp, data.amount, data.category, data.detail1, data.detail2, "", ""];
      
      targetSheet.getRange(nextExpenseRow, 1, 1, expenseData.length).setValues([expenseData]);
      
      return ContentService.createTextOutput(JSON.stringify({"status": "success", "message": "Saved!"})).setMimeType(ContentService.MimeType.JSON);
    }

    // ==========================================
    // 4. GET EXPENSES LOGIC
    // ==========================================
    if (action === "get_expenses") {
      var targetSheet = ss.getSheetByName(data.username);
      if (!targetSheet) {
        return ContentService.createTextOutput(JSON.stringify({"status": "success", "data": []})).setMimeType(ContentService.MimeType.JSON);
      }
      
      var rows = targetSheet.getDataRange().getValues();
      var userExpenses = [];
      
      for (var i = 2; i < rows.length; i++) {
        if(rows[i][0] != "") { 
            var dateStr = "";
            try {
              if (rows[i][0] instanceof Date) {
                 dateStr = Utilities.formatDate(rows[i][0], "Asia/Kolkata", "dd-MM-yyyy hh:mm a");
              } else {
                 dateStr = rows[i][0].toString();
              }
            } catch(e) {
              dateStr = rows[i][0].toString();
            }

            userExpenses.push({
              date: dateStr,      
              amount: rows[i][1],    
              category: rows[i][2]   
            });
        }
      }
      return ContentService.createTextOutput(JSON.stringify({"status": "success", "data": userExpenses})).setMimeType(ContentService.MimeType.JSON);
    }

    // ==========================================
    // 5. ADD INVESTMENT LOGIC (FIXED)
    // ==========================================
    if (action === "add_investment") {
      var targetSheet = ss.getSheetByName(data.username);
      if (!targetSheet) {
        return ContentService.createTextOutput(JSON.stringify({"status": "error", "message": "User sheet not found!"})).setMimeType(ContentService.MimeType.JSON);
      }
      
      var jValues = targetSheet.getRange("J:J").getValues();
      var nextInvRow = 3; 
      for (var k = jValues.length - 1; k >= 0; k--) {
        if (jValues[k][0] != "") {
          nextInvRow = k + 2;
          break;
        }
      }
      
      var invData = [
        data.inv_date,                  
        data.asset_name,                
        data.asset_type,                
        data.quantity,                  
        data.buy_price,                 
        data.invested_value,            
        data.current_price,             
        data.current_value,             
        data.one_day_return,            
        data.total_return_rupee,        
        data.total_return_percent,      
        data.broker,                    
        data.notes,
        "", "", "" 
      ];
      
      targetSheet.getRange(nextInvRow, 9, 1, invData.length).setValues([invData]);
      
      return ContentService.createTextOutput(JSON.stringify({"status": "success", "message": "Investment Saved!"})).setMimeType(ContentService.MimeType.JSON);
    }

    // ==========================================
    // 6. GET INVESTMENTS LOGIC
    // ==========================================
    if (action === "get_investments") {
      var targetSheet = ss.getSheetByName(data.username);
      if (!targetSheet) {
        return ContentService.createTextOutput(JSON.stringify({"status": "success", "data": []})).setMimeType(ContentService.MimeType.JSON);
      }
      
      var rows = targetSheet.getDataRange().getValues();
      var userInvestments = [];
      
      for (var i = 2; i < rows.length; i++) {
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
      }
      return ContentService.createTextOutput(JSON.stringify({"status": "success", "data": userInvestments})).setMimeType(ContentService.MimeType.JSON);
    }

    // ==========================================
    // 7. NAYA COMBO PACK: GET ALL DATA
    // ==========================================
    if (action === "get_all_data") {
      var targetSheet = ss.getSheetByName(data.username);
      if (!targetSheet) {
        return ContentService.createTextOutput(JSON.stringify({
          "status": "success", 
          "expenses": [], 
          "investments": []
        })).setMimeType(ContentService.MimeType.JSON);
      }
      
      var rows = targetSheet.getDataRange().getValues();
      var userExpenses = [];
      var userInvestments = [];
      
      for (var i = 2; i < rows.length; i++) {
        // --- Pehle Expenses Extract Karega ---
        if(rows[i][0] != "") { 
            var dateStr = "";
            try {
              if (rows[i][0] instanceof Date) {
                 dateStr = Utilities.formatDate(rows[i][0], "Asia/Kolkata", "dd-MM-yyyy hh:mm a");
              } else {
                 dateStr = rows[i][0].toString();
              }
            } catch(e) {
              dateStr = rows[i][0].toString();
            }

            userExpenses.push({
              date: dateStr,      
              amount: rows[i][1],    
              category: rows[i][2],
              detail1: rows[i][3] ? rows[i][3].toString() : "", // Future proofing ke liye details bhi le li hain
              detail2: rows[i][4] ? rows[i][4].toString() : ""
            });
        }

        // --- Phir Investments Extract Karega ---
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
      }
      
      return ContentService.createTextOutput(JSON.stringify({
        "status": "success", 
        "expenses": userExpenses,
        "investments": userInvestments
      })).setMimeType(ContentService.MimeType.JSON);
    }

  } catch(error) {
    return ContentService.createTextOutput(JSON.stringify({"status": "error", "message": error.message})).setMimeType(ContentService.MimeType.JSON);
  }
}
