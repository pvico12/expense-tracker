package com.cs446.expensetracker.mockData

data class MockDealJson(
    var mock_deals: List<Deal>,
    val current_location: String,
)

data class Deal(
    val cost: Double,
    val item: String,
    val location: String,
    val note: String = "",
    val date: String,
    val upvotes: Int,
    val downvotes: Int,
    val address: String,
)

val mockDeals = listOf(
    Deal(4.99, "grapes", "Walmart", "Off from 6.99", "2024-02-10T10:30:00.000Z", 5, 2, "70 Bridgeport Road E, Waterloo, ON N2V0A4"),
    Deal(5.00, "toothpaste", "Shoppers Drugmart", "worth it", "2024-02-09T08:45:00.000Z", 200, 5, "335 Farmers Market Rd, Waterloo, ON N2V 0A4"),
    Deal(6.00, "apples", "Shoppers Drugmart", "worth it", "2024-02-09T08:45:00.000Z", 5, 200, "1400 Ottawa St S, Kitchener, ON N2E 4E2"),
    Deal(6.00, "apples", "Shoppers Drugmart", "worth it", "2024-02-09T08:45:00.000Z", 5, 200, "1400 Ottawa St S, Kitchener, ON N2E 4E2"),
)

var mock_deal_json = MockDealJson(mock_deals = mockDeals, current_location ="Kitchener, Ontario")

