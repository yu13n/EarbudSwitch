package app.tuuure.earbudswitch.recyclerList


data class ListItem(
    val name: String,
    val address: String,
    var isChecked: Boolean = false,
    var isA2dpConnected: Boolean = false,
    var isHeadsetConnected: Boolean = false,
    var server: String = ""
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ListItem

        return address == other.address
    }

    override fun hashCode(): Int = address.hashCode()

}