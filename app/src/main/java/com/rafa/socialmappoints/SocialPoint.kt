package com.rafa.socialmappoints

data class SocialPoint(
    var userID: String,
    var title: String,
    var description: String,
    var latitude: Double,
    var longitude: Double,
    var comments: MutableList<Comment>
) {
    constructor() : this("", "", "", 0.0, 0.0, mutableListOf<Comment>())
}




