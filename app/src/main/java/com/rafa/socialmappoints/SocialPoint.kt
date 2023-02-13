package com.rafa.socialmappoints

data class SocialPoint(
    var userID: String,
    var title: String,
    var description: String,
    var latitude: Double,
    var longitude: Double
) {
    constructor() : this("", "", "", 0.0, 0.0)
}



