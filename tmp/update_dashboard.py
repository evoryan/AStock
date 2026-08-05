with open('app/src/main/java/com/example/ui/screens/DashboardScreen.kt', 'r') as f:
    lines = f.readlines()

# Replace the onClick body for POS Checkout (originally lines 857 to 884, indices 856 to 883)
new_checkout_block = """                                            if (item.stock < cartQuantity) {
                                                return@Button
                                            }
                                            coroutineScope.launch {
                                                try {
                                                     val finalStock = item.stock - cartQuantity
                                                     ApiClient.getService().updateStock(
                                                         item.id,
                                                         StockUpdateRequest(tenant.dbName, finalStock)
                                                     )

                                                     val txId = "TX-${tenant.id.uppercase()}-${(1000..9999).random()}"
                                                     val nowMs = System.currentTimeMillis()
                                                     ApiClient.getService().addTransaction(
                                                         TransactionAddRequest(
                                                             db_name = tenant.dbName,
                                                             id = txId,
                                                             productName = item.name,
                                                             sku = item.sku,
                                                             quantity = cartQuantity,
                                                             totalPrice = finalTotal,
                                                             timestamp = nowMs,
                                                             operator = tenant.ownerName
                                                         )
                                                     )

                                                     checkoutSuccessMessage = "$cartQuantity x ${item.name} berhasil dibeli."
                                                     selectedProductForCart = null
                                                     cartQuantity = 1
                                                     refreshData()
                                                } catch (e: Exception) {
                                                    dashboardError = "Transaksi POS gagal: ${e.localizedMessage}"
                                                }
                                            }
"""

# Replace the POS checkout onClick body (lines 857 to 884)
lines[856:884] = [new_checkout_block]

# Now let's locate TenantSimulator again after our first replacement shifted the lines
add_product_idx = -1
for idx, line in enumerate(lines):
    if 'TenantSimulator.addProduct' in line:
        add_product_idx = idx
        break

if add_product_idx != -1:
    # We want to replace the onClick body of add product which starts above add_product_idx and ends after it.
    start_idx = add_product_idx - 17 # usually around val priceParsed = ...
    end_idx = add_product_idx + 3    # showAddProductModal = false

    print(f"Replacing add product onClick body from index {start_idx} to {end_idx}")

    new_add_product_block = """                            val priceParsed = newProdPrice.toDoubleOrNull()
                            val stockParsed = newProdStock.toIntOrNull()

                            if (newProdName.isBlank() || newProdSku.isBlank() || newProdCategory.isBlank() || priceParsed == null || stockParsed == null) {
                                addProductError = "Semua kolom wajib diisi dengan benar!"
                                return@Button
                            }

                            coroutineScope.launch {
                                try {
                                    val response = ApiClient.getService().addProduct(
                                        ProductAddRequest(
                                            db_name = tenant.dbName,
                                            name = newProdName,
                                            sku = newProdSku,
                                            category = newProdCategory,
                                            price = priceParsed,
                                            stock = stockParsed,
                                            min_stock_alert = 5
                                        )
                                    )
                                    if (response.isSuccessful && response.body()?.get("success") == true) {
                                        showAddProductModal = false
                                        newProdName = ""
                                        newProdSku = ""
                                        newProdCategory = ""
                                        newProdPrice = ""
                                        newProdStock = ""
                                        refreshData()
                                    } else {
                                        addProductError = "Gagal menyimpan produk ke database VPS."
                                    }
                                } catch (e: Exception) {
                                    addProductError = "Koneksi Error: ${e.localizedMessage}"
                                }
                            }
"""

    lines[start_idx:end_idx] = [new_add_product_block]

with open('app/src/main/java/com/example/ui/screens/DashboardScreen.kt', 'w') as f:
    f.write(''.join(lines))
print('Successfully modified DashboardScreen!')
