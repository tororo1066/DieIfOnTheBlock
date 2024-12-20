package tororo1066.dieifontheblock

import org.bukkit.configuration.file.YamlConfiguration
import tororo1066.dieifontheblock.data.BlockElement
import tororo1066.tororopluginapi.SJavaPlugin

class BattleConfig {

    companion object {
        private lateinit var config: YamlConfiguration

        var BLOCK_LIST = ArrayList<BlockElement>()
        var BLOCK_FIRST: BlockElement? = null
        var BLOCK_SHARE = false
        var BLOCK_CHANGE_TIME = 0
        var BLOCK_CHANGE_TIME_MULTIPLIER = 0.0

        var BORDER_SIZE = 0.0
        var BORDER_SPEED = 0.0
        var BORDER_SPEED_MULTIPLIER = 0.0
        var BORDER_DAMAGE = 0.0

        var HEAL_SPEED = 0.0

        fun reload() {
            config = SJavaPlugin.sConfig.getConfig("battle_config").let {
                if (it == null) {
                    SJavaPlugin.plugin.saveResource("battle_config.yml", false)
                    SJavaPlugin.sConfig.getConfig("battle_config")!!
                } else it
            }
            SJavaPlugin.plugin.reloadConfig()
            BLOCK_LIST = ArrayList(
                config.getStringList("block.list")
                    .map {
                        val split = it.split(",")
                        BlockElement(split[1], split[0].toBoolean(), split.getOrNull(2))
                    }
            )
            BLOCK_FIRST = BLOCK_LIST.firstOrNull()
            BLOCK_SHARE = config.getBoolean("block.share")
            BLOCK_CHANGE_TIME = config.getInt("block.change_time")
            BLOCK_CHANGE_TIME_MULTIPLIER = config.getDouble("block.change_time_multiplier")
            BORDER_SIZE = config.getDouble("border.size")
            BORDER_SPEED = config.getDouble("border.speed")
            BORDER_SPEED_MULTIPLIER = config.getDouble("border.speed_multiplier")
            BORDER_DAMAGE = config.getDouble("border.damage")

            HEAL_SPEED = config.getDouble("heal.speed")
        }

//        fun addBlock(block: Material) {
//            val list = config.getStringList("block.list")
//            list.add(block.name)
//            config.set("block.list", list)
//            config.save(File(SJavaPlugin.plugin.dataFolder, "battle_config.yml"))
//            BLOCK_LIST.add(block.name)
//        }
    }
}