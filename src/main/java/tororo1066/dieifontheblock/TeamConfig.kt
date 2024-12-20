package tororo1066.dieifontheblock

import org.bukkit.configuration.file.YamlConfiguration
import tororo1066.dieifontheblock.data.BlockElement
import tororo1066.tororopluginapi.SJavaPlugin

class TeamConfig {

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
            config = SJavaPlugin.sConfig.getConfig("team_config").let {
                if (it == null) {
                    SJavaPlugin.plugin.saveResource("team_config.yml", false)
                    SJavaPlugin.sConfig.getConfig("team_config")!!
                } else it
            }
            BLOCK_LIST = ArrayList(
                config.getStringList("block.list")
                    .map {
                        val split = it.split(",")
                        BlockElement(split[1], split[0].toBoolean(), split.getOrNull(2))
                    }
            )
            BLOCK_CHANGE_TIME = config.getInt("block.change_time")
            HEAL_SPEED = config.getDouble("heal.speed")
        }
    }
}