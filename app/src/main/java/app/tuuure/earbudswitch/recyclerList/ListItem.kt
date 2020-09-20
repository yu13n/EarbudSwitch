package app.tuuure.earbudswitch.recyclerList


data class ListItem(
    val name: String,
    val address: String,
    var isChecked: Boolean = false,
    var server: String = ""
)