package tororo1066.dieifontheblock

import net.kyori.adventure.text.Component
import net.kyori.adventure.title.Title
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import tororo1066.dieifontheblock.DieIfOnTheBlock.Companion.sendPrefixMsg
import tororo1066.tororopluginapi.SStr
import tororo1066.tororopluginapi.annotation.SCommandV2Body
import tororo1066.tororopluginapi.sCommand.v2.SCommandV2
import java.time.Duration

class Command: SCommandV2("dot") {

    init {
        root.setPermission("dot.op")
    }

//    @SCommandBody
//    val block = command {
//        literal("block") {
//            literal("add") {
//                literal("battle") {
//                    setFunctionExecutor { sender, _, _ ->
//                        val p = sender as? Player ?: return@setFunctionExecutor
//                        val item = p.inventory.itemInMainHand
//                        if (item.type.isAir) {
//                            p.sendPrefixMsg(SStr("&cアイテムを持ってください"))
//                            return@setFunctionExecutor
//                        }
//                        BattleConfig.addBlock(item.type)
//                        p.sendPrefixMsg(SStr("&a追加しました"))
//                    }
//                }
//            }
//        }
//    }

    @SCommandV2Body
    val start = command {
        literal("start") {
            literal("battle") {
                setFunctionExecutor { sender, _, _ ->
                    val p = sender as? Player ?: return@setFunctionExecutor
                    DieIfOnTheBlock.gameTask = BattleGameTask(p.location)
                    DieIfOnTheBlock.gameTask?.start()
                }
            }
        }
    }

    @SCommandV2Body
    val stop = command {
        literal("stop") {
            literal("battle") {
                setFunctionExecutor { sender, _, _ ->
                    DieIfOnTheBlock.gameTask?.interrupt()
                    DieIfOnTheBlock.gameTask = null
                }
            }
        }
    }

    @SCommandV2Body
    val reload = command {
        literal("reload") {
            setFunctionExecutor { sender, _, _ ->
                sender.sendPrefixMsg(SStr("&aリロードしました"))
                BattleConfig.reload()
//                TeamConfig.reload()
            }
        }
    }
}