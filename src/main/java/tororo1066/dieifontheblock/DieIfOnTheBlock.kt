package tororo1066.dieifontheblock

import org.bukkit.command.CommandSender
import tororo1066.tororopluginapi.SJavaPlugin
import tororo1066.tororopluginapi.SStr
import tororo1066.tororopluginapi.utils.sendMessage

class DieIfOnTheBlock: SJavaPlugin(UseOption.SConfig) {

    companion object {
        val prefix = SStr("&6[&cDieIfOnTheBlock&6]&r")

        var gameTask: Thread? = null

        fun CommandSender.sendPrefixMsg(msg: SStr) {
            sendMessage(prefix + msg)
        }
    }

    override fun onStart() {
        BattleConfig.reload()
        Command()
    }
}