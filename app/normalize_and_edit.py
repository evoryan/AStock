import os

file_path = "/app/src/main/java/com/example/MainActivity.kt"

with open(file_path, "r", encoding="utf-8") as f:
    content = f.read()

# Normalize line endings to \n
content = content.replace("\r\n", "\n")

# Target 1: first onLogoutClick (before onShowGuideClick)
target_1 = """                                         onLogoutClick = {
                                             loggedInTenant = null
                                             currentScreen = AppScreen.Login
                                         },
                                         onShowGuideClick = {"""

replacement_1 = """                                         onLogoutClick = {
                                             loggedInTenant = null
                                             val sharedPrefs = this@MainActivity.getSharedPreferences("app_session", android.content.Context.MODE_PRIVATE)
                                             sharedPrefs.edit().clear().apply()
                                             currentScreen = AppScreen.Login
                                         },
                                         onShowGuideClick = {"""

# Target 2: second onLogoutClick (before refreshData)
target_2 = """                                         onLogoutClick = {
                                             loggedInTenant = null
                                             currentScreen = AppScreen.Login
                                         },
                                         refreshData = {"""

replacement_2 = """                                         onLogoutClick = {
                                             loggedInTenant = null
                                             val sharedPrefs = this@MainActivity.getSharedPreferences("app_session", android.content.Context.MODE_PRIVATE)
                                             sharedPrefs.edit().clear().apply()
                                             currentScreen = AppScreen.Login
                                         },
                                         refreshData = {"""

if target_1 in content:
    content = content.replace(target_1, replacement_1)
    print("Successfully replaced first logout block!")
else:
    print("Warning: target_1 not found!")

if target_2 in content:
    content = content.replace(target_2, replacement_2)
    print("Successfully replaced second logout block!")
else:
    print("Warning: target_2 not found!")

with open(file_path, "w", encoding="utf-8") as f:
    f.write(content)

print("MainActivity.kt written successfully.")
