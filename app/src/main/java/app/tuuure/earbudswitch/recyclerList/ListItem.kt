package app.tuuure.earbudswitch.recyclerList

import java.util.*


data class ListItem(var devices: SortedMap<String, String>, var state: Boolean = false) {
    fun isTWS(): Boolean {
        return devices.size == 2
    }

    fun getName(): String? {
        return devices.firstKey()
    }

    fun getAddress(): Collection<String> {
        return devices.values
    }
}