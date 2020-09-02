package app.tuuure.earbudswitch

data class ParamDevices(val devices: Collection<String>) {
    override fun toString(): String {
        return "devices= $devices"
    }
}

data class ParamTarget(val target: String) {
    override fun toString(): String {
        return "target= $target"
    }
}

data class ParamString(val content: String) {
    override fun toString(): String {
        return "content= $content"
    }
}

data class ParamServer(val server: String, val devices: Collection<String>) {
    override fun toString(): String {
        return "server= $server --devices= $devices"
    }
}

data class ParamUnit(val message: String = "") {
    override fun toString(): String {
        return "message= $message"
    }
}

class EventTag {
    companion object {
        const val SCAN = "scan_tag"
        const val CONNECT_GATT = "connect_gatt_tag"
        const val NOTIFY = "notify_tag"
        const val WRITE = "write_tag"
        const val CONNECT = "connect_tag"
        const val ADVERTISE = "advertise_tag"
        const val NOTIFIED = "notified_tag"
        const val WRITTEN = "written_tag"
        const val DISCONNECT = "disconnect_tag"
        const val STOP_SERVICE = "stop_self_tag"
        const val TYPE_RESULT = "result_"
        const val TYPE_RECEIVER = "receiver_"
    }
}