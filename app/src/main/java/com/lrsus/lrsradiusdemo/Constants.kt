package com.lrsus.lrsradiusdemo

/**
 * Created by fali on 1/11/18.
 */

class Constants {

    interface ACTION {
        companion object {
            val MAIN_ACTION = "com.lrsus.lrsradiusdemo.action.main"
            val INIT_ACTION = "com.lrsus.lrsradiusdemo.action.init"
            val BROADCAST_ACTION = "com.lrsus.lrsradiusdemo.action.broadcast"
            val STARTFOREGROUND_ACTION = "com.lrsus.lrsradiusdemo.action.startforeground"
            val STOPFOREGROUND_ACTION = "com.lrsus.lrsradiusdemo.action.stopforeground"
        }
    }

    interface NOTIFICATION_ID {
        companion object {
            val FOREGROUND_SERVICE = 101
        }
    }
}