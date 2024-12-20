package tororo1066.dieifontheblock.data

import org.bukkit.block.Biome

class TeamBlockElement(val name: String, val y: IntRange, val biome: Biome? = null,
                       val allowContains: Boolean = false, val displayName: String? = null)