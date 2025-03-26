package com.cs446.expensetracker.ui.deals

import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.PlaceLikelihood
import com.google.android.libraries.places.api.net.FetchPlaceResponse
import com.google.android.libraries.places.api.net.FindCurrentPlaceResponse

internal const val FIELD_SEPARATOR = "\n\t"
internal const val RESULT_SEPARATOR = "\n---\n\t"

fun FetchPlaceResponse.prettyPrint(): String {
    val response = this

    return buildString {
        append("Fetch Place Result:")
        append(RESULT_SEPARATOR)
        append(response.place.prettyPrint())
    }
}

fun FindCurrentPlaceResponse.prettyPrint(): String {
    val response = this

    return buildString {
        append(response.placeLikelihoods.size)
        append(" Current Place Results:")
        if (response.placeLikelihoods.isNotEmpty()) {
            append(RESULT_SEPARATOR)
        }
        append(
            response.placeLikelihoods.joinToString(RESULT_SEPARATOR) { placeLikelihood ->
                placeLikelihood.prettyPrint()
            }
        )
    }
}

internal fun prettyPrintAutocompleteWidget(place: Place, raw: Boolean): String {
    return buildString {
        append("Autocomplete Widget Result:")
        append(RESULT_SEPARATOR)
        if (raw) {
            append(place)
        } else {
            append(place.prettyPrint())
        }
    }
}

private fun PlaceLikelihood.prettyPrint() =
    "Likelihood: $likelihood${FIELD_SEPARATOR}Place: ${place.prettyPrint()}"

private fun LatLng.prettyPrint() = "$latitude, $longitude"

private fun Place.prettyPrint() = "$name ($id) is located at ${latLng?.prettyPrint()} ($address)"